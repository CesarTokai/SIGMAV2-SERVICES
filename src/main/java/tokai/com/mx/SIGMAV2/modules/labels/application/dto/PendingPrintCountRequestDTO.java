package tokai.com.mx.SIGMAV2.modules.labels.application.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PendingPrintCountRequestDTO {

    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;

    @NotNull(message = "El almacén es obligatorio")
    private Long warehouseId;

    // Opcional: Contar solo marbetes de cierto producto
    private Long productId;
}

