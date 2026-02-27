package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para cancelar un marbete desde la interfaz de conteo.
 *
 * IMPORTANTE:
 * - folio: OBLIGATORIO (se acepta como número o string)
 * - periodId: OPCIONAL (se obtiene del marbete si no se proporciona)
 * - warehouseId: OPCIONAL (se obtiene del marbete si no se proporciona)
 * - motivoCancelacion: OPCIONAL
 */
@Data
public class CancelLabelRequestDTO {

    @NotNull(message = "El folio es obligatorio")
    @JsonDeserialize(using = NumberDeserializers.LongDeserializer.class)
    private Long folio;

    // Opcionales: se obtienen del marbete si no se proporcionan
    private Long periodId;
    private Long warehouseId;

    @Size(max = 500, message = "El motivo de cancelación no puede exceder 500 caracteres")
    @JsonProperty("motivoCancelacion")
    private String motivoCancelacion;
}

