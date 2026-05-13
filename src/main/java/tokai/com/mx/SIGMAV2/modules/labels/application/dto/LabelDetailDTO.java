package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelDetailDTO {
    private Long folio;
    private Long productId;
    private String claveProducto;
    private String nombreProducto;
    private Long warehouseId;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Long periodId;
    private String estado; // GENERADO, IMPRESO, CANCELADO
    private String createdAt;
    private String impresoAt;
    private Integer existencias; // Existencias del producto
}

