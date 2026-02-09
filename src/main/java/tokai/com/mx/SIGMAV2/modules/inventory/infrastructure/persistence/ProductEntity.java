package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(columnNames = "cve_art", name = "uk_products_cve_art")
})
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product")
    private Long idProduct;

    @Column(name = "cve_art", unique = true, nullable = false)
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
