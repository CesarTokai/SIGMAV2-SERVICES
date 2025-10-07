package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public class PrintRequestDTO {

    @NotNull
    private Long periodId;

    @NotNull
    private Long warehouseId;

    @NotNull
    @Min(1)
    private Long startFolio;

    @NotNull
    @Min(1)
    private Long endFolio;

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getStartFolio() { return startFolio; }
    public void setStartFolio(Long startFolio) { this.startFolio = startFolio; }

    public Long getEndFolio() { return endFolio; }
    public void setEndFolio(Long endFolio) { this.endFolio = endFolio; }
}

