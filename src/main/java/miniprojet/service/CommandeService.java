package miniprojet.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import miniprojet.dao.CommandeRepository;
import miniprojet.dao.DispensaireRepository;
import miniprojet.dao.LigneRepository;
import miniprojet.dao.MedicamentRepository;
import miniprojet.entity.Commande;
import miniprojet.entity.Ligne;

@Slf4j
@Service
@Validated
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final DispensaireRepository dispensaireRepository;
    private final LigneRepository ligneRepository;
    private final MedicamentRepository medicamentRepository;

    public CommandeService(CommandeRepository commandeRepository,
                           DispensaireRepository dispensaireRepository,
                           LigneRepository ligneRepository,
                           MedicamentRepository medicamentRepository) {
        this.commandeRepository = commandeRepository;
        this.dispensaireRepository = dispensaireRepository;
        this.ligneRepository = ligneRepository;
        this.medicamentRepository = medicamentRepository;
    }

    /**
     * Ajoute un ligne à une commande. 
     * Si le médicament est déjà dans la commande, la quantité est augmentée.
     */
    @Transactional
    public Ligne ajouterLigne(int commandeId, int medicamentRef, @Positive int quantite) {
        log.info("Traitement ligne commande : cmd={}, ref={}, qte={}", commandeId, medicamentRef, quantite);

        // 1. Vérifications préalables
        // On récupère le médicament et on vérifie sa disponibilité
        var medicament = medicamentRepository.findById(medicamentRef)
            .orElseThrow(() -> new NoSuchElementException("Médicament introuvable : " + medicamentRef));
        
        if (medicament.isIndisponible()) {
            log.warn("Tentative de commande d'un médicament indisponible: {}", medicamentRef);
            throw new IllegalStateException("Ce médicament est actuellement indisponible.");
        }

        // On vérifie le stock (Stock actuel < Quantité demandée + Quantités déjà réservées)
        if (medicament.getUnitesEnStock() < quantite + medicament.getUnitesCommandees()) {
            throw new IllegalStateException("Stock insuffisant pour satisfaire la demande.");
        }

        // On vérifie l'état de la commande
        var commande = commandeRepository.findById(commandeId)
            .orElseThrow(() -> new NoSuchElementException("Commande introuvable : " + commandeId));
            
        if (commande.getEnvoyeele() != null) {
            throw new IllegalStateException("Impossible de modifier une commande déjà expédiée.");
        }

        // 2. Mise à jour ou création de la ligne
        var ligne = ligneRepository.findByCommandeAndMedicament(commande, medicament)
            .orElseGet(() -> new Ligne(commande, medicament, 0));

        // Mise à jour des compteurs
        ligne.setQuantite(ligne.getQuantite() + quantite);
        medicament.setUnitesCommandees(medicament.getUnitesCommandees() + quantite);

        // Pas besoin de save(medicament) car transactionnel
        return ligneRepository.save(ligne);
    }

    /**
     * Initialise une nouvelle commande pour un dispensaire donné.
     * Applique automatiquement la remise fidélité si applicable.
     */
    @Transactional
    public Commande creerCommande(@NonNull String codeDispensaire) {
        log.info("Initialisation nouvelle commande pour le dispensaire {}", codeDispensaire);
        
        var dispensaire = dispensaireRepository.findById(codeDispensaire)
            .orElseThrow(() -> new NoSuchElementException("Dispensaire inconnu : " + codeDispensaire));

        var nouvelleCommande = new Commande(dispensaire);
        nouvelleCommande.setAdresseLivraison(dispensaire.getAdresse());

        // Calcul de la remise automatique (>100 articles commandés)
        int articlesCommandes = dispensaireRepository.nombreArticlesCommandesPar(codeDispensaire);
        if (articlesCommandes > 100) {
            log.info("Application remise fidélité pour {}", codeDispensaire);
            nouvelleCommande.setRemise(new BigDecimal("0.15"));
        }

        return commandeRepository.save(nouvelleCommande);
    }

    /**
     * Retourne une commande par son identifiant
     * @param commandeNum l'identifiant de la commande
     * @return la commande
     * @throws NoSuchElementException si la commande n'existe pas
     */
    @Transactional(readOnly = true)
    public Commande getCommande(Integer commandeNum) {
        return commandeRepository.findById(commandeNum)
            .orElseThrow(() -> new NoSuchElementException("Commande " + commandeNum + " introuvable"));
    }

    /**
     * Enregistre l'expédition d'une commande.
     * La commande passe à l'état "envoyée".
     * @param commandeNum l'identifiant de la commande
     * @return la commande mise à jour
     * @throws IllegalStateException si la commande est déjà expédiée
     */
    @Transactional
    public Commande enregistreExpedition(Integer commandeNum) {
        log.info("Expédition de la commande {}", commandeNum);
        // On réutilise la méthode getCommande pour ne pas dupliquer le code
        var commande = getCommande(commandeNum);
        if (commande.getEnvoyeele() != null) {
            throw new IllegalStateException("La commande est déjà expédiée.");
        }
        commande.setEnvoyeele(java.time.LocalDate.now());
        return commandeRepository.save(commande);
    }

    /**
     * Supprime une ligne de commande.
     * Le stock est rétabli.
     * @param ligneId l'identifiant de la ligne
     * @throws IllegalStateException si la commande est déjà expédiée
     */
    @Transactional
    public void supprimerLigne(Integer ligneId) {
        log.info("Suppression de la ligne {}", ligneId);
        var ligne = ligneRepository.findById(ligneId)
            .orElseThrow(() -> new NoSuchElementException("Ligne " + ligneId + " introuvable"));
        
        var commande = ligne.getCommande();
        if (commande.getEnvoyeele() != null) {
            throw new IllegalStateException("Impossible de modifier une commande expédiée.");
        }
        
        var medicament = ligne.getMedicament();
        // On rétablit les "unités commandées" (réservées)
        medicament.setUnitesCommandees(medicament.getUnitesCommandees() - ligne.getQuantite());
        
        ligneRepository.delete(ligne);
    }

    /**
     * Retourne la liste des commandes en cours pour un dispensaire.
     * @param codeDispensaire le code du dispensaire
     * @return la liste des commandes
     */
    @Transactional(readOnly = true)
    public List<Commande> getCommandeEnCoursPour(String codeDispensaire) {
        return commandeRepository.commandesEnCoursPour(codeDispensaire);
    }
}
