package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para el reporte de Marbetes Pendientes.
 * Marbetes que aún no tienen ambos conteos aplicados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingLabelsReportDTO {

    private Long numeroMarbete;
    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private String claveAlmacen;
    private String nombreAlmacen;
    private BigDecimal conteo1;
    private BigDecimal conteo2;
    private String estado;
}

