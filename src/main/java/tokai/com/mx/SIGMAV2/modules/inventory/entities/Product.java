package tokai.com.mx.SIGMAV2.modules.inventory.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product")
    private Long idProduct;
    
    @Column(name = "cve_art", unique = true, nullable = false, length = 64)
    private String cveArt;
    
    @Column(name = "descr", nullable = false)
    private String description;
    
    @Column(name = "uni_med", nullable = false, length = 50)
    private String unitOfMeasure;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.A;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum ProductStatus {
        A, B
    }
    
    // Constructors
    public Product() {}
    
    public Product(String cveArt, String description, String unitOfMeasure, ProductStatus status) {
        this.cveArt = cveArt;
        this.description = description;
        this.unitOfMeasure = unitOfMeasure;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getIdProduct() {
        return idProduct;
    }
    
    public void setIdProduct(Long idProduct) {
        this.idProduct = idProduct;
    }
    
    public String getCveArt() {
        return cveArt;
    }
    
    public void setCveArt(String cveArt) {
        this.cveArt = cveArt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    
    public ProductStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProductStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}