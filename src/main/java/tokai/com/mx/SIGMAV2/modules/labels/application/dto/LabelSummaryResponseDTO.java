package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LabelSummaryResponseDTO {
    private Long productId;
    private String claveProducto;
    private String nombreProducto;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Integer foliosSolicitados;
    private Integer foliosExistentes;
    private String estado;
    private Integer existencias;
}

