package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.ProductInfoPort;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.ProductInfoPort.ProductInfo;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.WarehouseInfoPort;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.WarehouseInfoPort.WarehouseInfo;

import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

/**
 * Servicio para generar PDFs de marbetes usando JasperReports
 * REFACTORIZADO: Ahora usa cache de reportes para mejor rendimiento
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JasperLabelPrintService {

    private final ProductInfoPort productInfoPort;
    private final WarehouseInfoPort warehouseInfoPort;
    private final JasperReportCacheService reportCacheService;

    /**
     * Genera un PDF con los marbetes especificados usando la plantilla JRXML
     * REFACTORIZADO: Usa cache de reportes para evitar compilación repetida
     *
     * @param labels Lista de marbetes a imprimir
     * @return byte[] del PDF generado
     */
    public byte[] generateLabelsPdf(List<Label> labels) {
        return generateLabelsPdf(labels, true);
    }

    /**
     * Genera un PDF con los marbetes especificados usando la plantilla JRXML (CON SOPORTE QR)
     * 
     * @param labels Lista de marbetes a imprimir
     * @param withQR true si genera PDF con QR; false para plantilla normal
     * @return byte[] del PDF generado
     */
    public byte[] generateLabelsPdf(List<Label> labels, boolean withQR) {
        String templateName = withQR ? "Carta_Tres_Cuadros_QR" : "Carta_Tres_Cuadros";
        log.info("Generando PDF con JasperReports para {} marbetes (withQR={}, template={})...", 
                 labels.size(), withQR, templateName);
        long startTime = System.currentTimeMillis();

        try {
            // Pre-cargar productos y almacenes para evitar N+1 queries
            Map<Long, ProductInfo> productsCache = loadProductsCache(labels);
            Map<Long, WarehouseInfo> warehousesCache = loadWarehousesCache(labels);

            // MEJORA #1: Usar cache de reportes en lugar de compilar cada vez
            JasperReport jasperReport = reportCacheService.getReport(templateName);

            // Convertir labels a estructura de datos para JasperReports
            List<Map<String, Object>> dataSource = buildDataSource(labels, productsCache, warehousesCache, withQR);

            log.info("DataSource construido con {} registros", dataSource.size());

            // Validar que el datasource no esté vacío
            if (dataSource.isEmpty()) {
                log.error("El datasource está vacío. No se puede generar el PDF.");
                log.error("Esto puede ocurrir si:");
                log.error("- Los productos asociados a los marbetes no existen en la base de datos");
                log.error("- Los almacenes asociados a los marbetes no existen en la base de datos");
                log.error("- Hay datos huérfanos en la tabla labels");
                throw new RuntimeException(
                    "No se puede generar el PDF: El datasource está vacío. " +
                    "Verifique que todos los productos y almacenes asociados a los marbetes existan en la base de datos."
                );
            }

            // Llenar el reporte con los datos
            JRBeanCollectionDataSource jrDataSource = new JRBeanCollectionDataSource(dataSource);
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport,
                new HashMap<>(), // Parámetros globales (vacío por ahora)
                jrDataSource
            );

            // Exportar a PDF
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            long endTime = System.currentTimeMillis();
            log.info("PDF generado exitosamente en {} ms ({} KB)",
                (endTime - startTime), pdfBytes.length / 1024);

            return pdfBytes;

        } catch (Exception e) {
            log.error("Error generando PDF con JasperReports", e);
            throw new RuntimeException("Error generando PDF de marbetes: " + e.getMessage(), e);
        }
    }


    /**
     * Pre-carga todos los productos en un mapa para evitar queries repetidas
     */
    private Map<Long, ProductInfo> loadProductsCache(List<Label> labels) {
        Set<Long> productIds = new HashSet<>();
        for (Label label : labels) productIds.add(label.getProductId());
        Map<Long, ProductInfo> cache = new HashMap<>();
        for (ProductInfo p : productInfoPort.findAllById(productIds)) cache.put(p.id(), p);
        log.info("Cache de productos cargado: {} productos", cache.size());
        return cache;
    }

    private Map<Long, WarehouseInfo> loadWarehousesCache(List<Label> labels) {
        Set<Long> warehouseIds = new HashSet<>();
        for (Label label : labels) warehouseIds.add(label.getWarehouseId());
        Map<Long, WarehouseInfo> cache = new HashMap<>();
        for (WarehouseInfo w : warehouseInfoPort.findAllById(warehouseIds)) cache.put(w.id(), w);
        log.info("Cache de almacenes cargado: {} almacenes", cache.size());
        return cache;
    }

    /**
     * Construye la estructura de datos para JasperReports
     * Para QR: Agrupa de 3 en 3 (nomMarbete1/2/3, clave1/2/3, etc.)
     * Para Normal: Un marbete por registro
     */
    private List<Map<String, Object>> buildDataSource(
            List<Label> labels,
            Map<Long, ProductInfo> productsCache,
            Map<Long, WarehouseInfo> warehousesCache,
            boolean withQR) {

        List<Map<String, Object>> dataSource = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fechaActual = LocalDate.now().format(dateFormatter);

        // Pre-procesar todos los marbetes para obtener sus datos
        List<Map<String, String>> labelDataList = new ArrayList<>();
        for (Label label : labels) {
            ProductInfo product = productsCache.get(label.getProductId());
            if (product == null) {
                log.error("CRÍTICO: Producto no encontrado para folio {}: productId={}",
                    label.getFolio(), label.getProductId());
                throw new IllegalStateException(
                    String.format("No se puede generar PDF: El folio %d está asociado a un producto inexistente (ID: %d). " +
                        "Esto indica datos huérfanos en la base de datos. Por favor, contacte al administrador del sistema.",
                        label.getFolio(), label.getProductId()));
            }

            WarehouseInfo warehouse = warehousesCache.get(label.getWarehouseId());
            if (warehouse == null) {
                log.error("CRÍTICO: Almacén no encontrado para folio {}: warehouseId={}",
                    label.getFolio(), label.getWarehouseId());
                throw new IllegalStateException(
                    String.format("No se puede generar PDF: El folio %d está asociado a un almacén inexistente (ID: %d). " +
                        "Esto indica datos huérfanos en la base de datos. Por favor, contacte al administrador del sistema.",
                        label.getFolio(), label.getWarehouseId()));
            }

            String folio = String.valueOf(label.getFolio());
            String codigo = product.cveArt();

            String descripcion = product.descr();
            if (descripcion != null && descripcion.length() > 40) {
                descripcion = descripcion.substring(0, 37) + "...";
            }
            descripcion = descripcion != null ? descripcion : "";

            String almacen = warehouse.warehouseKey() + " " + warehouse.nameWarehouse();
            
            Map<String, String> labelData = new HashMap<>();
            labelData.put("folio", folio);
            labelData.put("codigo", codigo);
            labelData.put("descripcion", descripcion);
            labelData.put("almacen", almacen);
            labelData.put("fecha", fechaActual);
            
            // Generar QR si es necesario
            if (withQR) {
                BufferedImage qrImage = generateQRCode(folio);
                labelData.put("_qrImage", qrImage.toString()); // Usar toString() como placeholder
            }
            
            labelDataList.add(labelData);
        }

        if (withQR) {
            // Para QR: Agrupar de 3 en 3
            for (int i = 0; i < labelDataList.size(); i += 3) {
                Map<String, Object> record = new HashMap<>();
                
                // Marbete 1 (siempre existe)
                Map<String, String> label1Data = labelDataList.get(i);
                record.put("nomMarbete1", label1Data.get("folio"));
                record.put("clave1", label1Data.get("codigo"));
                record.put("descr1", label1Data.get("descripcion"));
                record.put("almacen1", label1Data.get("almacen"));
                record.put("fecha1", label1Data.get("fecha"));
                record.put("qrImage1", generateQRCode(label1Data.get("folio")));
                
                // Marbete 2 (si existe)
                if (i + 1 < labelDataList.size()) {
                    Map<String, String> label2Data = labelDataList.get(i + 1);
                    record.put("nomMarbete2", label2Data.get("folio"));
                    record.put("clave2", label2Data.get("codigo"));
                    record.put("descr2", label2Data.get("descripcion"));
                    record.put("almacen2", label2Data.get("almacen"));
                    record.put("fecha2", label2Data.get("fecha"));
                    record.put("qrImage2", generateQRCode(label2Data.get("folio")));
                } else {
                    // Llenar con vacíos
                    record.put("nomMarbete2", "");
                    record.put("clave2", "");
                    record.put("descr2", "");
                    record.put("almacen2", "");
                    record.put("fecha2", "");
                    record.put("qrImage2", null);
                }
                
                // Marbete 3 (si existe)
                if (i + 2 < labelDataList.size()) {
                    Map<String, String> label3Data = labelDataList.get(i + 2);
                    record.put("nomMarbete3", label3Data.get("folio"));
                    record.put("clave3", label3Data.get("codigo"));
                    record.put("descr3", label3Data.get("descripcion"));
                    record.put("almacen3", label3Data.get("almacen"));
                    record.put("fecha3", label3Data.get("fecha"));
                    record.put("qrImage3", generateQRCode(label3Data.get("folio")));
                } else {
                    // Llenar con vacíos
                    record.put("nomMarbete3", "");
                    record.put("clave3", "");
                    record.put("descr3", "");
                    record.put("almacen3", "");
                    record.put("fecha3", "");
                    record.put("qrImage3", null);
                }
                
                dataSource.add(record);
            }
        } else {
            // Para Normal: Un marbete por registro (como estaba antes)
            for (Map<String, String> labelData : labelDataList) {
                Map<String, Object> record = new HashMap<>();
                record.put("nomMarbete1", labelData.get("folio"));
                record.put("clave1", labelData.get("codigo"));
                record.put("descr1", labelData.get("descripcion"));
                record.put("almacen1", labelData.get("almacen"));
                record.put("fecha1", labelData.get("fecha"));
                dataSource.add(record);
            }
        }

        return dataSource;
    }

    /**
     * Genera una imagen QR para el folio especificado
     * Usa ZXing para crear el código QR
     */
    private BufferedImage generateQRCode(String folio) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(folio, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            log.debug("✓ QR generado para folio: {}", folio);
            return qrImage;
        } catch (Exception e) {
            log.error("Error generando QR para folio {}: {}", folio, e.getMessage());
            // Retornar una imagen en blanco para evitar errores
            return new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        }
    }
}

