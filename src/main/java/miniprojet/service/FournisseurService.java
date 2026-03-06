package miniprojet.service;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import miniprojet.dao.CategorieRepository;
import miniprojet.dao.FournisseurRepository;
import miniprojet.entity.Fournisseur;

@Slf4j
@Service
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;
    private final CategorieRepository categorieRepository;

    public FournisseurService(FournisseurRepository fournisseurRepository, CategorieRepository categorieRepository) {
        this.fournisseurRepository = fournisseurRepository;
        this.categorieRepository = categorieRepository;
    }

    @Transactional
    public Fournisseur creerFournisseur(String nom, String email) {
        log.info("Création fournisseur: {} ({})", nom, email);
        return fournisseurRepository.save(new Fournisseur(nom, email));
    }

    @Transactional
    public Fournisseur updateFournisseur(Long id, String nom, String email) {
        log.info("Modification fournisseur {}", id);
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fournisseur " + id + " introuvable"));

        if (StringUtils.hasText(nom)) {
            fournisseur.setNom(nom);
        }
        if (StringUtils.hasText(email)) {
            fournisseur.setEmail(email);
        }
        // Le flush est automatique fin de transaction
        return fournisseur;
    }

    @Transactional(readOnly = true)
    public List<Fournisseur> findAll() {
        return fournisseurRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Fournisseur findById(Long id) {
        return fournisseurRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Fournisseur " + id + " introuvable"));
    }

    @Transactional(readOnly = true)
    public List<Fournisseur> findByNomContaining(String partieNom) {
        return fournisseurRepository.findByNomContainingIgnoreCase(partieNom);
    }

    @Transactional(readOnly = true)
    public List<Fournisseur> findByCategorieCode(Integer codeCategorie) {
        return fournisseurRepository.findByCategorieCode(codeCategorie);
    }

    /**
     * Supprime un fournisseur par son identifiant. Gère la relation ManyToMany
     * avec Categorie pour éviter les effets de bord indésirables.
     */
    @Transactional
    public void deleteFournisseur(Long id) {
        log.info("Suppression fournisseur {}", id);
        Fournisseur fournisseur = findById(id);

        // On rompt les liens avec les catégories (Categorie est le propriétaire de la relation)
        // On fait une copie de la liste pour itérer sans souci de concurrence
        List<miniprojet.entity.Categorie> categories = List.copyOf(fournisseur.getCategories());
        for (var categorie : categories) {
            categorie.getFournisseurs().remove(fournisseur);
            // On met aussi à jour le côté inverse pour la cohérence en mémoire
            fournisseur.getCategories().remove(categorie);
            categorieRepository.save(categorie);
        }

        fournisseurRepository.delete(fournisseur);
    }

    @Transactional
    public Fournisseur ajouterCategorie(Long fournisseurId, Integer categorieCode) {
        log.info("Ajout catégorie {} au fournisseur {}", categorieCode, fournisseurId);
        Fournisseur fournisseur = findById(fournisseurId);
        var categorie = categorieRepository.findById(categorieCode)
                .orElseThrow(() -> new NoSuchElementException("Catégorie " + categorieCode + " introuvable"));

        // Categorie est le owning side
        if (!categorie.getFournisseurs().contains(fournisseur)) {
            categorie.getFournisseurs().add(fournisseur);
            fournisseur.getCategories().add(categorie);
            categorieRepository.save(categorie);
        }
        return fournisseur;
    }

    @Transactional
    public Fournisseur retirerCategorie(Long fournisseurId, Integer categorieCode) {
        log.info("Retrait catégorie {} au fournisseur {}", categorieCode, fournisseurId);
        Fournisseur fournisseur = findById(fournisseurId);
        var categorie = categorieRepository.findById(categorieCode)
                .orElseThrow(() -> new NoSuchElementException("Catégorie " + categorieCode + " introuvable"));

        // Categorie est le owning side
        if (categorie.getFournisseurs().contains(fournisseur)) {
            categorie.getFournisseurs().remove(fournisseur);
            fournisseur.getCategories().remove(categorie);
            categorieRepository.save(categorie);
        }
        return fournisseur;
    }
}
