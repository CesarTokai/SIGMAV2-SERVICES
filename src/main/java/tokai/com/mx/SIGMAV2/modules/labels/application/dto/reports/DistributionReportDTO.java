package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el reporte de Distribución de Marbetes.
 * Presenta la distribución de folios por almacén basado en marbetes impresos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributionReportDTO {

    private String usuario;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Long primerFolio;
    private Long ultimoFolio;
    private Integer totalMarbetes;
}

