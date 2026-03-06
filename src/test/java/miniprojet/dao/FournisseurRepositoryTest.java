package miniprojet.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import miniprojet.entity.Categorie;
import miniprojet.entity.Fournisseur;
import miniprojet.entity.Medicament;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FournisseurRepositoryTest {

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Test
    void testFindByNom() {
        Fournisseur f = new Fournisseur("PharmaTest", "test@pharma.com");
        fournisseurRepository.saveAndFlush(f);

        Optional<Fournisseur> found = fournisseurRepository.findByNom("PharmaTest");
        assertTrue(found.isPresent());
        assertEquals("PharmaTest", found.get().getNom());
    }

    @Test
    void testFindByNomContainingIgnoreCase() {
        Fournisseur f1 = new Fournisseur("PharmaPlus Test", "test1@pharma.com");
        Fournisseur f2 = new Fournisseur("MedSupply Test", "test2@pharma.com");
        fournisseurRepository.saveAndFlush(f1);
        fournisseurRepository.saveAndFlush(f2);

        List<Fournisseur> result = fournisseurRepository.findByNomContainingIgnoreCase("pharma");
        // Au moins f1 + les fournisseurs de data.sql qui contiennent "pharma"
        assertTrue(result.stream().anyMatch(f -> f.getNom().equals("PharmaPlus Test")));
    }

    @Test
    void testFindByEmail() {
        Fournisseur f = new Fournisseur("EmailTest", "unique@email.com");
        fournisseurRepository.saveAndFlush(f);

        Optional<Fournisseur> found = fournisseurRepository.findByEmail("unique@email.com");
        assertTrue(found.isPresent());
        assertEquals("EmailTest", found.get().getNom());
    }

    @Test
    void testFindByCategorieCode() {
        // Créer une catégorie et un fournisseur associé
        Categorie cat = new Categorie();
        cat.setLibelle("Catégorie Test Fournisseur");
        cat = categorieRepository.saveAndFlush(cat);

        Fournisseur f = new Fournisseur("FournisseurCatTest", "cat@test.com");
        f.getCategories().add(cat);
        cat.getFournisseurs().add(f);
        fournisseurRepository.saveAndFlush(f);
        categorieRepository.saveAndFlush(cat);

        List<Fournisseur> result = fournisseurRepository.findByCategorieCode(cat.getCode());
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(fo -> fo.getNom().equals("FournisseurCatTest")));
    }

    @Test
    void testFindFournisseursForReappro() {
        // Créer une catégorie
        Categorie cat = new Categorie();
        cat.setLibelle("Catégorie Réappro Test");
        cat = categorieRepository.saveAndFlush(cat);

        // Créer un médicament avec stock <= niveau de réappro
        Medicament med = new Medicament();
        med.setNom("MedReappro");
        med.setCategorie(cat);
        med.setUnitesEnStock(5);
        med.setNiveauDeReappro(10); // stock (5) <= niveau réappro (10)
        med.setIndisponible(false);
        medicamentRepository.saveAndFlush(med);

        // Associer un fournisseur à la catégorie
        Fournisseur f = new Fournisseur("FournisseurReappro", "reappro@test.com");
        f.getCategories().add(cat);
        cat.getFournisseurs().add(f);
        fournisseurRepository.saveAndFlush(f);
        categorieRepository.saveAndFlush(cat);

        List<Fournisseur> result = fournisseurRepository.findFournisseursForReappro(cat.getCode());
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(fo -> fo.getNom().equals("FournisseurReappro")));
    }

    @Test
    void testFindFournisseursForReaapproExcludesIndisponible() {
        // Créer une catégorie
        Categorie cat = new Categorie();
        cat.setLibelle("Catégorie Indispo Test");
        cat = categorieRepository.saveAndFlush(cat);

        // Créer un médicament indisponible
        Medicament med = new Medicament();
        med.setNom("MedIndispo");
        med.setCategorie(cat);
        med.setUnitesEnStock(2);
        med.setNiveauDeReappro(10);
        med.setIndisponible(true); // indisponible
        medicamentRepository.saveAndFlush(med);

        // Associer un fournisseur
        Fournisseur f = new Fournisseur("FournisseurIndispo", "indispo@test.com");
        f.getCategories().add(cat);
        cat.getFournisseurs().add(f);
        fournisseurRepository.saveAndFlush(f);
        categorieRepository.saveAndFlush(cat);

        List<Fournisseur> result = fournisseurRepository.findFournisseursForReappro(cat.getCode());
        // Le médicament est indisponible, donc pas de fournisseur trouvé pour réappro
        assertTrue(result.isEmpty());
    }
}
