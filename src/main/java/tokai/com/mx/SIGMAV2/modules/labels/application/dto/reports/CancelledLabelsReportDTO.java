package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para el reporte de Marbetes Cancelados.
 * Listado de marbetes que fueron cancelados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelledLabelsReportDTO {

    private Long numeroMarbete;
    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private String claveAlmacen;
    private String nombreAlmacen;
    private BigDecimal conteo1;
    private BigDecimal conteo2;
    private String motivoCancelacion;
    private LocalDateTime canceladoAt;
    private String canceladoPor;
}

