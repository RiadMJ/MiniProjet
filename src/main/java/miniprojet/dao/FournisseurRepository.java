package miniprojet.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import miniprojet.entity.Fournisseur;

/**
 * Repository pour l'entité Fournisseur.
 * Auto-implémenté par Spring Data JPA.
 */
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    /**
     * Recherche un fournisseur par son nom (exact)
     * @param nom le nom recherché
     * @return le fournisseur correspondant (optionnel)
     */
    Optional<Fournisseur> findByNom(String nom);

    /**
     * Recherche les fournisseurs dont le nom contient une sous-chaîne
     * @param substring la sous-chaîne à rechercher
     * @return la liste des fournisseurs correspondants
     */
    List<Fournisseur> findByNomContainingIgnoreCase(String substring);

    /**
     * Recherche un fournisseur par son email
     * @param email l'email recherché
     * @return le fournisseur correspondant (optionnel)
     */
    Optional<Fournisseur> findByEmail(String email);

    /**
     * Recherche les fournisseurs qui fournissent une catégorie donnée
     * @param categorieCode le code de la catégorie
     * @return la liste des fournisseurs pour cette catégorie
     */
    @Query("SELECT f FROM Fournisseur f JOIN f.categories c WHERE c.code = :categorieCode")
    List<Fournisseur> findByCategorieCode(Integer categorieCode);

    /**
     * Recherche les fournisseurs qui fournissent des médicaments nécessitant un réapprovisionnement
     * (stock <= niveau de réappro) pour une catégorie donnée
     * @param categorieCode le code de la catégorie
     * @return la liste des fournisseurs concernés
     */
    @Query("""
        SELECT DISTINCT f FROM Fournisseur f
        JOIN f.categories c
        JOIN c.medicaments m
        WHERE c.code = :categorieCode
        AND m.unitesEnStock <= m.niveauDeReappro
        AND m.indisponible = false
    """)
    List<Fournisseur> findFournisseursForReappro(Integer categorieCode);
}
