package miniprojet.dto;

import lombok.Data;

import java.util.List;

@Data
public class FournisseurDTO {
    private Long id;
    private String nom;
    private String email;
    private List<String> categorieLibelles;
}
