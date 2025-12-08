package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO para cancelar un marbete desde la interfaz de conteo.
 */
@Data
public class CancelLabelRequestDTO {

    @NotNull(message = "El folio es obligatorio")
    private Long folio;

    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;

    @NotNull(message = "El almacén es obligatorio")
    private Long warehouseId;

    private String motivoCancelacion;
}

