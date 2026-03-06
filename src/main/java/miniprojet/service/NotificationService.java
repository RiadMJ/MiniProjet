package miniprojet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import miniprojet.dao.FournisseurRepository;
import miniprojet.dao.MedicamentRepository;
import miniprojet.entity.Categorie;
import miniprojet.entity.Fournisseur;
import miniprojet.entity.Medicament;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de notification pour le réapprovisionnement des médicaments.
 * Détermine les médicaments dont unitesEnStock < niveauDeReappro et envoie
 * UN SEUL email récapitulatif par fournisseur, groupé par catégorie, via SendGrid.
 */
@Slf4j
@Service
public class NotificationService {

    private final MedicamentRepository medicamentDao;
    private final FournisseurRepository fournisseurDao;
    private final EmailService emailService;

    public NotificationService(MedicamentRepository medicamentDao,
                               FournisseurRepository fournisseurDao,
                               EmailService emailService) {
        this.medicamentDao = medicamentDao;
        this.fournisseurDao = fournisseurDao;
        this.emailService = emailService;
    }

    /**
     * Vérifie si un médicament a atteint son niveau de réapprovisionnement (strictement inférieur).
     * Utilisé après chaque vente/commande pour déclencher une notification ciblée.
     *
     * @param medicament le médicament à vérifier
     */
    public void verifierEtNotifierReappro(Medicament medicament) {
        if (medicament.isIndisponible()) {
            return;
        }
        if (medicament.getUnitesEnStock() < medicament.getNiveauDeReappro()) {
            log.warn("Médicament '{}' sous le niveau de réapprovisionnement. Stock: {}, Niveau: {}",
                    medicament.getNom(), medicament.getUnitesEnStock(), medicament.getNiveauDeReappro());
            // Déclenche la vérification complète pour envoyer des emails consolidés
            verifierTousLesMedicaments();
        }
    }

    /**
     * Vérifie tous les médicaments, puis envoie UN SEUL email consolidé à chaque fournisseur
     * récapitulant, catégorie par catégorie, tous les médicaments à réapprovisionner
     * qu'il est susceptible de fournir.
     *
     * @return le nombre de médicaments nécessitant un réapprovisionnement
     */
    @Transactional(readOnly = true)
    public int verifierTousLesMedicaments() {
        log.info("Vérification du stock de tous les médicaments...");

        // 1. Trouver tous les médicaments dont unitesEnStock < niveauDeReappro (strictement inférieur)
        List<Medicament> aReapprovisionner = medicamentDao.findAll().stream()
                .filter(m -> !m.isIndisponible() && m.getUnitesEnStock() < m.getNiveauDeReappro())
                .collect(Collectors.toList());

        if (aReapprovisionner.isEmpty()) {
            log.info("Aucun médicament ne nécessite de réapprovisionnement");
            return 0;
        }
        log.info("{} médicament(s) nécessite(nt) un réapprovisionnement", aReapprovisionner.size());

        // 2. Grouper les médicaments à réapprovisionner par catégorie
        Map<Categorie, List<Medicament>> medParCategorie = aReapprovisionner.stream()
                .collect(Collectors.groupingBy(Medicament::getCategorie));

        // 3. Pour chaque fournisseur, construire UN email récapitulatif groupé par catégorie
        List<Fournisseur> tousLesFournisseurs = fournisseurDao.findAll();

        for (Fournisseur fournisseur : tousLesFournisseurs) {
            // Médicaments que CE fournisseur peut fournir, groupés par catégorie
            Map<Categorie, List<Medicament>> medicamentsDuFournisseur = new LinkedHashMap<>();

            for (Categorie categorie : fournisseur.getCategories()) {
                List<Medicament> meds = medParCategorie.get(categorie);
                if (meds != null && !meds.isEmpty()) {
                    medicamentsDuFournisseur.put(categorie, meds);
                }
            }

            if (!medicamentsDuFournisseur.isEmpty()) {
                int totalMeds = medicamentsDuFournisseur.values().stream().mapToInt(List::size).sum();
                log.info("Envoi d'un email récapitulatif à {} ({}) : {} médicament(s) dans {} catégorie(s)",
                        fournisseur.getNom(), fournisseur.getEmail(),
                        totalMeds, medicamentsDuFournisseur.size());

                String subject = "Demande de devis de réapprovisionnement - Pharmacie Centrale";
                String body = buildConsolidatedEmailBody(fournisseur, medicamentsDuFournisseur);
                emailService.sendEmail(fournisseur.getEmail(), subject, body);
            }
        }

        return aReapprovisionner.size();
    }

