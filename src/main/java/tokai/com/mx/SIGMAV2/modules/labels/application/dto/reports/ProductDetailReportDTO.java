package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para el reporte de Producto con Detalle.
 * Desglose de inventario físico por producto, agrupado por producto → almacén → marbete.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailReportDTO {

    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Long numeroMarbete;
    private BigDecimal existencias;
    private BigDecimal total;
    /** "C2", "C1", "SIN_CONTEO" o "CANCELADO" — de qué conteo proviene el valor de existencias. */
    private String fuenteConteo;
    /** Estado del marbete: "GENERADO", "IMPRESO" o "CANCELADO". */
    private String estado;
    /** true si el marbete está cancelado. */
    private Boolean cancelado;
}



