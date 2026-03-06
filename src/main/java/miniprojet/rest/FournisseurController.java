package miniprojet.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import miniprojet.dto.FournisseurDTO;
import miniprojet.entity.Categorie;
import miniprojet.entity.Fournisseur;
import miniprojet.service.FournisseurService;

@Slf4j
@RestController
@RequestMapping(path = "/api/fournisseurs")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    public FournisseurController(FournisseurService fournisseurService) {
        this.fournisseurService = fournisseurService;
    }

    /**
     * Récupère tous les fournisseurs
     */
    @GetMapping
    public ResponseEntity<List<FournisseurDTO>> getAll() {
        log.info("Contrôleur : récupérer tous les fournisseurs");
        List<FournisseurDTO> result = fournisseurService.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Récupère un fournisseur par son id
     */
    @GetMapping("/{id}")
    public ResponseEntity<FournisseurDTO> getById(@PathVariable Long id) {
        log.info("Contrôleur : récupérer le fournisseur {}", id);
        Fournisseur fournisseur = fournisseurService.findById(id);
        return ResponseEntity.ok(toDTO(fournisseur));
    }

    /**
     * Recherche les fournisseurs par nom (contient)
     */
    @GetMapping("/search")
    public ResponseEntity<List<FournisseurDTO>> searchByNom(@RequestParam String nom) {
        log.info("Contrôleur : rechercher les fournisseurs contenant '{}'", nom);
        List<FournisseurDTO> result = fournisseurService.findByNomContaining(nom)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Recherche les fournisseurs par catégorie
     */
    @GetMapping("/categorie/{categorieCode}")
    public ResponseEntity<List<FournisseurDTO>> getByCategorieCode(@PathVariable Integer categorieCode) {
        log.info("Contrôleur : récupérer les fournisseurs pour la catégorie {}", categorieCode);
        List<FournisseurDTO> result = fournisseurService.findByCategorieCode(categorieCode)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * Crée un nouveau fournisseur
     */
    @PostMapping
    public ResponseEntity<FournisseurDTO> create(@RequestParam String nom, @RequestParam String email) {
        log.info("Contrôleur : créer le fournisseur '{}' avec email '{}'", nom, email);
        Fournisseur fournisseur = fournisseurService.creerFournisseur(nom, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(fournisseur));
    }

    /**
     * Met à jour un fournisseur
     */
    @PutMapping("/{id}")
    public ResponseEntity<FournisseurDTO> update(
            @PathVariable Long id,
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String email) {
        log.info("Contrôleur : mettre à jour le fournisseur {}", id);
        Fournisseur fournisseur = fournisseurService.updateFournisseur(id, nom, email);
        return ResponseEntity.ok(toDTO(fournisseur));
    }

    /**
     * Supprime un fournisseur
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Contrôleur : supprimer le fournisseur {}", id);
        fournisseurService.deleteFournisseur(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Associe un fournisseur à une catégorie
     */
    @PostMapping("/{fournisseurId}/categories/{categorieCode}")
    public ResponseEntity<FournisseurDTO> ajouterCategorie(
            @PathVariable Long fournisseurId,
            @PathVariable Integer categorieCode) {
        log.info("Contrôleur : associer fournisseur {} à catégorie {}", fournisseurId, categorieCode);
        Fournisseur fournisseur = fournisseurService.ajouterCategorie(fournisseurId, categorieCode);
        return ResponseEntity.ok(toDTO(fournisseur));
    }

    /**
     * Retire l'association d'un fournisseur à une catégorie
     */
    @DeleteMapping("/{fournisseurId}/categories/{categorieCode}")
    public ResponseEntity<FournisseurDTO> retirerCategorie(
            @PathVariable Long fournisseurId,
            @PathVariable Integer categorieCode) {
        log.info("Contrôleur : retirer fournisseur {} de catégorie {}", fournisseurId, categorieCode);
        Fournisseur fournisseur = fournisseurService.retirerCategorie(fournisseurId, categorieCode);
        return ResponseEntity.ok(toDTO(fournisseur));
    }

    /**
     * Convertit une entité Fournisseur en DTO
     */
    private FournisseurDTO toDTO(Fournisseur fournisseur) {
        FournisseurDTO dto = new FournisseurDTO();
        dto.setId(fournisseur.getId());
        dto.setNom(fournisseur.getNom());
        dto.setEmail(fournisseur.getEmail());
        dto.setCategorieLibelles(
            fournisseur.getCategories().stream()
                .map(Categorie::getLibelle)
                .collect(Collectors.toList())
        );
        return dto;
    }
}
