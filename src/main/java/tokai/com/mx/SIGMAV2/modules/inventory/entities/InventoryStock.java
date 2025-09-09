package tokai.com.mx.SIGMAV2.modules.inventory.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_stock")
public class InventoryStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stock")
    private Long idStock;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_warehouse", nullable = false)
    private Warehouse warehouse;
    
    @Column(name = "exist_qty", nullable = false, precision = 18, scale = 2)
    private BigDecimal existQty = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StockStatus status = StockStatus.A;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum StockStatus {
        A, B
    }
    
    // Constructors
    public InventoryStock() {}
    
    public InventoryStock(Product product, Warehouse warehouse, BigDecimal existQty, StockStatus status) {
        this.product = product;
        this.warehouse = warehouse;
        this.existQty = existQty;
        this.status = status;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getIdStock() {
        return idStock;
    }
    
    public void setIdStock(Long idStock) {
        this.idStock = idStock;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public Warehouse getWarehouse() {
        return warehouse;
    }
    
    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
    
    public BigDecimal getExistQty() {
        return existQty;
    }
    
    public void setExistQty(BigDecimal existQty) {
        this.existQty = existQty;
    }
    
    public StockStatus getStatus() {
        return status;
    }
    
    public void setStatus(StockStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}