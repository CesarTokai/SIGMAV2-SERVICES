package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CountEventDTO {

    @NotNull(message = "El folio es obligatorio")
    private Long folio;

    @NotNull(message = "El valor del conteo es obligatorio")
    @Positive(message = "El valor del conteo debe ser mayor a cero")
    private BigDecimal countedValue;

    /**
     * Periodo al que se espera que pertenezca el folio (opcional para validación)
     * Si se proporciona, se valida que el marbete pertenezca a este periodo
     */
    private Long periodId;

    /**
     * Almacén al que se espera que pertenezca el folio (opcional para validación)
     * Si se proporciona, se valida que el marbete pertenezca a este almacén
     */
    private Long warehouseId;

    public Long getFolio() { return folio; }
    public void setFolio(Long folio) { this.folio = folio; }

    public BigDecimal getCountedValue() { return countedValue; }
    public void setCountedValue(BigDecimal countedValue) { this.countedValue = countedValue; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
}

