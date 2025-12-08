package tokai.com.mx.SIGMAV2.modules.labels.domain.exception;

/**
 * Excepción lanzada cuando no se encuentran datos para generar un reporte.
 */
public class ReportDataNotFoundException extends RuntimeException {

    public ReportDataNotFoundException(String message) {
        super(message);
    }

    public ReportDataNotFoundException(String reportType, Long periodId, Long warehouseId) {
        super(String.format("No se encontraron datos para el reporte %s con periodo %d y almacén %d",
            reportType, periodId, warehouseId));
    }
}

