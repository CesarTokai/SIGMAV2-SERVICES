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
    public InventoryImportJob importInventory(InventoryImportRequestDTO request) {

        MultipartFile file = request.getFile();
        Long periodId = request.getPeriodId();
        Long warehouseId = request.getWarehouseId();
        String username = request.getUsername();

        // 1. Validar periodo
        Period period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));
        if (period.getState() == Period.State.CLOSED || period.getState() == Period.State.LOCKED) {
            throw new IllegalStateException("No se puede importar en un periodo cerrado o bloqueado");
        }

        // 2. Validar almacén (si no se envía, usar default)
        Warehouse warehouse = warehouseId != null
                ? warehouseRepository.findById(warehouseId).orElseThrow(() -> new IllegalArgumentException("Almacén no existe"))
                : getDefaultWarehouse();

        // 3. Parsear archivo (aquí se asume un método parseExcel que retorna lista de DTOs)
        List<InventoryImportRow> rows = parseExcel(file);

        // 4. Procesar productos y snapshots
        Set<Long> importedProductIds = new HashSet<>();
        int totalRecords = 0;
        for (InventoryImportRow row : rows) {
            // Alta/actualización de producto
            Product product = productRepository.findByCveArt(row.getCveArt())
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setCveArt(row.getCveArt());
                        p.setDescr(row.getDescr());
                        p.setUniMed(row.getUniMed());
                        p.setStatus(Product.Status.A);
                        p.setCreatedAt(LocalDateTime.now());
                        return productRepository.save(p);
                    });
            importedProductIds.add(product.getId());

            // Alta/actualización de snapshot
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
            snapshot.setExistQty(row.getExistQty());
            snapshotRepository.save(snapshot);

            // (Opcional) Actualizar inventario stock
            // ... (si aplica según reglas)

            totalRecords++;
        }

        // 5. Marcar como baja los productos no incluidos en el archivo
        snapshotRepository.markAsInactiveNotInImport(period.getId(), warehouse.getId(), new ArrayList<>(importedProductIds));

        // 6. Registrar bitácora de importación
        InventoryImportJob job = new InventoryImportJob();
        job.setFileName(file.getOriginalFilename());
        job.setUser(username);
        job.setStartedAt(LocalDateTime.now());
        job.setFinishedAt(LocalDateTime.now());
        job.setTotalRecords(totalRecords);
        job.setStatus("SUCCESS");
        importJobRepository.save(job);

        return job;
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
}
