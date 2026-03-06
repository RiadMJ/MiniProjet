package miniprojet.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import miniprojet.entity.Commande;
import miniprojet.entity.Ligne;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CommandeServiceTest {

    @Autowired
    private CommandeService commandeService;

    @Test
    void testCreerCommande() {
        // DSP01 existe dans data.sql
        Commande commande = commandeService.creerCommande("DSP01");
        assertNotNull(commande.getNumero());
        assertEquals("DSP01", commande.getDispensaire().getCode());
        assertNotNull(commande.getAdresseLivraison());
        assertNull(commande.getEnvoyeele());
    }

    @Test
    void testCreerCommandeDispensaireInexistant() {
        assertThrows(NoSuchElementException.class, () -> {
            commandeService.creerCommande("XXXXX");
        });
    }

    @Test
    void testAjouterLigne() {
        // Créer une commande
        Commande commande = commandeService.creerCommande("DSP01");
        // Médicament 2 = Paracétamol 1000mg, stock = 350
        Ligne ligne = commandeService.ajouterLigne(commande.getNumero(), 2, 10);
        assertNotNull(ligne.getId());
        assertEquals(10, ligne.getQuantite());
    }

    @Test
    void testAjouterLigneMedicamentInexistant() {
        Commande commande = commandeService.creerCommande("DSP01");
        assertThrows(NoSuchElementException.class, () -> {
            commandeService.ajouterLigne(commande.getNumero(), 99999, 5);
        });
    }

    @Test
    void testAjouterLigneCommandeDejaExpediee() {
        Commande commande = commandeService.creerCommande("DSP01");
        commandeService.ajouterLigne(commande.getNumero(), 2, 5);
        commandeService.enregistreExpedition(commande.getNumero());

        assertThrows(IllegalStateException.class, () -> {
            commandeService.ajouterLigne(commande.getNumero(), 3, 3);
        });
    }

    @Test
    void testSupprimerLigne() {
        Commande commande = commandeService.creerCommande("DSP01");
        Ligne ligne = commandeService.ajouterLigne(commande.getNumero(), 2, 10);
        commandeService.supprimerLigne(ligne.getId());
        // Vérifier que la commande n'a plus de lignes
        Commande updated = commandeService.getCommande(commande.getNumero());
        assertTrue(updated.getLignes().isEmpty());
    }

    @Test
    void testEnregistreExpedition() {
        Commande commande = commandeService.creerCommande("DSP01");
        commandeService.ajouterLigne(commande.getNumero(), 2, 5);

        Commande expediee = commandeService.enregistreExpedition(commande.getNumero());
        assertNotNull(expediee.getEnvoyeele());
    }

    @Test
    void testEnregistreExpeditionDejaExpediee() {
        Commande commande = commandeService.creerCommande("DSP01");
        commandeService.ajouterLigne(commande.getNumero(), 2, 5);
        commandeService.enregistreExpedition(commande.getNumero());

        assertThrows(IllegalStateException.class, () -> {
            commandeService.enregistreExpedition(commande.getNumero());
        });
    }

    @Test
    void testGetCommande() {
        Commande commande = commandeService.creerCommande("DSP01");
        Commande found = commandeService.getCommande(commande.getNumero());
        assertEquals(commande.getNumero(), found.getNumero());
    }

    @Test
    void testGetCommandeInexistante() {
        assertThrows(NoSuchElementException.class, () -> {
            commandeService.getCommande(99999);
        });
    }

    @Test
    void testGetCommandeEnCoursPour() {
        // Créer une commande non expédiée
        commandeService.creerCommande("DSP01");
        var commandes = commandeService.getCommandeEnCoursPour("DSP01");
        assertFalse(commandes.isEmpty());
    }
}
