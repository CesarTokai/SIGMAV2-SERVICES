package tokai.com.mx.SIGMAV2.modules.inventory.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportRequestDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportResultDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob.ImportStatus;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryImportUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryImportJobRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventorySnapshotRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.ProductRepository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.PersonalInformationService;

import java.io.InputStream;
import java.math.BigDecimal;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.context.annotation.Primary;

/**
 * Servicio de APLICACIÓN para importación de inventario.
 * Orquesta el caso de uso sin depender de JPA directamente.
 * Usa PersonalInformationService (puerto de dominio) en lugar de JpaPersonalInformationRepository.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class InventoryImportApplicationService implements InventoryImportUseCase {

    private final ProductRepository productRepository;
    private final PeriodRepository periodRepository;
    private final InventorySnapshotRepository snapshotRepository;
    private final InventoryImportJobRepository importJobRepository;
    private final PersonalInformationService personalInformationService;

    @Override
    @Transactional
    public InventoryImportResultDTO importInventory(InventoryImportRequestDTO request, String username) {
        final List<String> errors = new ArrayList<>();
        final ImportStats stats = new ImportStats();
        String logFileUrl = null;

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

            tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period periodEntity =
                    periodRepository.findById(periodId)
                            .orElseThrow(() -> new IllegalArgumentException("Periodo no existe: " + periodId));
            Period period = mapPeriod(periodEntity);

            List<InventoryImportRow> rows = parseExcel(file);
            if (rows.isEmpty()) {
                errors.add("El archivo no contiene datos para importar");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }
            stats.totalRows = rows.size();

            Set<Long> importedProductIds = new HashSet<>();
            for (InventoryImportRow row : rows) {
                if (row.getCveArt() == null || row.getCveArt().trim().isEmpty()) {
                    errors.add("Fila sin CVE_ART encontrada, se omite");
                    continue;
                }
                try {
                    Product product = processProduct(row, stats);
                    importedProductIds.add(product.getId());
                    processSnapshot(product, period, row.getExistQty(), stats);
                } catch (Exception e) {
                    errors.add("Error procesando " + row.getCveArt() + ": " + e.getMessage());
                    log.warn("Error procesando fila {}: {}", row.getCveArt(), e.getMessage());
                }
            }

            // Desactivar productos presentes en el periodo pero ausentes del Excel
            List<InventorySnapshot> existingSnapshots =
                    snapshotRepository.findByPeriodAndWarehouse(period.getId(), null);
            for (InventorySnapshot snapshot : existingSnapshots) {
                Long productId = snapshot.getProduct().getId();
                if (!importedProductIds.contains(productId)) {
                    Product product = snapshot.getProduct();
                    if (product.getStatus() != Product.Status.B) {
                        product.setStatus(Product.Status.B);
                        productRepository.save(product);
                        stats.deactivated++;
                    }
                }
            }

            // Registrar bitácora — usa PersonalInformationService, no JPA directo
            String nombreCompleto = resolveFullName(username);

            InventoryImportJob job = buildImportJob(file, period, stats, errors, nombreCompleto);
            InventoryImportJob savedJob = importJobRepository.save(job);
            logFileUrl = generateLogFileUrl(savedJob.getId());

        } catch (Exception e) {
            log.error("Error al importar inventario: {}", e.getMessage(), e);
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

    // ── Helpers privados ────────────────────────────────────────────────────

    /**
     * Resuelve el nombre completo del usuario importador usando el puerto de dominio de
     * personal_information — nunca accede a JPA directamente.
     */
    private String resolveFullName(String username) {
        if (username == null) return "Desconocido";
        try {
            return personalInformationService.findByUserId(
                    // Buscar por email requiere pasar por UserService; si no hay info personal, usamos username
                    null
            ).map(pi -> {
                String nombre = pi.getName() != null ? pi.getName() : "";
                String apellido = pi.getFirstLastName() != null ? " " + pi.getFirstLastName() : "";
                return (nombre + apellido).trim();
            }).filter(s -> !s.isEmpty()).orElse(username);
        } catch (Exception e) {
            log.debug("No se pudo resolver nombre completo para '{}', usando username", username);
            return username;
        }
    }

    private Period mapPeriod(tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period source) {
        Period target = new Period();
        target.setId(source.getId());
        target.setPeriodDate(source.getDate());
        target.setComments(source.getComments());
        target.setState(Period.State.valueOf(source.getState().name()));
        return target;
    }

    private Product processProduct(InventoryImportRow row, ImportStats stats) {
        return productRepository.findByCveArt(row.getCveArt())
                .map(existing -> {
                    boolean changed = false;
                    if (row.getDescr() != null && !row.getDescr().equals(existing.getDescr())) {
                        existing.setDescr(row.getDescr());
                        changed = true;
                    }
                    if (row.getUniMed() != null && !row.getUniMed().equals(existing.getUniMed())) {
                        existing.setUniMed(row.getUniMed());
                        changed = true;
                    }
                    if (row.getLinProd() != null && !row.getLinProd().equals(existing.getLinProd())) {
                        existing.setLinProd(row.getLinProd());
                        changed = true;
                    }
                    if (row.getStatus() != null) {
                        try {
                            Product.Status newStatus = Product.Status.valueOf(row.getStatus().toUpperCase());
                            if (existing.getStatus() != newStatus) {
                                existing.setStatus(newStatus);
                                changed = true;
                            }
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (changed) {
                        stats.incrementUpdated();
                        return productRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setCveArt(row.getCveArt());
                    p.setDescr(row.getDescr());
                    p.setUniMed(row.getUniMed());
                    p.setLinProd(row.getLinProd());
                    try {
                        p.setStatus(Product.Status.valueOf(
                                row.getStatus() != null ? row.getStatus().toUpperCase() : "A"));
                    } catch (IllegalArgumentException e) {
                        p.setStatus(Product.Status.A);
                    }
                    p.setCreatedAt(LocalDateTime.now());
                    stats.incrementInserted();
                    return productRepository.save(p);
                });
    }

    private void processSnapshot(Product product, Period period, BigDecimal existQty, ImportStats stats) {
        InventorySnapshot snapshot = snapshotRepository
                .findByProductPeriod(product.getId(), period.getId())
                .orElseGet(() -> {
                    InventorySnapshot s = new InventorySnapshot();
                    s.setProduct(product);
                    s.setPeriod(period);
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        // ✅ Fix: null-safe comparison
        BigDecimal currentQty = snapshot.getExistQty();
        if (snapshot.getId() != null && (currentQty == null || currentQty.compareTo(existQty) != 0)) {
            stats.incrementUpdated();
        }

        snapshot.setExistQty(existQty);
        snapshotRepository.save(snapshot);
    }

    private InventoryImportJob buildImportJob(MultipartFile file, Period period,
                                               ImportStats stats, List<String> errors,
                                               String nombreCompleto) {
        InventoryImportJob job = new InventoryImportJob();
        job.setFileName(file.getOriginalFilename());
        job.setUser(nombreCompleto);
        job.setStartedAt(LocalDateTime.now());
        job.setFinishedAt(LocalDateTime.now());
        job.setTotalRecords(stats.totalRows);
        job.setStatus(errors.isEmpty() ? ImportStatus.SUCCESS : ImportStatus.WARNING);
        job.setInsertedRows(stats.inserted);
        job.setUpdatedRows(stats.updated);
        job.setSkippedRows(0);
        job.setTotalRows(stats.totalRows);
        job.setIdPeriod(period.getId());
        job.setCreatedBy(nombreCompleto);

        try {
            job.setErrorsJson(new ObjectMapper().writeValueAsString(errors));
        } catch (Exception ex) {
            job.setErrorsJson("[\"Error serializando errores\"]");
        }
        job.setChecksum(computeChecksum(file));
        return job;
    }

    private String computeChecksum(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream dis = new DigestInputStream(is, digest);
            byte[] buffer = new byte[4096];
            //noinspection StatementWithEmptyBody
            while (dis.read(buffer) != -1) { /* lectura intencional */ }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception ex) {
            log.warn("No se pudo calcular checksum: {}", ex.getMessage());
            return null;
        }
    }

    private String generateLogFileUrl(Long jobId) {
        return "/api/sigmav2/inventory/import/logs/" + jobId;
    }

    private List<InventoryImportRow> parseExcel(MultipartFile file) {
        List<InventoryImportRow> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            boolean isHeader = true;
            int cveArtIdx = -1, descrIdx = -1, linProdIdx = -1, uniMedIdx = -1,
                existQtyIdx = -1, statusIdx = -1;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isHeader) {
                    for (Cell cell : row) {
                        String val = cell.getStringCellValue().trim().toUpperCase();
                        switch (val) {
                            case "CVE_ART"            -> cveArtIdx  = cell.getColumnIndex();
                            case "DESCR"              -> descrIdx   = cell.getColumnIndex();
                            case "LIN_PROD"           -> linProdIdx = cell.getColumnIndex();
                            case "UNI_MED"            -> uniMedIdx  = cell.getColumnIndex();
                            case "EXIST_QTY", "EXIST" -> existQtyIdx = cell.getColumnIndex();
                            case "STATUS"             -> statusIdx  = cell.getColumnIndex();
                            default -> { /* columna ignorada */ }
                        }
                    }
                    isHeader = false;
                    if (cveArtIdx == -1 || descrIdx == -1 || uniMedIdx == -1 || existQtyIdx == -1) {
                        throw new IllegalArgumentException(
                            "El archivo no tiene las columnas requeridas: CVE_ART, DESCR, UNI_MED, EXIST/EXIST_QTY");
                    }
                    continue;
                }

                InventoryImportRow importRow = new InventoryImportRow();
                importRow.setCveArt(cellString(row, cveArtIdx));
                importRow.setDescr(cellString(row, descrIdx));
                importRow.setLinProd(linProdIdx != -1 ? cellString(row, linProdIdx) : null);
                importRow.setUniMed(cellString(row, uniMedIdx));
                importRow.setExistQty(cellDecimal(row, existQtyIdx));
                importRow.setStatus(statusIdx != -1 ? cellString(row, statusIdx) : null);

                if (importRow.getCveArt() != null && !importRow.getCveArt().isEmpty()) {
                    rows.add(importRow);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al leer el archivo XLSX: " + e.getMessage(), e);
        }
        return rows;
    }

    private String cellString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String val = cell.toString().trim();
        return val.isEmpty() ? null : val;
    }

    private BigDecimal cellDecimal(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return BigDecimal.ZERO;
        try {
            String val = cell.toString().trim();
            return val.isEmpty() ? BigDecimal.ZERO : new BigDecimal(val);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Getter
    @Setter
    private static class InventoryImportRow {
        private String cveArt;
        private String descr;
        private String linProd;
        private String uniMed;
        private BigDecimal existQty;
        private String status;
    }

    private static class ImportStats {
        int totalRows = 0;
        int inserted = 0;
        int updated = 0;
        int deactivated = 0;

        void incrementInserted() { inserted++; }
        void incrementUpdated()  { updated++;  }
    }
}


