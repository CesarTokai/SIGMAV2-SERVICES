package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCancelledDTO {

    private Long idLabelCancelled;
    private Long folio;
    private Long productId;
    private String claveProducto;
    private String nombreProducto;
    private Long warehouseId;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Long periodId;
    private Integer existenciasAlCancelar;
    private Integer existenciasActuales;
    private String motivoCancelacion;
    private String canceladoAt;
    private Boolean reactivado;
    private String reactivadoAt;
    private String notas;
}

