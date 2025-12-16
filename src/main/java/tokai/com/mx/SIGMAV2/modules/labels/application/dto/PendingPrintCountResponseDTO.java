package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

/**
 * DTO de respuesta para el conteo de marbetes pendientes de impresión
 */
public class PendingPrintCountResponseDTO {

    private Long count;
    private Long periodId;
    private Long warehouseId;
    private String warehouseName;
    private String periodName;

    public PendingPrintCountResponseDTO() {}

    public PendingPrintCountResponseDTO(Long count, Long periodId, Long warehouseId, String warehouseName, String periodName) {
        this.count = count;
        this.periodId = periodId;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.periodName = periodName;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

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

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getPeriodName() {
        return periodName;
    }

    public void setPeriodName(String periodName) {
        this.periodName = periodName;
    }
}

