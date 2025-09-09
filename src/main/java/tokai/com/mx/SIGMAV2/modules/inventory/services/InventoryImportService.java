package tokai.com.mx.SIGMAV2.modules.inventory.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.ImportLogEntryDto;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.ImportResultDto;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.InventoryImportRowDto;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.*;
import tokai.com.mx.SIGMAV2.modules.inventory.exceptions.InventoryException;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.*;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InventoryImportService {
    
    @Autowired
    private FileParserService fileParserService;
    
    @Autowired
    private ImportLogService importLogService;
    
    @Autowired
    private InventoryProductRepository productRepository;
    
    @Autowired
    private InventoryWarehouseRepository warehouseRepository;
    
    @Autowired
    private InventoryPeriodRepository periodRepository;
    
    @Autowired
    private InventoryStockRepository inventoryStockRepository;
    
    @Autowired
    private InventoryImportJobRepository importJobRepository;
    
    public enum ImportMode {
        MERGE, REPLACE
    }
    
    @Transactional
    public ImportResultDto importInventory(MultipartFile file, Long idPeriod, Long idWarehouse, 
                                         ImportMode mode, String idempotencyKey) throws Exception {
        
        long startTime = System.currentTimeMillis();
        String jobId = UUID.randomUUID().toString();
        List<ImportLogEntryDto> logEntries = new ArrayList<>();
        
        // 1. Validar periodo obligatorio
        Optional<Period> periodOpt = periodRepository.findById(idPeriod);
        if (periodOpt.isEmpty()) {
            throw InventoryException.periodNotFound(idPeriod);
        }
        Period period = periodOpt.get();
        
        // 2. Validar almacén si se especifica
        Warehouse warehouse = null;
        if (idWarehouse != null) {
            Optional<Warehouse> warehouseOpt = warehouseRepository.findById(idWarehouse);
            if (warehouseOpt.isEmpty()) {
                throw InventoryException.warehouseNotFound(idWarehouse);
            }
            warehouse = warehouseOpt.get();
        }
        
        // 3. Crear job de importación
        InventoryImportJob importJob = new InventoryImportJob(period, warehouse, 
            file.getOriginalFilename(), "system"); // TODO: obtener usuario actual
        
        // 4. Calcular checksum para idempotencia
        String checksum = calculateChecksum(file);
        importJob.setChecksum(checksum);
        
        // 5. Verificar duplicados (idempotencia)
        Optional<InventoryImportJob> existingJob = importJobRepository
            .findByPeriodAndWarehouseAndChecksum(idPeriod, idWarehouse, checksum);
        
        if (existingJob.isPresent() && mode != ImportMode.REPLACE) {
            throw InventoryException.duplicateImport(checksum);
        }
        
        importJob.setStatus(InventoryImportJob.JobStatus.RUNNING);
        importJob = importJobRepository.save(importJob);
        
        try {
            // 6. Parsear archivo (formato Excel obligatorio según reglas)
            List<InventoryImportRowDto> rows = fileParserService.parseFile(file);
            importJob.setTotalRows(rows.size());
            
            // 7. Validar duplicados en el archivo y productos inexistentes
            List<String> validationErrors = validateBusinessRules(rows, idWarehouse, logEntries);
            
            if (!validationErrors.isEmpty()) {
                importJob.setStatus(InventoryImportJob.JobStatus.ERROR);
                importJob.setErrorsJson(String.join("; ", validationErrors));
                importJob.setFinishedAt(LocalDateTime.now());
                importJobRepository.save(importJob);
                
                ImportResultDto result = new ImportResultDto();
                result.setJobId(jobId);
                result.setPeriod(period.getPeriod().toString());
                result.setMode(mode.name());
                result.setErrors(validationErrors);
                result.setTotalRows(rows.size());
                result.setDurationMs(System.currentTimeMillis() - startTime);
                return result;
            }
            
            // 8. Aplicar cambios en transacción (todo o nada)
            ImportStats stats = applyChangesWithLogging(rows, idWarehouse, logEntries);
            
            // 9. Generar log descargable CSV
            String logFilePath = importLogService.generateLogFile(idPeriod, 
                file.getOriginalFilename(), logEntries);
            
            // 10. Actualizar job como completado
            importJob.setInsertedRows(stats.inserted);
            importJob.setUpdatedRows(stats.updated);
            importJob.setSkippedRows(stats.skipped);
            importJob.setStatus(InventoryImportJob.JobStatus.DONE);
            importJob.setLogFilePath(logFilePath);
            importJob.setFinishedAt(LocalDateTime.now());
            importJobRepository.save(importJob);
            
            long duration = System.currentTimeMillis() - startTime;
            
            ImportResultDto result = new ImportResultDto();
            result.setJobId(jobId);
            result.setJobIdLong(importJob.getId());
            result.setPeriod(period.getPeriod().toString());
            result.setMode(mode.name());
            result.setTotalRows(rows.size());
            result.setInserted(stats.inserted);
            result.setUpdated(stats.updated);
            result.setSkipped(stats.skipped);
            result.setDurationMs(duration);
            result.setLogDownloadUrl("/api/v1/inventory/import/log/" + importJob.getId());
            
            return result;
            
        } catch (Exception e) {
            // En caso de error, marcar job como fallido y hacer rollback
            importJob.setStatus(InventoryImportJob.JobStatus.ERROR);
            importJob.setErrorsJson("Error de procesamiento: " + e.getMessage());
            importJob.setFinishedAt(LocalDateTime.now());
            importJobRepository.save(importJob);
            
            throw e; // Re-lanzar para que se haga rollback de la transacción
        }
    }
    
    private String calculateChecksum(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(file.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    private List<String> validateBusinessRules(List<InventoryImportRowDto> rows, Long idWarehouse, 
                                              List<ImportLogEntryDto> logEntries) {
        List<String> errors = new ArrayList<>();
        Set<String> processedKeys = new HashSet<>(); // Para detectar duplicados en el archivo
        
        for (InventoryImportRowDto row : rows) {
            // Validar campos obligatorios
            if (row.getCveArt() == null || row.getCveArt().trim().isEmpty()) {
                String error = "Fila " + row.getRowNumber() + ": CVE_ART es obligatorio";
                errors.add(error);
                logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), "", error));
                continue;
            }
            
            if (row.getDescr() == null || row.getDescr().trim().isEmpty()) {
                String error = "Fila " + row.getRowNumber() + ": DESCR es obligatorio";
                errors.add(error);
                logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), row.getCveArt(), error));
                continue;
            }
            
            if (row.getUniMed() == null || row.getUniMed().trim().isEmpty()) {
                String error = "Fila " + row.getRowNumber() + ": UNI_MED es obligatorio";
                errors.add(error);
                logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), row.getCveArt(), error));
                continue;
            }
            
            // Normalizar clave de artículo
            String normalizedKey = row.getCveArt().trim().toUpperCase();
            row.setCveArt(normalizedKey);
            
            // Validar duplicados en el archivo
            String duplicateKey = normalizedKey + (idWarehouse != null ? "_" + idWarehouse : 
                (row.getWarehouseKey() != null ? "_" + row.getWarehouseKey().trim() : ""));
            
            if (processedKeys.contains(duplicateKey)) {
                String error = "Fila " + row.getRowNumber() + ": Producto duplicado en el archivo: " + normalizedKey;
                errors.add(error);
                logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), normalizedKey, error));
                continue;
            }
            processedKeys.add(duplicateKey);
            
            // Validar que el producto existe
            if (!productRepository.existsByCveArt(normalizedKey)) {
                String error = "Fila " + row.getRowNumber() + ": Producto inexistente: " + normalizedKey;
                errors.add(error);
                logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), normalizedKey, error));
                continue;
            }
            
            // Validar existencia no negativa
            if (row.getExist() == null || row.getExist().compareTo(BigDecimal.ZERO) < 0) {
                String error = "Fila " + row.getRowNumber() + ": EXIST debe ser >= 0";
                errors.add(error);
                logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), normalizedKey, error));
                continue;
            }
            
            // Validar status
            if (row.getStatus() != null && !row.getStatus().matches("^[AB]$")) {
                String error = "Fila " + row.getRowNumber() + ": STATUS debe ser 'A' o 'B'";
                errors.add(error);
                logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), normalizedKey, error));
                continue;
            }
            
            // Si no se especifica almacén en parámetro, debe venir en archivo
            if (idWarehouse == null) {
                if (row.getWarehouseKey() == null || row.getWarehouseKey().trim().isEmpty()) {
                    String error = "Fila " + row.getRowNumber() + ": WAREHOUSE_KEY es obligatorio cuando no se especifica almacén";
                    errors.add(error);
                    logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), normalizedKey, error));
                    continue;
                }
                
                if (!warehouseRepository.existsByWarehouseKey(row.getWarehouseKey().trim())) {
                    String error = "Fila " + row.getRowNumber() + ": Almacén inexistente: " + row.getWarehouseKey();
                    errors.add(error);
                    logEntries.add(ImportLogEntryDto.error(row.getRowNumber(), normalizedKey, error));
                    continue;
                }
            }
        }
        
        return errors;
    }
    
    private ImportStats applyChangesWithLogging(List<InventoryImportRowDto> rows, Long idWarehouse, 
                                               List<ImportLogEntryDto> logEntries) {
        ImportStats stats = new ImportStats();
        
        for (InventoryImportRowDto row : rows) {
            try {
                // Obtener producto
                Product product = productRepository.findByCveArt(row.getCveArt().trim().toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + row.getCveArt()));
                
                // Obtener almacén
                Warehouse warehouse;
                if (idWarehouse != null) {
                    warehouse = warehouseRepository.findById(idWarehouse)
                        .orElseThrow(() -> new RuntimeException("Almacén no encontrado: " + idWarehouse));
                } else {
                    warehouse = warehouseRepository.findByWarehouseKey(row.getWarehouseKey().trim())
                        .orElseThrow(() -> new RuntimeException("Almacén no encontrado: " + row.getWarehouseKey()));
                }
                
                // Upsert inventory stock con logging detallado
                Optional<InventoryStock> existingStock = inventoryStockRepository
                    .findByProductIdProductAndWarehouseIdWarehouse(product.getIdProduct(), warehouse.getIdWarehouse());
                
                if (existingStock.isPresent()) {
                    // Actualizar registro existente
                    InventoryStock stock = existingStock.get();
                    BigDecimal previousQty = stock.getExistQty();
                    InventoryStock.StockStatus previousStatus = stock.getStatus();
                    
                    stock.setExistQty(row.getExist());
                    stock.setStatus(InventoryStock.StockStatus.valueOf(row.getStatus() != null ? row.getStatus() : "A"));
                    inventoryStockRepository.save(stock);
                    
                    stats.updated++;
                    logEntries.add(ImportLogEntryDto.update(
                        row.getRowNumber(), 
                        row.getCveArt(),
                        "Actualizado en almacén " + warehouse.getWarehouseKey(),
                        "Qty: " + previousQty + ", Status: " + previousStatus,
                        "Qty: " + row.getExist() + ", Status: " + stock.getStatus()
                    ));
                    
                } else {
                    // Insertar nuevo registro
                    InventoryStock newStock = new InventoryStock();
                    newStock.setProduct(product);
                    newStock.setWarehouse(warehouse);
                    newStock.setExistQty(row.getExist());
                    newStock.setStatus(InventoryStock.StockStatus.valueOf(row.getStatus() != null ? row.getStatus() : "A"));
                    inventoryStockRepository.save(newStock);
                    
                    stats.inserted++;
                    logEntries.add(ImportLogEntryDto.insert(
                        row.getRowNumber(), 
                        row.getCveArt(),
                        "Insertado en almacén " + warehouse.getWarehouseKey() + 
                        " con cantidad " + row.getExist()
                    ));
                }
                
            } catch (Exception e) {
                stats.skipped++;
                logEntries.add(ImportLogEntryDto.error(
                    row.getRowNumber(), 
                    row.getCveArt(),
                    "Error de procesamiento: " + e.getMessage()
                ));
                // Log error pero continuar procesando
                System.err.println("Error procesando fila " + row.getRowNumber() + ": " + e.getMessage());
            }
        }
        
        return stats;
    }
    
    private static class ImportStats {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
    }
}