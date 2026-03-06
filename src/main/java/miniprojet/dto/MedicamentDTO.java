package miniprojet.dto;
import lombok.Data;
@Data
public class MedicamentDTO {
    private Integer reference;
    // nom du médicament pour la ligne
    private String nom;
    private Integer prixUnitaire;
}
