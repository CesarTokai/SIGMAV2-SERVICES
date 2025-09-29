package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryStockDTO {
    private Long id;
    private Long productId;
    private Long warehouseId;
    private BigDecimal existQty;
    private String status;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public BigDecimal getExistQty() { return existQty; }
    public void setExistQty(BigDecimal existQty) { this.existQty = existQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
