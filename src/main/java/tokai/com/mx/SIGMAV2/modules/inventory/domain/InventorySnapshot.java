package tokai.com.mx.SIGMAV2.modules.inventory.domain;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class InventorySnapshot {

    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long periodId;
    private BigDecimal existQty;
    private LocalDateTime createdAt;

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
}
