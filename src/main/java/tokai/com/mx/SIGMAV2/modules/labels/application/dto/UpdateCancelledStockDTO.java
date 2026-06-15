package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCancelledStockDTO {

    @NotNull(message = "El folio es requerido")
    private Long folio;

    /** Período del marbete — opcional, mejora el lookup en sistemas multi-período */
    private Long periodId;

    @NotNull(message = "Las existencias son requeridas")
    private Integer existenciasActuales;

    private String notas;
}

