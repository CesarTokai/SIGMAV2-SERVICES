package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio para generar PDFs de marbetes usando JasperReports
 * REFACTORIZADO: Ahora usa cache de reportes para mejor rendimiento
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JasperLabelPrintService {

    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JasperReportCacheService reportCacheService;

    /**
     * Genera un PDF con los marbetes especificados usando la plantilla JRXML
     * REFACTORIZADO: Usa cache de reportes para evitar compilación repetida
     *
     * @param labels Lista de marbetes a imprimir
     * @return byte[] del PDF generado
     */
    public byte[] generateLabelsPdf(List<Label> labels) {
        log.info("Generando PDF con JasperReports para {} marbetes...", labels.size());
        long startTime = System.currentTimeMillis();

        try {
            // Pre-cargar productos y almacenes para evitar N+1 queries
            Map<Long, ProductEntity> productsCache = loadProductsCache(labels);
            Map<Long, WarehouseEntity> warehousesCache = loadWarehousesCache(labels);

            // MEJORA #1: Usar cache de reportes en lugar de compilar cada vez
            JasperReport jasperReport = reportCacheService.getReport("Carta_Tres_Cuadros");

            // Convertir labels a estructura de datos para JasperReports
            List<Map<String, Object>> dataSource = buildDataSource(labels, productsCache, warehousesCache);

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
    private Map<Long, ProductEntity> loadProductsCache(List<Label> labels) {
        Set<Long> productIds = new HashSet<>();
        for (Label label : labels) {
            productIds.add(label.getProductId());
        }

        List<ProductEntity> products = productRepository.findAllById(productIds);
        Map<Long, ProductEntity> cache = new HashMap<>();
        for (ProductEntity product : products) {
            cache.put(product.getIdProduct(), product);
        }

        log.info("Cache de productos cargado: {} productos", cache.size());
        return cache;
    }

    /**
     * Pre-carga todos los almacenes en un mapa para evitar queries repetidas
     */
    private Map<Long, WarehouseEntity> loadWarehousesCache(List<Label> labels) {
        Set<Long> warehouseIds = new HashSet<>();
        for (Label label : labels) {
            warehouseIds.add(label.getWarehouseId());
        }

        List<WarehouseEntity> warehouses = warehouseRepository.findAllById(warehouseIds);
        Map<Long, WarehouseEntity> cache = new HashMap<>();
        for (WarehouseEntity warehouse : warehouses) {
            cache.put(warehouse.getIdWarehouse(), warehouse);
        }

        log.info("Cache de almacenes cargado: {} almacenes", cache.size());
        return cache;
    }

    /**
     * Construye la estructura de datos para JasperReports
     * Cada elemento del DataSource representa UN REGISTRO (puede ser una fila de 3 marbetes)
     */
    private List<Map<String, Object>> buildDataSource(
            List<Label> labels,
            Map<Long, ProductEntity> productsCache,
            Map<Long, WarehouseEntity> warehousesCache) {

        List<Map<String, Object>> dataSource = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String fechaActual = LocalDate.now().format(dateFormatter);

        // El diseño tiene 3 marbetes por fila, pero JasperReports maneja esto automáticamente
        // con el detalle del reporte. Solo necesitamos pasar cada marbete como un registro
        for (Label label : labels) {
            Map<String, Object> record = new HashMap<>();

            // CORRECCIÓN ERROR #4: Lanzar excepción en lugar de continuar silenciosamente
            // Obtener datos del producto
            ProductEntity product = productsCache.get(label.getProductId());
            if (product == null) {
                log.error("CRÍTICO: Producto no encontrado para folio {}: productId={}",
                    label.getFolio(), label.getProductId());
                throw new IllegalStateException(
                    String.format("No se puede generar PDF: El folio %d está asociado a un producto inexistente (ID: %d). " +
                        "Esto indica datos huérfanos en la base de datos. Por favor, contacte al administrador del sistema.",
                        label.getFolio(), label.getProductId()));
            }

            // Obtener datos del almacén
            WarehouseEntity warehouse = warehousesCache.get(label.getWarehouseId());
            if (warehouse == null) {
                log.error("CRÍTICO: Almacén no encontrado para folio {}: warehouseId={}",
                    label.getFolio(), label.getWarehouseId());
                throw new IllegalStateException(
                    String.format("No se puede generar PDF: El folio %d está asociado a un almacén inexistente (ID: %d). " +
                        "Esto indica datos huérfanos en la base de datos. Por favor, contacte al administrador del sistema.",
                        label.getFolio(), label.getWarehouseId()));
            }

            // Mapear datos a las variables del JRXML
            record.put("NomMarbete", String.valueOf(label.getFolio()));
            record.put("Codigo", product.getCveArt());

            // Truncar descripción si es muy larga
            String descripcion = product.getDescr();
            if (descripcion != null && descripcion.length() > 40) {
                descripcion = descripcion.substring(0, 37) + "...";
            }
            record.put("Descripcion", descripcion != null ? descripcion : "");

            record.put("CLAVE", product.getCveArt()); // Para el campo de código arriba
            record.put("DESCR", descripcion != null ? descripcion : ""); // Para descripción arriba

            // Datos del almacén
            record.put("Clave almacen", warehouse.getWarehouseKey());
            record.put("Nombre almacen", warehouse.getNameWarehouse());
            record.put("Almacen", warehouse.getWarehouseKey() + " " + warehouse.getNameWarehouse());

            // Fecha actual
            record.put("Fecha", fechaActual);

            dataSource.add(record);
        }

        return dataSource;
    }
}

