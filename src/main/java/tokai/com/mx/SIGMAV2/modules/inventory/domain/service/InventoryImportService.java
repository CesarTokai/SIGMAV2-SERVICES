package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportRequestDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportResultDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryImportUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.*;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryImportService implements InventoryImportUseCase {


    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final PeriodRepository periodRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final InventoryStockRepository stockRepository;
    private final InventoryImportJobRepository importJobRepository;


    public InventoryImportService(
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            PeriodRepository periodRepository,
            InventorySnapshotRepository snapshotRepository,
            InventoryStockRepository stockRepository,
            InventoryImportJobRepository importJobRepository
    ) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.periodRepository = periodRepository;
        this.snapshotRepository = snapshotRepository;
        this.stockRepository = stockRepository;
        this.importJobRepository = importJobRepository;
    }

    @Override
    @Transactional
    public InventoryImportResultDTO importInventory(InventoryImportRequestDTO request) {
        final List<String> errors = new ArrayList<>();
        String logFileUrl = null;
        final ImportStats stats = new ImportStats();

        try {
            // Validaciones iniciales
            MultipartFile file = request.getFile();
            if (file == null || file.isEmpty()) {
                errors.add("El archivo está vacío");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

            // Validar extensión del archivo
            String fileName = file.getOriginalFilename();
            if (fileName != null && !fileName.toLowerCase().endsWith(".xlsx")) {
                errors.add("El archivo debe ser formato XLSX");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

            Long periodId = request.getIdPeriod();
            if (periodId == null) {
                errors.add("El periodo es obligatorio");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

            // 1. Validar periodo
            Period period = periodRepository.findById(periodId)
                    .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));
            if (period.getState() == Period.State.CLOSED || period.getState() == Period.State.LOCKED) {
                errors.add("No se puede importar en un periodo cerrado o bloqueado");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

            // 2. Validar almacén
            Long warehouseId = request.getIdWarehouse();
            Warehouse warehouse = warehouseId != null
                    ? warehouseRepository.findById(warehouseId)
                            .orElseThrow(() -> new IllegalArgumentException("Almacén no existe"))
                    : getDefaultWarehouse();

            // 3. Parsear archivo
            List<InventoryImportRow> rows = parseExcel(file);
            if (rows.isEmpty()) {
                errors.add("El archivo no contiene datos para importar");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }
            stats.totalRows = rows.size();

            // 4. Procesar productos y snapshots
            Set<Long> importedProductIds = new HashSet<>();
            for (InventoryImportRow row : rows) {
                // Validar datos requeridos
                if (row.getCveArt() == null || row.getCveArt().trim().isEmpty()) {
                    errors.add("Fila " + (importedProductIds.size() + 1) + ": CVE_ART es requerido");
                    continue;
                }

                try {
                    Product product = processProduct(row, stats);
                    importedProductIds.add(product.getId());
                    processSnapshot(product, warehouse, period, row.getExistQty(), stats);
                } catch (Exception e) {
                    errors.add("Error procesando " + row.getCveArt() + ": " + e.getMessage());
                }
            }

            // 5. Marcar como baja los productos no incluidos
            if (!importedProductIds.isEmpty()) {
                stats.deactivated = snapshotRepository.markAsInactiveNotInImport(
                    period.getId(),
                    warehouse.getId(),
                    new ArrayList<>(importedProductIds)
                );
            }

            // 6. Registrar bitácora
            InventoryImportJob job = new InventoryImportJob();
            job.setFileName(file.getOriginalFilename());
            job.setUser("SYSTEM"); // TODO: Obtener del contexto de seguridad
            job.setStartedAt(LocalDateTime.now());
            job.setFinishedAt(LocalDateTime.now());
            job.setTotalRecords(stats.totalRows);
            job.setStatus(errors.isEmpty() ? "SUCCESS" : "WARNING");
            importJobRepository.save(job);

            logFileUrl = generateLogFileUrl(job.getId());

        } catch (Exception e) {
            errors.add("Error al importar inventario: " + e.getMessage());
        }

        return new InventoryImportResultDTO(
                stats.totalRows,
                stats.inserted,
                stats.updated,
                stats.deactivated,
                errors,
                logFileUrl
        );
    }

    private Product processProduct(InventoryImportRow row, ImportStats stats) {
        return productRepository.findByCveArt(row.getCveArt())
                .map(existingProduct -> {
                    // Actualizar datos si cambiaron
                    boolean changed = false;
                    if (!existingProduct.getDescr().equals(row.getDescr())) {
                        existingProduct.setDescr(row.getDescr());
                        changed = true;
                    }
                    if (!existingProduct.getUniMed().equals(row.getUniMed())) {
                        existingProduct.setUniMed(row.getUniMed());
                        changed = true;
                    }
                    if (changed) {
                        return productRepository.save(existingProduct);
                    }
                    return existingProduct;
                })
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setCveArt(row.getCveArt());
                    p.setDescr(row.getDescr());
                    p.setUniMed(row.getUniMed());
                    p.setStatus(Product.Status.A);
                    p.setCreatedAt(LocalDateTime.now());
                    stats.incrementInserted();
                    return productRepository.save(p);
                });
    }

    private void processSnapshot(Product product, Warehouse warehouse, Period period,
                               BigDecimal existQty, ImportStats stats) {
        InventorySnapshot snapshot = snapshotRepository
                .findByProductWarehousePeriod(product.getId(), warehouse.getId(), period.getId())
                .orElseGet(() -> {
                    InventorySnapshot s = new InventorySnapshot();
                    s.setProduct(product);
                    s.setWarehouse(warehouse);
                    s.setPeriod(period);
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        if (snapshot.getId() != null && !snapshot.getExistQty().equals(existQty)) {
            stats.incrementUpdated();
        }

        snapshot.setExistQty(existQty);
        snapshotRepository.save(snapshot);
    }

    private String generateLogFileUrl(Long jobId) {
        // Implementar lógica para generar URL del archivo de log
        return "/api/inventory/import/logs/" + jobId;
    }

    private Warehouse getDefaultWarehouse() {
        // Implementar lógica para obtener almacén por defecto
        return warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay almacén por defecto configurado"));
    }

    private List<InventoryImportRow> parseExcel(MultipartFile file) {
        // Implementar lógica de parseo de Excel/CSV a DTOs
        // Este método debe validar y mapear cada fila a InventoryImportRow
        throw new UnsupportedOperationException("No implementado");
    }



    // DTO auxiliar para parseo de archivo
    private static class InventoryImportRow {
        private String cveArt;
        private String descr;
        private String uniMed;
        private BigDecimal existQty;

        // Getters y setters
        public String getCveArt() { return cveArt; }
        public void setCveArt(String cveArt) { this.cveArt = cveArt; }
        public String getDescr() { return descr; }
        public void setDescr(String descr) { this.descr = descr; }
        public String getUniMed() { return uniMed; }
        public void setUniMed(String uniMed) { this.uniMed = uniMed; }
        public BigDecimal getExistQty() { return existQty; }
        public void setExistQty(BigDecimal existQty) { this.existQty = existQty; }
    }

    // Clase auxiliar para manejar contadores
    private static class ImportStats {
        int totalRows = 0;
        int inserted = 0;
        int updated = 0;
        int deactivated = 0;

        void incrementInserted() {
            inserted++;
        }

        void incrementUpdated() {
            updated++;
        }
    }
}
