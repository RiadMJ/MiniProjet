package miniprojet.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import miniprojet.service.NotificationService;

import java.util.Map;

/**
 * Contrôleur REST pour déclencher manuellement les vérifications de stock
 * et les notifications de réapprovisionnement.
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Vérifie tous les médicaments et envoie des notifications aux fournisseurs
     * pour ceux qui ont atteint leur niveau de réapprovisionnement.
     *
     * @return le nombre de médicaments nécessitant un réapprovisionnement
     */
    @PostMapping("/verifier-stock")
    public ResponseEntity<Map<String, Object>> verifierStock() {
        log.info("Contrôleur : vérification du stock et notification des fournisseurs");
        int count = notificationService.verifierTousLesMedicaments();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", count + " médicament(s) nécessite(nt) un réapprovisionnement",
            "count", count
        ));
    }
}
