package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para solicitar el conteo de marbetes pendientes de impresión
 */
public class PendingPrintCountRequestDTO {

    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;

    @NotNull(message = "El almacén es obligatorio")
    private Long warehouseId;

    // Opcional: Contar solo marbetes de cierto producto
    private Long productId;

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
}

