package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * DTO para imprimir marbetes de múltiples almacenes sin especificar warehouseId
 * El sistema autodetecta el almacén de cada marbete
 */
@Getter
@Setter
public class PrintSelectedLabelsAutoWarehouseDTO {

    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;

    @NotEmpty(message = "Debe proporcionar al menos un folio")
    private List<Long> folios;

    // Tipo de información a mostrar en el PDF
    private String infoType = "BASICA"; // BASICA, COMPLETA

    // Información adicional
    private String observaciones;
}

