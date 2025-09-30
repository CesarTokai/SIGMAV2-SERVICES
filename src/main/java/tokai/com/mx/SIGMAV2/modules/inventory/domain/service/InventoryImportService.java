package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportRequestDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportResultDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryImportUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence.JpaPersonalInformationRepository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;

@Service
public class InventoryImportService implements InventoryImportUseCase {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final PeriodRepository periodRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final InventoryStockRepository stockRepository;
    private final InventoryImportJobRepository importJobRepository;
    private final JpaPersonalInformationRepository personalInformationRepository;

    @Autowired
    public InventoryImportService(
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            @Qualifier("inventoryPeriodRepositoryAdapter") PeriodRepository periodRepository,
            InventorySnapshotRepository snapshotRepository,
            InventoryStockRepository stockRepository,
            @Qualifier("inventoryImportJobRepositoryAdapter") InventoryImportJobRepository importJobRepository,
            JpaPersonalInformationRepository personalInformationRepository
    ) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.periodRepository = periodRepository;
        this.snapshotRepository = snapshotRepository;
        this.stockRepository = stockRepository;
        this.importJobRepository = importJobRepository;
        this.personalInformationRepository = personalInformationRepository;
    }

    // Método para mapear el modelo Period de periods a inventory
    private Period mapPeriod(tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period source) {
        Period target = new Period();
        target.setId(source.getId());
        target.setPeriodDate(source.getDate());
        target.setComments(source.getComments());
        target.setState(Period.State.valueOf(source.getState().name()));
        return target;
    }

    @Override
    @Transactional
    public InventoryImportResultDTO importInventory(InventoryImportRequestDTO request, String username) {
        final List<String> errors = new ArrayList<>();
        String logFileUrl = null;
        final ImportStats stats = new ImportStats();

        try {
            MultipartFile file = request.getFile();
            if (file == null || file.isEmpty()) {
                errors.add("El archivo está vacío");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

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
            tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period periodEntity = periodRepository.findById(periodId)
                    .orElseThrow(() -> new IllegalArgumentException("Periodo no existe"));
            Period period = mapPeriod(periodEntity);

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
            String nombreCompleto = "Desconocido";
            if (username != null) {
                BeanPersonalInformation personalInfo = personalInformationRepository.findAll()
                    .stream()
                    .filter(pi -> pi.getUser().getEmail().equals(username)) // Ajusta si tu campo no es email
                    .findFirst()
                    .orElse(null);
                if (personalInfo != null) {
                    nombreCompleto = personalInfo.getName();
                    if (personalInfo.getFirstLastName() != null && !personalInfo.getFirstLastName().isEmpty()) {
                        nombreCompleto += " " + personalInfo.getFirstLastName();
                    }
                }
            }
            InventoryImportJob job = new InventoryImportJob();
            job.setFileName(file.getOriginalFilename());
            job.setUser(nombreCompleto);
            job.setStartedAt(LocalDateTime.now());
            job.setFinishedAt(LocalDateTime.now());
            job.setTotalRecords(stats.totalRows);
            job.setStatus(errors.isEmpty() ? "SUCCESS" : "WARNING");
            job.setInsertedRows(stats.inserted);
            job.setUpdatedRows(stats.updated);
            job.setSkippedRows(0); // Si tienes lógica para filas omitidas, asígnala aquí
            job.setTotalRows(stats.totalRows);
            job.setIdPeriod(period.getId());
            job.setIdWarehouse(warehouse.getId());
            job.setCreatedBy(nombreCompleto);
            logFileUrl = generateLogFileUrl(job.getId());
            job.setLogFilePath(logFileUrl);
            // Guardar errores como JSON (aunque esté vacío)
            try {
                job.setErrorsJson(new ObjectMapper().writeValueAsString(errors));
            } catch (Exception ex) {
                job.setErrorsJson("[\"Error serializando errores\"]");
            }
            // Calcular y guardar checksum SHA-256 del archivo
            try (InputStream is = file.getInputStream()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                DigestInputStream dis = new DigestInputStream(is, digest);
                byte[] buffer = new byte[4096];
                while (dis.read(buffer) != -1) {}
                byte[] hash = digest.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                job.setChecksum(hexString.toString());
            } catch (Exception ex) {
                job.setChecksum(null);
            }
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
                    boolean changed = false;
                    if (!existingProduct.getDescr().equals(row.getDescr())) {
                        existingProduct.setDescr(row.getDescr());
                        changed = true;
                    }
                    if (!existingProduct.getUniMed().equals(row.getUniMed())) {
                        existingProduct.setUniMed(row.getUniMed());
                        changed = true;
                    }
                    if (row.getLinProd() != null && (existingProduct.getLinProd() == null || !existingProduct.getLinProd().equals(row.getLinProd()))) {
                        existingProduct.setLinProd(row.getLinProd());
                        changed = true;
                    }
                    if (row.getStatus() != null) {
                        try {
                            Product.Status newStatus = Product.Status.valueOf(row.getStatus().toUpperCase());
                            if (existingProduct.getStatus() != newStatus) {
                                existingProduct.setStatus(newStatus);
                                changed = true;
                            }
                        } catch (Exception ignored) {}
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
                    p.setLinProd(row.getLinProd());
                    try {
                        p.setStatus(Product.Status.valueOf(row.getStatus() != null ? row.getStatus().toUpperCase() : "A"));
                    } catch (Exception e) {
                        p.setStatus(Product.Status.A);
                    }
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
        return "/api/inventory/import/logs/" + jobId;
    }

    private Warehouse getDefaultWarehouse() {
        return warehouseRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay almacén por defecto configurado"));
    }

    private List<InventoryImportRow> parseExcel(MultipartFile file) {
        List<InventoryImportRow> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            boolean isHeader = true;
            int cveArtIdx = -1, descrIdx = -1, linProdIdx = -1, uniMedIdx = -1, existQtyIdx = -1, statusIdx = -1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isHeader) {
                    // Buscar índices de columnas por nombre
                    for (Cell cell : row) {
                        String val = cell.getStringCellValue().trim().toUpperCase();
                        if (val.equals("CVE_ART")) cveArtIdx = cell.getColumnIndex();
                        if (val.equals("DESCR")) descrIdx = cell.getColumnIndex();
                        if (val.equals("LIN_PROD")) linProdIdx = cell.getColumnIndex();
                        if (val.equals("UNI_MED")) uniMedIdx = cell.getColumnIndex();
                        if (val.equals("EXIST_QTY") || val.equals("EXIST")) existQtyIdx = cell.getColumnIndex();
                        if (val.equals("STATUS")) statusIdx = cell.getColumnIndex();
                    }
                    isHeader = false;
                    // Validar que todas las columnas existan
                    if (cveArtIdx == -1 || descrIdx == -1 || uniMedIdx == -1 || existQtyIdx == -1) {
                        throw new IllegalArgumentException("El archivo no tiene las columnas requeridas: CVE_ART, DESCR, UNI_MED, EXIST/EXIST_QTY");
                    }
                    continue;
                }
                InventoryImportRow importRow = new InventoryImportRow();
                Cell cveArtCell = row.getCell(cveArtIdx);
                Cell descrCell = row.getCell(descrIdx);
                Cell linProdCell = linProdIdx != -1 ? row.getCell(linProdIdx) : null;
                Cell uniMedCell = row.getCell(uniMedIdx);
                Cell existQtyCell = row.getCell(existQtyIdx);
                Cell statusCell = statusIdx != -1 ? row.getCell(statusIdx) : null;
                importRow.setCveArt(cveArtCell != null ? cveArtCell.toString().trim() : null);
                importRow.setDescr(descrCell != null ? descrCell.toString().trim() : null);
                importRow.setLinProd(linProdCell != null ? linProdCell.toString().trim() : null);
                importRow.setUniMed(uniMedCell != null ? uniMedCell.toString().trim() : null);
                if (existQtyCell != null) {
                    try {
                        String val = existQtyCell.toString().trim();
                        importRow.setExistQty(val.isEmpty() ? BigDecimal.ZERO : new BigDecimal(val));
                    } catch (Exception e) {
                        importRow.setExistQty(BigDecimal.ZERO);
                    }
                } else {
                    importRow.setExistQty(BigDecimal.ZERO);
                }
                importRow.setStatus(statusCell != null ? statusCell.toString().trim() : null);
                // Solo agregar si tiene clave de producto
                if (importRow.getCveArt() != null && !importRow.getCveArt().isEmpty()) {
                    rows.add(importRow);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al leer el archivo XLSX: " + e.getMessage());
        }
        return rows;
    }

    // DTO auxiliar para parseo de archivo
    private static class InventoryImportRow {
        private String cveArt;
        private String descr;
        private String linProd;
        private String uniMed;
        private BigDecimal existQty;
        private String status;

        public String getCveArt() { return cveArt; }
        public void setCveArt(String cveArt) { this.cveArt = cveArt; }
        public String getDescr() { return descr; }
        public void setDescr(String descr) { this.descr = descr; }
        public String getLinProd() { return linProd; }
        public void setLinProd(String linProd) { this.linProd = linProd; }
        public String getUniMed() { return uniMed; }
        public void setUniMed(String uniMed) { this.uniMed = uniMed; }
        public BigDecimal getExistQty() { return existQty; }
        public void setExistQty(BigDecimal existQty) { this.existQty = existQty; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
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
