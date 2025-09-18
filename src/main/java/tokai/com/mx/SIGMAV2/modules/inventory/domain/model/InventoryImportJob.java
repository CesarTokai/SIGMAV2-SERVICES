package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryImportJob {

    private Long id;
    private Product product;
    private Warehouse warehouse;
    private BigDecimal existQty;
    private InventoryStock.Status status;
    private LocalDateTime updatedAt;

    public enum Status { A, B }


    public BigDecimal getExistQty() {
        return existQty;
    }

    public void setExistQty(BigDecimal existQty) {
        this.existQty = existQty;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public InventoryStock.Status getStatus() {
        return status;
    }

    public void setStatus(InventoryStock.Status status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}
