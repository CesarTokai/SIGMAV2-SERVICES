package tokai.com.mx.SIGMAV2.modules.labels.application.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}

