package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para el reporte de Listado de Marbetes.
 * Listado completo de todos los marbetes generados por almacén.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelListReportDTO {

    private Long numeroMarbete;
    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private String claveAlmacen;
    private String nombreAlmacen;
    private BigDecimal conteo1;
    private BigDecimal conteo2;
    private String estado; // "GENERADO", "IMPRESO", "CANCELADO"
    private Boolean cancelado;
}

