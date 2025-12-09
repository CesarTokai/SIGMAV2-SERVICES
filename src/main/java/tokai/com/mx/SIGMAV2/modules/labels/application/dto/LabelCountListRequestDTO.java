package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar la lista de marbetes disponibles para conteo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCountListRequestDTO {

    @NotNull(message = "El ID del periodo es obligatorio")
    private Long periodId;

    @NotNull(message = "El ID del almacén es obligatorio")
    private Long warehouseId;
}

