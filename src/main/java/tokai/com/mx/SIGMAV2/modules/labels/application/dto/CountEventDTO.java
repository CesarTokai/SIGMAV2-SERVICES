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
 * DTO unificado para registrar y actualizar conteos (C1 y C2).
 * Soporta tanto operaciones de REGISTRO (registerCountC1/C2) como ACTUALIZACIÓN (updateCountC1/C2).
 *
 * Campo 'operation' indica si es nuevo registro o actualización:
 * - REGISTER: conteo nuevo (usado por registerCountC1/C2)
 * - UPDATE: actualizar conteo existente (usado por updateCountC1/C2)
 *
 * Default: REGISTER si no se especifica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountEventDTO {

    public enum Operation {
        REGISTER,  // Registrar nuevo conteo
        UPDATE     // Actualizar conteo existente
    }

    @NotNull(message = "El folio es obligatorio")
    private Long folio;

    @NotNull(message = "El valor del conteo es obligatorio")
    @Positive(message = "El valor del conteo debe ser mayor a cero")
    private BigDecimal countedValue;

    @NotNull(message = "El período es obligatorio")
    private Long periodId;

    private Long warehouseId;

    @Builder.Default
    private Operation operation = Operation.REGISTER;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    @Size(max = 600, message = "El comentario no puede exceder 600 caracteres")
    private String comment;
}

