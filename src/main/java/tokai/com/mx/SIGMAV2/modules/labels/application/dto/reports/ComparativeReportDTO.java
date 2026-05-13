package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para el reporte Comparativo.
 * Diferencias entre existencias teóricas vs físicas por producto y almacén.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparativeReportDTO {

    private String claveAlmacen;
    private String nombreAlmacen;
    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private BigDecimal existenciasFisicas;
    private BigDecimal existenciasTeoricas;
    private BigDecimal diferencia;
    private BigDecimal porcentajeDiferencia;
}

