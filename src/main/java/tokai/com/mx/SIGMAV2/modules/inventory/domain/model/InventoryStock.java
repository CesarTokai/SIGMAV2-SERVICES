package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryStock {
    private Long id;
    private Long productId;
    private Long warehouseId;
    private BigDecimal existQty;
    private String status;
    private LocalDateTime updatedAt;

    // No explicit constructors; Lombok will generate builder and constructors
}
