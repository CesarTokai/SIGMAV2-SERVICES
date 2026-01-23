package tokai.com.mx.SIGMAV2.modules.labels.application.dto;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class PrintRequestDTO {

    @NotNull
    private Long periodId;

    @NotNull
    private Long warehouseId;

    // Opcional: Lista específica de folios a imprimir (para reimpresión)
    private List<Long> folios;

    // Opcional: Imprimir solo marbetes de cierto producto
    private Long productId;

    // Flag para forzar reimpresión de marbetes ya impresos
    private Boolean forceReprint = false;
}

