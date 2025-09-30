package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
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

    public String getLinProd() { return linProd; }
    public void setLinProd(String linProd) { this.linProd = linProd; }


}
