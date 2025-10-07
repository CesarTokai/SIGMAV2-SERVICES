package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class GenerateBatchDTO {

    @NotNull
    private Long productId;

    @NotNull
    private Long warehouseId;

    @NotNull
    private Long periodId;

    @NotNull
    @Min(1)
    private Integer labelsToGenerate;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Integer getLabelsToGenerate() { return labelsToGenerate; }
    public void setLabelsToGenerate(Integer labelsToGenerate) { this.labelsToGenerate = labelsToGenerate; }
}

