package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "ProductEntity")
public class ProductEntity {

    private Long id;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product")
    private Long idProduct;

    // Otros campos según tu modelo
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }



    public Long getIdProduct() {
        return idProduct;
    }

    public void setIdProduct(Long idProduct) {
        this.idProduct = idProduct;
    }


    public void setCveArt(String cveArt) { this.cveArt = cveArt; }

    public void setDescr(String descr) { this.descr = descr; }

    public void setUniMed(String uniMed) { this.uniMed = uniMed; }

    public void setStatus(String status) { this.status = status; }

    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }


}