    /**
     * Construit le corps HTML d'un email récapitulatif destiné à UN fournisseur,
     * listant tous les médicaments à réapprovisionner groupés par catégorie.
     *
     * @param fournisseur            le fournisseur destinataire
     * @param medicamentsParCategorie les médicaments regroupés par catégorie
     * @return le contenu HTML de l'email
     */
    private String buildConsolidatedEmailBody(Fournisseur fournisseur,
                                               Map<Categorie, List<Medicament>> medicamentsParCategorie) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <html>
            <body style="font-family: Arial, sans-serif; padding: 20px; color: #333;">
                <h2 style="color: #2c3e50;">Demande de devis de réapprovisionnement</h2>
                <p>Bonjour <strong>%s</strong>,</p>
                <p>Nous vous contactons afin de solliciter un devis de réapprovisionnement
                   pour les médicaments suivants, dont le stock est actuellement insuffisant.
                   Merci de nous transmettre vos disponibilités et tarifs.</p>
            """.formatted(fournisseur.getNom()));

        for (Map.Entry<Categorie, List<Medicament>> entry : medicamentsParCategorie.entrySet()) {
            Categorie categorie = entry.getKey();
            List<Medicament> meds = entry.getValue();

            sb.append("<h3 style=\"color:#2980b9; border-bottom:1px solid #ccc; padding-bottom:4px;\">")
              .append(categorie.getLibelle())
              .append("</h3>");

            sb.append("""
                <table style="border-collapse: collapse; width: 100%; margin-bottom: 16px;">
                  <thead>
                    <tr style="background-color:#2980b9; color:#fff;">
                      <th style="padding:8px; text-align:left;">Médicament</th>
                      <th style="padding:8px; text-align:right;">Stock actuel</th>
                      <th style="padding:8px; text-align:right;">Niveau réappro</th>
                      <th style="padding:8px; text-align:right;">Prix unitaire</th>
                    </tr>
                  </thead>
                  <tbody>
                """);

            boolean alt = false;
            for (Medicament m : meds) {
                String bg = alt ? "background-color:#f2f2f2;" : "";
                sb.append("<tr style=\"%s\">".formatted(bg))
                  .append("<td style=\"padding:7px; border:1px solid #ddd;\">").append(m.getNom()).append("</td>")
                  .append("<td style=\"padding:7px; border:1px solid #ddd; text-align:right; color:#c0392b;\">")
                  .append(m.getUnitesEnStock()).append("</td>")
                  .append("<td style=\"padding:7px; border:1px solid #ddd; text-align:right;\">")
                  .append(m.getNiveauDeReappro()).append("</td>")
                  .append("<td style=\"padding:7px; border:1px solid #ddd; text-align:right;\">")
                  .append(m.getPrixUnitaire()).append(" €</td>")
                  .append("</tr>");
                alt = !alt;
            }
            sb.append("</tbody></table>");
        }

        sb.append("""
                <p>Nous vous remercions de bien vouloir nous adresser votre devis dans les meilleurs délais.</p>
                <p>Cordialement,<br><strong>Pharmacie Centrale</strong></p>
            </body>
            </html>
            """);

        return sb.toString();
    }
}
