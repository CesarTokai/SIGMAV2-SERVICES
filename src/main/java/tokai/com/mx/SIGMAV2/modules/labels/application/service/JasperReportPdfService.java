package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Servicio que genera PDFs de los reportes de marbetes usando JasperReports.
 *
 * Flujo:
 *  1. Recibe los datos ya consultados (List<DTO>)
 *  2. Carga la plantilla .jrxml desde cache (JasperReportCacheService)
 *  3. Convierte la lista en JRBeanCollectionDataSource
 *  4. Llena el reporte con parámetros + datasource
 *  5. Exporta a PDF en memoria y retorna byte[]
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JasperReportPdfService {

    private final JasperReportCacheService reportCacheService;

    // ── Ruta del directorio de reportes en el classpath ──────────────────
    private static final String REPORTS_CLASSPATH = "reports/";

    // ═══════════════════════════════════════════════════════════════════
    // PDF POR TIPO DE REPORTE
    // ═══════════════════════════════════════════════════════════════════

    public byte[] generateDistributionPdf(List<DistributionReportDTO> data) {
        log.info("Generando PDF distribución — {} registros", data.size());
        return buildPdf("distribucion_marbetes", data, baseParams());
    }

    public byte[] generateListPdf(List<LabelListReportDTO> data) {
        log.info("Generando PDF listado — {} registros", data.size());
        return buildPdf("listado_marbetes", data, baseParams());
    }

    public byte[] generatePendingPdf(List<PendingLabelsReportDTO> data) {
        log.info("Generando PDF pendientes — {} registros", data.size());
        return buildPdf("pendientes_marbetes", data, baseParams());
    }

    public byte[] generateDifferencesPdf(List<DifferencesReportDTO> data) {
        log.info("Generando PDF diferencias — {} registros", data.size());
        return buildPdf("diferencias_marbetes", data, baseParams());
    }

    public byte[] generateCancelledPdf(List<CancelledLabelsReportDTO> data) {
        log.info("Generando PDF cancelados — {} registros", data.size());
        return buildPdf("cancelados_marbetes", data, baseParams());
    }

    public byte[] generateComparativePdf(List<ComparativeReportDTO> data) {
        log.info("Generando PDF comparativo — {} registros", data.size());
        return buildPdf("comparativo_marbetes", data, baseParams());
    }

    public byte[] generateWarehouseDetailPdf(List<WarehouseDetailReportDTO> data) {
        log.info("Generando PDF almacén-detalle — {} registros", data.size());
        return buildPdf("detalle_almacen_marbetes", data, baseParams());
    }

    public byte[] generateProductDetailPdf(List<ProductDetailReportDTO> data) {
        log.info("Generando PDF producto-detalle — {} registros", data.size());
        return buildPdf("detalle_producto_marbetes", data, baseParams());
    }

    // ═══════════════════════════════════════════════════════════════════
    // MOTOR CENTRAL DE GENERACIÓN
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Genera un PDF para cualquier plantilla y cualquier lista de DTOs.
     *
     * @param templateName nombre del .jrxml sin extensión
     * @param data         lista de DTOs (campos deben coincidir con los <field> del .jrxml)
     * @param params       parámetros adicionales (fecha, REPORT_DIR, etc.)
     * @return byte[] del PDF generado en memoria
     */
    private <T> byte[] buildPdf(String templateName, List<T> data, Map<String, Object> params) {
        try {
            JasperReport jasperReport = reportCacheService.getReport(templateName);

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baos));

            SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
            config.setCompressed(true);
            exporter.setConfiguration(config);
            exporter.exportReport();

            log.info("PDF generado correctamente: {} — {} bytes", templateName, baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando PDF para plantilla {}: {}", templateName, e.getMessage(), e);
            throw new RuntimeException("Error generando PDF del reporte '" + templateName + "': " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PARÁMETROS BASE
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Construye los parámetros comunes a todos los reportes:
     *  - fecha: fecha actual formateada
     *  - REPORT_DIR: ruta al directorio de recursos para cargar logos
     */
    private Map<String, Object> baseParams() {
        Map<String, Object> params = new HashMap<>();

        // Fecha actual formateada
        String fecha = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "MX")));
        params.put("fecha", fecha);

        // Ruta del directorio de reportes para que el .jrxml encuentre los logos
        try {
            String reportDir = new ClassPathResource(REPORTS_CLASSPATH).getURL().toString();
            params.put("REPORT_DIR", reportDir);
        } catch (Exception e) {
            log.warn("No se pudo resolver REPORT_DIR desde classpath, usando ruta vacía: {}", e.getMessage());
            params.put("REPORT_DIR", "");
        }

        return params;
    }
}

