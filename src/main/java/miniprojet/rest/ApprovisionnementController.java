package miniprojet.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import miniprojet.service.ApprovisionnementService;

@RestController
@RequestMapping("/api/approvisionnement")
@Tag(name = "Approvisionnement", description = "API de gestion du réapprovisionnement")
public class ApprovisionnementController {

    private final ApprovisionnementService approvisionnementService;

    public ApprovisionnementController(ApprovisionnementService approvisionnementService) {
        this.approvisionnementService = approvisionnementService;
    }

    @Operation(summary = "Lancer le réapprovisionnement", description = "Identifie les médicaments en rupture de stock et envoie les demandes de devis aux fournisseurs.")
    @PostMapping("/lancer")
    public ResponseEntity<String> lancerApprovisionnement() {
        approvisionnementService.lancerApprovisionnement();
        return ResponseEntity.ok("Le processus d'approvisionnement a été lancé avec succès.");
    }
}
