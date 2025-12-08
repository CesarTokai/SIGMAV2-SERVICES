package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO base para filtros de reportes de marbetes.
 */
@Data
public class ReportFilterDTO {

    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;

    /**
     * Almacén específico o null para todos los almacenes
     */
    private Long warehouseId;
}

