package miniprojet.dto;
import lombok.Data;
@Data
public class LigneDTO {
    private Integer id;
    // Médicament pour la ligne
    private MedicamentDTO medicament;
    private Integer quantite;
}
