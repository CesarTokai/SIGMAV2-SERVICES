package tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para el reporte de Almacén con Detalle.
 * Desglose de inventario físico por almacén.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDetailReportDTO {

    private String claveAlmacen;
    private String nombreAlmacen;
    private String claveProducto;
    private String descripcionProducto;
    private String unidad;
    private Long numeroMarbete;
    private BigDecimal cantidad;
    private String estado;
    private Boolean cancelado;
}

