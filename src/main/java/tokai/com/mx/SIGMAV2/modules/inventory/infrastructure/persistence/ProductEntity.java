package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product")
    private Long idProduct;

    @Column(name = "cve_art")
    private String cveArt;

    @Column(name = "descr")
    private String descr;

    @Column(name = "uni_med")
    private String uniMed;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "lin_prod")
    private String linProd;


}
