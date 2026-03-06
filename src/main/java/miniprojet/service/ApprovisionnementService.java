package miniprojet.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import miniprojet.dao.MedicamentRepository;
import miniprojet.entity.Categorie;
import miniprojet.entity.Fournisseur;
import miniprojet.entity.Medicament;

@Slf4j
@Service
public class ApprovisionnementService {

    private final MedicamentRepository medicamentRepository;
    private final EmailService emailService;

    public ApprovisionnementService(MedicamentRepository medicamentRepository, EmailService emailService) {
        this.medicamentRepository = medicamentRepository;
        this.emailService = emailService;
    }

    /**
     * Identifie les médicaments à réapprovisionner et envoie des emails aux
     * fournisseurs concernés. Un médicament est à réapprovisionner si son stock
     * est inférieur à son niveau de réapprovisionnement. Les emails sont
     * groupés par catégorie.
     */
    @Transactional
    public void lancerApprovisionnement() {
        log.info("Lancement du processus d'approvisionnement...");

        // 1. Identifier les médicaments nécessitant un réapprovisionnement
        List<Medicament> medicamentsACommander = medicamentRepository.findAll().stream()
                .filter(m -> m.getUnitesEnStock() < m.getNiveauDeReappro())
                .collect(Collectors.toList());

        if (medicamentsACommander.isEmpty()) {
            log.info("Aucun médicament ne nécessite un réapprovisionnement.");
            return;
        }

        log.info("{} médicaments identifiés pour réapprovisionnement.", medicamentsACommander.size());

        // 2. Regrouper par fournisseur et par catégorie
        // Structure : Fournisseur -> (Categorie -> Liste de Médicaments)
        Map<Fournisseur, Map<Categorie, List<Medicament>>> mapFournisseur = new HashMap<>();

        for (Medicament medicament : medicamentsACommander) {
            Categorie categorie = medicament.getCategorie();
            List<Fournisseur> fournisseurs = categorie.getFournisseurs();

            if (fournisseurs == null || fournisseurs.isEmpty()) {
                log.warn("Aucun fournisseur trouvé pour la catégorie : {}", categorie.getLibelle());
                continue;
            }

            for (Fournisseur fournisseur : fournisseurs) {
                mapFournisseur
                        .computeIfAbsent(fournisseur, k -> new HashMap<>())
                        .computeIfAbsent(categorie, k -> new ArrayList<>())
                        .add(medicament);
            }
        }

        // 3. Envoyer les emails aux fournisseurs
        for (Map.Entry<Fournisseur, Map<Categorie, List<Medicament>>> entry : mapFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();
            Map<Categorie, List<Medicament>> categoriesMap = entry.getValue();

            envoyerEmailFournisseur(fournisseur, categoriesMap);
        }

        log.info("Processus d'approvisionnement terminé.");
    }

    private void envoyerEmailFournisseur(Fournisseur fournisseur, Map<Categorie, List<Medicament>> categoriesMap) {
        String sujet = "Demande de devis - Réapprovisionnement Pharmacie";
        StringBuilder corps = new StringBuilder();

        corps.append("<html><body>");
        corps.append("<h2>Bonjour ").append(fournisseur.getNom()).append(",</h2>");
        corps.append("<p>Veuillez nous transmettre un devis pour le réapprovisionnement des médicaments suivants, classés par catégorie :</p>");

        for (Map.Entry<Categorie, List<Medicament>> catEntry : categoriesMap.entrySet()) {
            Categorie categorie = catEntry.getKey();
            List<Medicament> medicaments = catEntry.getValue();

            corps.append("<h3>Catégorie : ").append(categorie.getLibelle()).append("</h3>");
            corps.append("<ul>");
            for (Medicament m : medicaments) {
                corps.append("<li>")
                        .append("<strong>").append(m.getNom()).append("</strong>")
                        .append(" (Stock actuel : ").append(m.getUnitesEnStock())
                        .append(", Niveau de réappro : ").append(m.getNiveauDeReappro()).append(")")
                        .append("</li>");
            }
            corps.append("</ul>");
        }

        corps.append("<p>Cordialement,<br/>La Pharmacie Centrale</p>");
        corps.append("</body></html>");

        boolean sent = emailService.sendEmail(fournisseur.getEmail(), sujet, corps.toString());
        if (sent) {
            log.info("Email envoyé avec succès à {}", fournisseur.getEmail());
        } else {
            log.error("Échec de l'envoi de l'email à {}", fournisseur.getEmail());
        }
    }
}
