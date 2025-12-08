package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para el reporte de Marbetes con Diferencias.
 * Marbetes donde conteo1 ≠ conteo2 y ambos conteos están completos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifferencesReportDTO {

    private Long numeroMarbete;
    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private String claveAlmacen;
    private String nombreAlmacen;
    private BigDecimal conteo1;
    private BigDecimal conteo2;
    private BigDecimal diferencia;
    private String estado;
}

