package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class InventorySnapshotDTO {
    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long periodId;
    private BigDecimal existQty;
    private LocalDateTime createdAt;


}
