package miniprojet.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import miniprojet.entity.Fournisseur;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Chaque test est exécuté dans une transaction qui sera rollbackée
class FournisseurServiceTest {

    @Autowired
    private FournisseurService fournisseurService;

    @Test
    void testCreerFournisseur() {
        Fournisseur f = fournisseurService.creerFournisseur("NouveauFournisseur", "new@test.com");
        assertNotNull(f.getId());
        assertEquals("NouveauFournisseur", f.getNom());
        assertEquals("new@test.com", f.getEmail());
    }

    @Test
    void testFindById() {
        Fournisseur f = fournisseurService.creerFournisseur("FindTest", "find@test.com");
        Fournisseur found = fournisseurService.findById(f.getId());
        assertEquals(f.getId(), found.getId());
        assertEquals("FindTest", found.getNom());
    }

    @Test
    void testFindByIdInexistant() {
        assertThrows(NoSuchElementException.class, () -> {
            fournisseurService.findById(99999L);
        });
    }

    @Test
    void testUpdateFournisseur() {
        Fournisseur f = fournisseurService.creerFournisseur("AvantUpdate", "avant@test.com");
        Fournisseur updated = fournisseurService.updateFournisseur(f.getId(), "ApresUpdate", "apres@test.com");
        assertEquals("ApresUpdate", updated.getNom());
        assertEquals("apres@test.com", updated.getEmail());
    }

    @Test
    void testUpdateFournisseurPartiel() {
        Fournisseur f = fournisseurService.creerFournisseur("PartialUpdate", "partial@test.com");
        // Update seulement le nom
        Fournisseur updated = fournisseurService.updateFournisseur(f.getId(), "NouveauNom", null);
        assertEquals("NouveauNom", updated.getNom());
        assertEquals("partial@test.com", updated.getEmail()); // email inchangé
    }

    @Test
    void testDeleteFournisseur() {
        Fournisseur f = fournisseurService.creerFournisseur("ASupprimer", "delete@test.com");
        Long id = f.getId();
        fournisseurService.deleteFournisseur(id);
        assertThrows(NoSuchElementException.class, () -> {
            fournisseurService.findById(id);
        });
    }

    @Test
    void testDeleteFournisseurInexistant() {
        assertThrows(NoSuchElementException.class, () -> {
            fournisseurService.deleteFournisseur(99999L);
        });
    }

    @Test
    void testAjouterCategorie() {
        Fournisseur f = fournisseurService.creerFournisseur("FournCat", "cat@test.com");

        // Utiliser une catégorie existante (chargée par data.sql, code = 1)
        Fournisseur result = fournisseurService.ajouterCategorie(f.getId(), 1);

        assertFalse(result.getCategories().isEmpty());
        assertTrue(result.getCategories().stream()
                .anyMatch(c -> c.getCode().equals(1)));
    }

    @Test
    void testAjouterCategorieDejaAssociee() {
        Fournisseur f = fournisseurService.creerFournisseur("FournDoublon", "doublon@test.com");
        fournisseurService.ajouterCategorie(f.getId(), 1);

        // Tentative d'ajouter la même catégorie
        assertThrows(IllegalStateException.class, () -> {
            fournisseurService.ajouterCategorie(f.getId(), 1);
        });
    }

    @Test
    void testAjouterCategorieInexistante() {
        Fournisseur f = fournisseurService.creerFournisseur("FournCatInex", "catinex@test.com");
        assertThrows(NoSuchElementException.class, () -> {
            fournisseurService.ajouterCategorie(f.getId(), 99999);
        });
    }

    @Test
    void testRetirerCategorie() {
        Fournisseur f = fournisseurService.creerFournisseur("FournRetirer", "retirer@test.com");
        fournisseurService.ajouterCategorie(f.getId(), 1);

        Fournisseur result = fournisseurService.retirerCategorie(f.getId(), 1);
        assertTrue(result.getCategories().stream()
                .noneMatch(c -> c.getCode().equals(1)));
    }

    @Test
    void testFindByNomContaining() {
        fournisseurService.creerFournisseur("TestSearchAlpha", "alpha@test.com");
        fournisseurService.creerFournisseur("TestSearchBeta", "beta@test.com");

        List<Fournisseur> result = fournisseurService.findByNomContaining("TestSearch");
        assertTrue(result.size() >= 2);
    }

    @Test
    void testFindAll() {
        // data.sql insère déjà 5 fournisseurs
        List<Fournisseur> all = fournisseurService.findAll();
        assertTrue(all.size() >= 5);
    }
}
