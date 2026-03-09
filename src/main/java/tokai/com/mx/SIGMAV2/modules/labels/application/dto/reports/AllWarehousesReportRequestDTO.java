package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para solicitar reportes de todos los almacenes (sin filtrar por almacén específico).
 */
@Data
public class AllWarehousesReportRequestDTO {

    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;
}

