package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para buscar marbete por folio, periodo y almacén (POST).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelForCountRequestDTO {
    @NotNull(message = "El folio es obligatorio")
    private Long folio;
    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;
    @NotNull(message = "El almacén es obligatorio")
    private Long warehouseId;
}

