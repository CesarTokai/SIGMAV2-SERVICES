package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabelStatusResponseDTO {
    private Long folio;
    private Long productId;
    private String claveProducto;
    private String nombreProducto;
    private Long warehouseId;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Long periodId;
    private String estado;
    private Boolean impreso;
    private String fechaImpresion;
    private String mensaje;
}

