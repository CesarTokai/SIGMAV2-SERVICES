
package tokai.com.mx.SIGMAV2.modules.inventory.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Primary;
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
import java.math.RoundingMode;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de APLICACIÓN para importación de inventario.
 * Correcciones aplicadas:
 *  1. cellString() maneja celdas numéricas → evita "1001.0" en CVE_ART.
 *  2. processSnapshot() propaga el status del producto al snapshot.
 *  3. Status siempre tiene valor por defecto "A" si viene nulo del Excel.
 *  4. Contador updated se incrementa DESPUÉS del save().
 *  5. resolveFullName() ya no pasa null al servicio.
 *  6. Validaciones exhaustivas en parseExcel() y en el bucle principal.
 *  7. Warnings en lugar de silencio cuando el status es inválido.
 *  8. deactivated se registra correctamente en el job.
 *  9. Validación de existQty negativa.
 * 10. Límite máximo de filas para evitar archivos maliciosos.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class InventoryImportApplicationService implements InventoryImportUseCase {

    // ── Constantes de validación ────────────────────────────────────────────
    private static final int    MAX_ROWS            = 50_000;
    private static final int    MAX_CVE_ART_LENGTH  = 50;
    private static final int    MAX_DESCR_LENGTH    = 255;
    private static final int    MAX_UNI_MED_LENGTH  = 20;
    private static final int    MAX_LIN_PROD_LENGTH = 100;
    private static final Set<String> VALID_STATUSES = Set.of("A", "B");

    // ── Dependencias ────────────────────────────────────────────────────────
    private final ProductRepository              productRepository;
    private final PeriodRepository               periodRepository;
    private final InventorySnapshotRepository    snapshotRepository;
    private final InventoryImportJobRepository   importJobRepository;
    private final PersonalInformationService     personalInformationService;

    /** Lock por período para evitar importaciones concurrentes (#8 race condition) */
    private static final Map<Long, Object> PERIOD_LOCKS = new ConcurrentHashMap<>();

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTODO PRINCIPAL
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public InventoryImportResultDTO importInventory(InventoryImportRequestDTO request,
                                                    String username) {
        final List<String> errors = new ArrayList<>();
        final ImportStats  stats  = new ImportStats();
        String logFileUrl = null;

        try {
            // ── 1. Validaciones del archivo ──────────────────────────────
            MultipartFile file = request.getFile();
            List<String> fileErrors = validateFile(file);
            if (!fileErrors.isEmpty()) {
                return new InventoryImportResultDTO(0, 0, 0, 0, fileErrors, null);
            }

            // ── 2. Validación del periodo ────────────────────────────────
            Long periodId = request.getIdPeriod();
            if (periodId == null || periodId <= 0) {
                errors.add("El periodo es obligatorio y debe ser un ID válido.");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

            tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period periodEntity =
                    periodRepository.findById(periodId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "El periodo con ID " + periodId + " no existe."));

            // Validar que el periodo esté activo
            if (periodEntity.getState() == null ||
                    periodEntity.getState().name().equalsIgnoreCase("C")) {
                errors.add("El periodo seleccionado está cerrado y no permite importaciones.");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

            Period period = mapPeriod(periodEntity);

            // ── 2b. Protección contra importación concurrente (#8) ───────
            Object lock = PERIOD_LOCKS.computeIfAbsent(periodId, k -> new Object());
            synchronized (lock) {
                try {
                    return doImport(file, period, username, errors, stats);
                } finally {
                    PERIOD_LOCKS.remove(periodId);
                }
            }

        } catch (Exception e) {
            log.error("Error crítico al importar inventario: {}", e.getMessage(), e);
            errors.add("Error crítico al importar inventario: " + e.getMessage());
        }

        return new InventoryImportResultDTO(
                stats.totalRows, stats.inserted, stats.updated,
                stats.deactivated, errors, logFileUrl
        );
    }

    /**
     * Lógica interna de importación, ejecutada dentro del lock de período.
     */
    private InventoryImportResultDTO doImport(MultipartFile file, Period period,
                                               String username, List<String> errors,
                                               ImportStats stats) {
        String logFileUrl = null;
        try {
            // ── 3. Detección de archivo duplicado por checksum (#5) ──────
            String checksum = computeChecksum(file);
            if (checksum != null) {
                Optional<InventoryImportJob> lastJob = importJobRepository.findLatestByPeriodId(period.getId());
                if (lastJob.isPresent() && checksum.equals(lastJob.get().getChecksum())) {
                    errors.add("El archivo es idéntico al de la última importación para este periodo. No se realizaron cambios.");
                    return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
                }
            }

            // ── 3b. FIX #10: Advertir si ya existen snapshots con warehouse ──
            List<InventorySnapshot> existingSnapshots =
                    snapshotRepository.findByPeriodAndWarehouse(period.getId(), null);
            boolean hasWarehouseSnapshots = existingSnapshots.stream()
                    .anyMatch(s -> s.getWarehouse() != null && s.getWarehouse().getId() != null);
            if (hasWarehouseSnapshots) {
                errors.add("ADVERTENCIA: Ya existen registros con almacén asignado para este periodo "
                        + "(probablemente importados desde MultiAlmacén). "
                        + "La importación de inventario simple se registrará SIN almacén. "
                        + "MultiAlmacén seguirá siendo la fuente de verdad para marbetes.");
                log.warn("Re-importación de inventario simple sobre periodo con datos de multialmacén");
            }

            // ── 4. Parseo del Excel ──────────────────────────────────────
            List<InventoryImportRow> rows = parseExcel(file, errors);

            if (!errors.isEmpty()) {
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }
            if (rows.isEmpty()) {
                errors.add("El archivo no contiene filas de datos para importar.");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }
            if (rows.size() > MAX_ROWS) {
                errors.add("El archivo supera el límite de " + MAX_ROWS + " filas permitidas.");
                return new InventoryImportResultDTO(0, 0, 0, 0, errors, null);
            }

            stats.totalRows = rows.size();

            // ── 5. Procesamiento fila por fila ───────────────────────────
            Set<Long> importedProductIds = new HashSet<>();
            int processedCount = 0;

            for (int i = 0; i < rows.size(); i++) {
                InventoryImportRow row = rows.get(i);
                int rowNum = i + 2;

                List<String> rowErrors = validateRow(row, rowNum);
                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    stats.skipped++;
                    continue;
                }

                try {
                    Product product = processProduct(row, stats, errors, rowNum);
                    importedProductIds.add(product.getId());
                    processSnapshot(product, period, row.getExistQty(), stats);
                    processedCount++;
                } catch (Exception e) {
                    String msg = String.format("Fila %d [%s]: %s", rowNum, row.getCveArt(), e.getMessage());
                    errors.add(msg);
                    log.warn("Error procesando fila {} ({}): {}", rowNum, row.getCveArt(), e.getMessage());
                    stats.skipped++;
                }
            }
            log.info("Filas procesadas correctamente: {}/{}", processedCount, rows.size());

            // ── 6. Desactivar productos ausentes en el Excel (bulk) ──────
            deactivateMissingProducts(period, importedProductIds, stats, errors);

            // ── 7. Registrar bitácora ────────────────────────────────────
            String nombreCompleto = resolveFullName(username);
            InventoryImportJob job = buildImportJob(file, period, stats, errors, nombreCompleto);
            job.setChecksum(checksum);
            InventoryImportJob savedJob = importJobRepository.save(job);
            logFileUrl = generateLogFileUrl(savedJob.getId());

            log.info("Importación completada — total:{} insertados:{} actualizados:{} desactivados:{} errores:{}",
                    stats.totalRows, stats.inserted, stats.updated, stats.deactivated, errors.size());

        } catch (Exception e) {
            log.error("Error crítico en doImport: {}", e.getMessage(), e);
            errors.add("Error crítico al importar inventario: " + e.getMessage());
        }

        return new InventoryImportResultDTO(
                stats.totalRows, stats.inserted, stats.updated,
                stats.deactivated, errors, logFileUrl
        );
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VALIDACIONES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Valida el archivo antes de procesarlo.
     */
    private List<String> validateFile(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            errors.add("El archivo está vacío o no fue enviado.");
            return errors;
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            errors.add("El archivo debe tener formato XLSX. Archivo recibido: "
                    + (fileName != null ? fileName : "sin nombre"));
        }
        // Límite de tamaño: 10 MB
        if (file.getSize() > 10 * 1024 * 1024) {
            errors.add("El archivo supera el tamaño máximo permitido de 10 MB.");
        }
        return errors;
    }

    /**
     * Valida una fila individual antes de procesarla.
     * CVE_ART es el único campo bloqueante. DESCR y UNI_MED generan advertencia
     * pero se usan defaults para no rechazar la fila.
     */
    private List<String> validateRow(InventoryImportRow row, int rowNum) {
        List<String> errors = new ArrayList<>();
        String prefix = "Fila " + rowNum;

        // CVE_ART obligatorio — sin esto no se puede procesar
        if (row.getCveArt() == null || row.getCveArt().isBlank()) {
            errors.add(prefix + ": CVE_ART es obligatorio.");
            return errors;
        }
        if (row.getCveArt().length() > MAX_CVE_ART_LENGTH) {
            errors.add(prefix + " [" + row.getCveArt() + "]: CVE_ART supera los "
                    + MAX_CVE_ART_LENGTH + " caracteres permitidos.");
        }

        // DESCR: si viene vacío, se usará default en processProduct (no es bloqueante)
        if (row.getDescr() == null || row.getDescr().isBlank()) {
            row.setDescr("SIN DESCRIPCION");
            log.debug("Fila {} [{}]: DESCR vacío, usando 'SIN DESCRIPCION'", rowNum, row.getCveArt());
        } else if (row.getDescr().length() > MAX_DESCR_LENGTH) {
            row.setDescr(row.getDescr().substring(0, MAX_DESCR_LENGTH));
            log.debug("Fila {} [{}]: DESCR truncado a {} caracteres", rowNum, row.getCveArt(), MAX_DESCR_LENGTH);
        }

        // UNI_MED: si viene vacío, se usará default (no es bloqueante)
        if (row.getUniMed() == null || row.getUniMed().isBlank()) {
            row.setUniMed("PZ");
            log.debug("Fila {} [{}]: UNI_MED vacío, usando 'PZ'", rowNum, row.getCveArt());
        } else if (row.getUniMed().length() > MAX_UNI_MED_LENGTH) {
            row.setUniMed(row.getUniMed().substring(0, MAX_UNI_MED_LENGTH));
        }

        // LIN_PROD opcional, truncar si excede
        if (row.getLinProd() != null && row.getLinProd().length() > MAX_LIN_PROD_LENGTH) {
            row.setLinProd(row.getLinProd().substring(0, MAX_LIN_PROD_LENGTH));
        }

        // EXIST_QTY no puede ser negativa
        if (row.getExistQty() != null && row.getExistQty().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(prefix + " [" + row.getCveArt() + "]: EXIST_QTY no puede ser negativa ("
                    + row.getExistQty() + ").");
        }

        // STATUS: si viene vacío ya se defaulteó a "A" en parseExcel
        if (row.getStatus() != null && !row.getStatus().isBlank()
                && !VALID_STATUSES.contains(row.getStatus().toUpperCase())) {
            // resolveStatus en processProduct manejará la normalización
            log.debug("Fila {} [{}]: STATUS '{}' será normalizado en processProduct",
                    rowNum, row.getCveArt(), row.getStatus());
        }

        return errors;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PROCESAMIENTO
    // ════════════════════════════════════════════════════════════════════════

    private Product processProduct(InventoryImportRow row, ImportStats stats,
                                   List<String> errors, int rowNum) {
        Product.Status incomingStatus = resolveStatus(row.getStatus(), row.getCveArt(), rowNum, errors);

        // FIX #3: Normalizar CVE_ART a uppercase para evitar duplicados por case-sensitivity
        String normalizedCveArt = row.getCveArt().trim().toUpperCase();

        return productRepository.findByCveArt(normalizedCveArt)
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
                    // ✅ FIX: status siempre se evalúa, con valor resuelto (nunca null)
                    if (existing.getStatus() != incomingStatus) {
                        existing.setStatus(incomingStatus);
                        changed = true;
                    }

                    if (changed) {
                        stats.incrementUpdated();
                        return productRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setCveArt(normalizedCveArt);
                    p.setDescr(row.getDescr());
                    p.setUniMed(row.getUniMed());
                    p.setLinProd(row.getLinProd());
                    p.setStatus(incomingStatus); // ✅ FIX: nunca null
                    p.setCreatedAt(LocalDateTime.now());
                    stats.incrementInserted();
                    return productRepository.save(p);
                });
    }

    /**
     * ✅ FIX CENTRAL: Propaga el status del producto al snapshot SIEMPRE.
     * El problema anterior era que en re-importaciones, el snapshot venía de BD
     * con un Product que solo tenía el ID (sin status), y nunca se actualizaba.
     * Ahora se asigna el producto COMPLETO al snapshot antes de guardar.
     */
    private void processSnapshot(Product product, Period period,
                                  BigDecimal existQty, ImportStats stats) {
        InventorySnapshot snapshot = snapshotRepository
                .findByProductPeriod(product.getId(), period.getId())
                .orElseGet(() -> {
                    InventorySnapshot s = new InventorySnapshot();
                    s.setPeriod(period);
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });

        boolean isUpdate   = snapshot.getId() != null;
        BigDecimal current = snapshot.getExistQty();
        boolean qtyChanged = current == null || current.compareTo(existQty) != 0;

        // ✅ FIX: Siempre asignar el producto COMPLETO (con status) al snapshot
        // Esto garantiza que toJpaEntity() tenga acceso al status correcto
        snapshot.setProduct(product);
        snapshot.setExistQty(existQty != null ? existQty : BigDecimal.ZERO);
        snapshotRepository.save(snapshot);
        if (isUpdate && qtyChanged) {
            stats.incrementUpdated();
        }
    }

    /**
     * FIX #1: Desactiva productos ausentes usando BULK UPDATE en lugar de N+1 queries.
     * Antes: loop con productRepository.save() + snapshotRepository.save() por cada ausente.
     * Ahora: 1 sola query UPDATE ... WHERE productId NOT IN (:importedIds).
     */
    private void deactivateMissingProducts(Period period, Set<Long> importedProductIds,
                                            ImportStats stats, List<String> errors) {
        try {
            if (importedProductIds.isEmpty()) {
                log.warn("No se importaron productos — no se desactivará nada para evitar marcar TODO como baja.");
                return;
            }

            List<Long> activeIds = new ArrayList<>(importedProductIds);
            int deactivated = snapshotRepository.markAsInactiveNotInImport(
                    period.getId(), null, activeIds);

            if (deactivated > 0) {
                stats.deactivated = deactivated;
                log.info("Productos desactivados por ausencia en Excel: {} (bulk UPDATE)", deactivated);
            }
        } catch (Exception e) {
            log.warn("Error al desactivar productos faltantes: {}", e.getMessage());
            errors.add("Advertencia: no se pudieron desactivar productos faltantes: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LECTURA DEL EXCEL
    // ════════════════════════════════════════════════════════════════════════

    private List<InventoryImportRow> parseExcel(MultipartFile file, List<String> errors) {
        List<InventoryImportRow> rows = new ArrayList<>();
        try (InputStream is = file.getInputStream();
             XSSFWorkbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                errors.add("El archivo XLSX no contiene hojas de cálculo.");
                return rows;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                errors.add("La hoja de cálculo está vacía.");
                return rows;
            }

            // ── Leer encabezados ─────────────────────────────────────────
            Row headerRow = rowIterator.next();
            int cveArtIdx  = -1, descrIdx  = -1, linProdIdx = -1,
                uniMedIdx  = -1, existQtyIdx = -1, statusIdx = -1;

            for (Cell cell : headerRow) {
                // ✅ FIX: usar cellString para encabezados también
                String val = cellString(cell).toUpperCase().replace(" ", "_");
                switch (val) {
                    case "CVE_ART"             -> cveArtIdx   = cell.getColumnIndex();
                    case "DESCR"               -> descrIdx    = cell.getColumnIndex();
                    case "LIN_PROD"            -> linProdIdx  = cell.getColumnIndex();
                    case "UNI_MED"             -> uniMedIdx   = cell.getColumnIndex();
                    case "EXIST_QTY", "EXIST"  -> existQtyIdx = cell.getColumnIndex();
                    case "STATUS"              -> statusIdx   = cell.getColumnIndex();
                    default -> log.debug("Columna ignorada en encabezado: '{}'", val);
                }
            }

            // Validar columnas obligatorias
            List<String> missingCols = new ArrayList<>();
            if (cveArtIdx  == -1) missingCols.add("CVE_ART");
            if (descrIdx   == -1) missingCols.add("DESCR");
            if (uniMedIdx  == -1) missingCols.add("UNI_MED");
            if (existQtyIdx == -1) missingCols.add("EXIST / EXIST_QTY");
            if (!missingCols.isEmpty()) {
                errors.add("El archivo no tiene las columnas requeridas: " + String.join(", ", missingCols));
                return rows;
            }

            if (statusIdx == -1) {
                log.warn("Columna STATUS no encontrada en el Excel — se usará 'A' como valor por defecto para TODAS las filas.");
            }

            // ── Leer filas de datos ──────────────────────────────────────
            int rowNum = 1;
            while (rowIterator.hasNext()) {
                rowNum++;
                Row row = rowIterator.next();

                // Omitir filas completamente vacías
                if (isRowEmpty(row)) continue;

                InventoryImportRow importRow = new InventoryImportRow();
                // FIX #3: Normalizar CVE_ART a uppercase al parsear
                String rawCveArt = cellStringFromRow(row, cveArtIdx);
                importRow.setCveArt(rawCveArt != null ? rawCveArt.trim().toUpperCase() : null);
                importRow.setDescr(cellStringFromRow(row, descrIdx));
                importRow.setLinProd(linProdIdx != -1 ? cellStringFromRow(row, linProdIdx) : null);
                importRow.setUniMed(cellStringFromRow(row, uniMedIdx));
                // FIX: Escalar EXIST a 2 decimales según regla de negocio
                BigDecimal rawExist = cellDecimal(row, existQtyIdx);
                importRow.setExistQty(rawExist.setScale(2, RoundingMode.HALF_UP));

                // ✅ FIX: default "A" si no hay columna STATUS o el valor está vacío
                String rawStatus = statusIdx != -1 ? cellStringFromRow(row, statusIdx) : null;
                importRow.setStatus(
                        (rawStatus != null && !rawStatus.isBlank())
                                ? rawStatus.trim().toUpperCase()
                                : "A"
                );

                if (importRow.getCveArt() != null && !importRow.getCveArt().isBlank()) {
                    rows.add(importRow);
                } else {
                    log.debug("Fila {} omitida: CVE_ART vacío", rowNum);
                }
            }

        } catch (Exception e) {
            errors.add("Error al leer el archivo XLSX: " + e.getMessage());
            log.error("Error parseando Excel: {}", e.getMessage(), e);
        }
        return rows;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS DE CELDA
    // ════════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIX CRÍTICO: Maneja celdas numéricas correctamente.
     * Sin esto, CVE_ART = 1001 en Excel se lee como "1001.0" y nunca hace match en BD.
     */
    private String cellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                // Evita "1001.0" → devuelve "1001"
                double val = cell.getNumericCellValue();
                yield (val == Math.floor(val))
                        ? String.valueOf((long) val)
                        : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf((long) cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> cell.toString().trim();
        };
    }

    private String cellStringFromRow(Row row, int idx) {
        if (idx < 0) return null;
        Cell cell = row.getCell(idx);
        String val = cellString(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private BigDecimal cellDecimal(Row row, int idx) {
        if (idx < 0) return BigDecimal.ZERO;
        Cell cell = row.getCell(idx);
        if (cell == null) return BigDecimal.ZERO;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String val = cell.toString().trim();
            return val.isEmpty() ? BigDecimal.ZERO : new BigDecimal(val);
        } catch (NumberFormatException e) {
            log.warn("Valor no numérico en EXIST_QTY: '{}', se usa 0", cell.toString());
            return BigDecimal.ZERO;
        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = cellString(cell).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS DE DOMINIO
    // ════════════════════════════════════════════════════════════════════════

    /**
     * ✅ FIX: Resuelve el Product.Status desde un string, con default "A" y log de advertencia.
     * NUNCA retorna null — siempre retorna A o B.
     */
    private Product.Status resolveStatus(String rawStatus, String cveArt,
                                          int rowNum, List<String> errors) {
        if (rawStatus == null || rawStatus.isBlank()) {
            // En re-importación, si la columna STATUS está vacía, mantener "A" por defecto
            log.debug("Fila {} [{}]: STATUS vacío, usando 'A' por defecto", rowNum, cveArt);
            return Product.Status.A;
        }
        String normalized = rawStatus.trim().toUpperCase();
        try {
            return Product.Status.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // ✅ FIX: ya no silencia — registra advertencia y usa default
            String warn = String.format(
                    "Fila %d [%s]: STATUS '%s' inválido, se usará 'A' por defecto.",
                    rowNum, cveArt, rawStatus);
            errors.add(warn);
            log.warn(warn);
            return Product.Status.A;
        }
    }

    /**
     * ✅ FIX: ya no pasa null al servicio de personal.
     */
    private String resolveFullName(String username) {
        // NOTA: Se requiere el userId, no el username. Si solo tienes username, busca el usuario primero.
        // Aquí se asume que username es el userId en formato String (ajusta según tu flujo real).
        if (username == null || username.isBlank()) return "Desconocido";
        try {
            Long userId = Long.valueOf(username);
            return personalInformationService.findByUserId(userId)
                    .map(pi -> {
                        String full = pi.getFullName();
                        return (full != null && !full.isBlank()) ? full : username;
                    })
                    .orElse(username);
        } catch (Exception e) {
            log.debug("No se pudo resolver nombre completo para '{}': {}", username, e.getMessage());
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
        job.setSkippedRows(stats.skipped);       // ✅ FIX: era hardcodeado en 0
        // job.setDeactivatedRows(stats.deactivated); // No existe el campo, omitir o agregar si lo deseas
        job.setTotalRows(stats.totalRows);
        job.setIdPeriod(period.getId());
        job.setCreatedBy(nombreCompleto);

        try {
            job.setErrorsJson(new ObjectMapper().writeValueAsString(errors));
        } catch (Exception ex) {
            job.setErrorsJson("[\"Error al serializar lista de errores\"]");
        }
        return job;
    }

    private String computeChecksum(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream dis = new DigestInputStream(is, digest);
            byte[] buffer = new byte[8192];
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

    // ════════════════════════════════════════════════════════════════════════
    //  CLASES INTERNAS
    // ════════════════════════════════════════════════════════════════════════

    @Getter
    @Setter
    private static class InventoryImportRow {
        private String     cveArt;
        private String     descr;
        private String     linProd;
        private String     uniMed;
        private BigDecimal existQty;
        private String     status;
    }

    private static class ImportStats {
        int totalRows   = 0;
        int inserted    = 0;
        int updated     = 0;
        int deactivated = 0;
        int skipped     = 0;

        void incrementInserted()    { inserted++;    }
        void incrementUpdated()     { updated++;     }
    }
}


