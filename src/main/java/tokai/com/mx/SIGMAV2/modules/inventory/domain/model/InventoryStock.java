package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de dominio para existencias de inventario
 * Representa las cantidades de productos por almacén y periodo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStock {
    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long periodId;
    private BigDecimal existQty;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Estados posibles del inventario
     */
    public static final String STATUS_ACTIVE = "A";
    public static final String STATUS_INACTIVE = "B";
}
