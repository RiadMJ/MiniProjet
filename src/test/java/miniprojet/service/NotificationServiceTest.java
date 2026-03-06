package miniprojet.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import miniprojet.dao.CategorieRepository;
import miniprojet.dao.FournisseurRepository;
import miniprojet.dao.MedicamentRepository;
import miniprojet.entity.Categorie;
import miniprojet.entity.Fournisseur;
import miniprojet.entity.Medicament;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MedicamentRepository medicamentRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Test
    void testVerifierEtNotifierReappro_stockBas() {
        // Créer une catégorie avec un fournisseur
        Categorie cat = new Categorie();
        cat.setLibelle("TestNotif");
        cat = categorieRepository.saveAndFlush(cat);

        Fournisseur f = new Fournisseur("NotifFournisseur", "notif@test.com");
        f.getCategories().add(cat);
        cat.getFournisseurs().add(f);
        fournisseurRepository.saveAndFlush(f);
        categorieRepository.saveAndFlush(cat);

        // Créer un médicament avec stock bas
        Medicament med = new Medicament();
        med.setNom("MedNotif");
        med.setCategorie(cat);
        med.setUnitesEnStock(3);
        med.setNiveauDeReappro(10);
        med.setIndisponible(false);
        final Medicament savedMed1 = medicamentRepository.saveAndFlush(med);

        // La méthode doit s'exécuter sans erreur
        // (email ne sera pas envoyé car clé API non configurée en test)
        assertDoesNotThrow(() -> notificationService.verifierEtNotifierReappro(savedMed1));
    }

    @Test
    void testVerifierEtNotifierReappro_stockOk() {
        Categorie cat = new Categorie();
        cat.setLibelle("TestNotifOk");
        cat = categorieRepository.saveAndFlush(cat);

        Medicament med = new Medicament();
        med.setNom("MedNotifOk");
        med.setCategorie(cat);
        med.setUnitesEnStock(100);
        med.setNiveauDeReappro(10); // stock (100) > niveau réappro (10)
        med.setIndisponible(false);
        final Medicament savedMed2 = medicamentRepository.saveAndFlush(med);

        // Ne doit pas provoquer d'erreur (pas de notification car stock OK)
        assertDoesNotThrow(() -> notificationService.verifierEtNotifierReappro(savedMed2));
    }

    @Test
    void testVerifierEtNotifierReappro_indisponible() {
        Categorie cat = new Categorie();
        cat.setLibelle("TestNotifIndispo");
        cat = categorieRepository.saveAndFlush(cat);

        Medicament med = new Medicament();
        med.setNom("MedNotifIndispo");
        med.setCategorie(cat);
        med.setUnitesEnStock(2);
        med.setNiveauDeReappro(10);
        med.setIndisponible(true); // indisponible = pas de notif
        final Medicament savedMed3 = medicamentRepository.saveAndFlush(med);

        assertDoesNotThrow(() -> notificationService.verifierEtNotifierReappro(savedMed3));
    }

    @Test
    void testVerifierTousLesMedicaments() {
        // Appeler la vérification globale, ne doit pas provoquer d'erreur
        int count = notificationService.verifierTousLesMedicaments();
        assertTrue(count >= 0);
    }
}
