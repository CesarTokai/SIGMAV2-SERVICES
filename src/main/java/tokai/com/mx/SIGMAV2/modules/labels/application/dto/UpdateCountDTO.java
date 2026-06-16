package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para actualizar un conteo existente (C1 o C2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCountDTO {

    @NotNull(message = "El folio es obligatorio")
    private Long folio;

    /**
     * Período al que pertenece el folio — requerido para lookup correcto
     * cuando el mismo número de folio existe en múltiples períodos.
     */
    @NotNull(message = "El periodId es obligatorio")
    private Long periodId;

    /**
     * Almacén al que pertenece el folio (opcional, para validación adicional)
     */
    private Long warehouseId;

    @NotNull(message = "El valor del conteo es obligatorio")
    @Positive(message = "El valor del conteo debe ser mayor a cero")
    private BigDecimal countedValue;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}

