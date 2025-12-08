package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para el reporte de Producto con Detalle.
 * Desglose de inventario físico por producto con totales.
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
    private BigDecimal total; // Suma de existencias en todos los almacenes
}

