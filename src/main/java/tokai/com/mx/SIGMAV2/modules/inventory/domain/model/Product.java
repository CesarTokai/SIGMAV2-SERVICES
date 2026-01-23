package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Product {

    private Long id;
    private String cveArt;
    private String descr;
    private String uniMed;
    private Status status;
    private LocalDateTime createdAt;
    private String linProd;

    public enum Status { A, B }


}
