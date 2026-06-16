package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para cancelar un marbete.
 *
 * - folio: OBLIGATORIO
 * - periodId: OBLIGATORIO — necesario para identificar el marbete correcto
 *   cuando el mismo número de folio existe en múltiples períodos.
 * - warehouseId: OPCIONAL (para validación adicional de acceso)
 * - motivoCancelacion: OPCIONAL
 */
@Data
public class CancelLabelRequestDTO {

    @NotNull(message = "El folio es obligatorio")
    private Long folio;

    @NotNull(message = "El periodId es obligatorio")
    private Long periodId;

    private Long warehouseId;

    @Size(max = 500, message = "El motivo de cancelación no puede exceder 500 caracteres")
    @JsonProperty("motivoCancelacion")
    private String motivoCancelacion;
}

