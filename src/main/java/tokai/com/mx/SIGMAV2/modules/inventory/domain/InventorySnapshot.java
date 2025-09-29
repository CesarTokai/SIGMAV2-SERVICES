package tokai.com.mx.SIGMAV2.modules.inventory.domain;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventorySnapshot {

    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long periodId;
    private BigDecimal existQty;
    private LocalDateTime createdAt;

    // Constructor, getters y setters
    public InventorySnapshot(Long id, Long productId, Long warehouseId, Long periodId, BigDecimal existQty, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.periodId = periodId;
        this.existQty = existQty;
        this.createdAt = createdAt;
    }

    public InventorySnapshot() {

    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public BigDecimal getExistQty() { return existQty; }
    public void setExistQty(BigDecimal existQty) { this.existQty = existQty; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
