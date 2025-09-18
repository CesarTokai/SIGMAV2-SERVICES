package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventorySnapshot {

    private Long id;
    private Product product;
    private Warehouse warehouse;
    private Period period;
    private BigDecimal existQty;
    private LocalDateTime createdAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

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

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
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
}
