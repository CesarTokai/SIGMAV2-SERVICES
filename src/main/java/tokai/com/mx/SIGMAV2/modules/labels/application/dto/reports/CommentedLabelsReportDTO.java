package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para reporte de marbetes con comentarios de conteos C1 y C2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentedLabelsReportDTO {

    private Long numeroMarbete;
    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private String claveAlmacen;
    private String nombreAlmacen;
    private BigDecimal conteo1;
    private String comentarioC1;
    private BigDecimal conteo2;
    private String comentarioC2;
    private BigDecimal diferencia;
    private String estado;
    private String statusConteo; // COMPLETO / INCOMPLETO / PENDIENTE
}
