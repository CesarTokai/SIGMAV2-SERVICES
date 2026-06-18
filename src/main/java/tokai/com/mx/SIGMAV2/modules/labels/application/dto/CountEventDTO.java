package tokai.com.mx.SIGMAV2.modules.labels.application.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class CountEventDTO {

    @NotNull(message = "El folio es obligatorio")
    private Long folio;

    @NotNull(message = "El valor del conteo es obligatorio")
    @Positive(message = "El valor del conteo debe ser mayor a cero")
    private BigDecimal countedValue;

    /**
     * Periodo al que se espera que pertenezca el folio — requerido para lookup correcto
     * cuando el mismo número de folio existe en múltiples períodos.
     */
    @NotNull(message = "El periodId es obligatorio")
    private Long periodId;

    /**
     * Almacén al que se espera que pertenezca el folio (opcional para validación)
     * Si se proporciona, se valida que el marbete pertenezca a este almacén
     */
    private Long warehouseId;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;
}

