package tokai.com.mx.SIGMAV2.modules.inventory.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.ImportResultDto;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.InventoryDto;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.InventoryImportJob;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.InventoryImportJobRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.services.InventoryImportService;
import tokai.com.mx.SIGMAV2.modules.inventory.services.InventoryQueryService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {
    
    @Autowired
    private InventoryImportService inventoryImportService;
    
    @Autowired
    private InventoryQueryService inventoryQueryService;
    
    @Autowired
    private InventoryImportJobRepository importJobRepository;
    
    @PostMapping("/import")
    public ResponseEntity<?> importInventory(
            @RequestParam("file") MultipartFile file,
            @RequestParam("idPeriod") Long idPeriod,
            @RequestParam(value = "idWarehouse", required = false) Long idWarehouse,
            @RequestParam(value = "mode", defaultValue = "MERGE") String mode,
            @RequestParam(value = "idempotencyKey", required = false) String idempotencyKey) {
        
        try {
            // Validar archivo
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("El archivo no puede estar vacío"));
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && 
                                   !filename.toLowerCase().endsWith(".xls") && 
                                   !filename.toLowerCase().endsWith(".csv"))) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(createErrorResponse("Formato de archivo no soportado. Use XLSX, XLS o CSV."));
            }
            
            // Validar modo
            InventoryImportService.ImportMode importMode;
            try {
                importMode = InventoryImportService.ImportMode.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Modo inválido. Use MERGE o REPLACE."));
            }
            
            // Procesar importación
            ImportResultDto result = inventoryImportService.importInventory(
                file, idPeriod, idWarehouse, importMode, idempotencyKey);
            
            // Si hay errores de validación, retornar 400
            if (result.getErrors() != null && !result.getErrors().isEmpty()) {
                return ResponseEntity.badRequest().body(result);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getInventory(
            @RequestParam("idPeriod") Long idPeriod,
            @RequestParam(value = "idWarehouse", required = false) Long idWarehouse,
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "sort", defaultValue = "cveArt") String sort) {
        
        try {
            // Validar parámetros de paginación
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 25;
            
            // Crear sort
            Sort sortBy = Sort.by(Sort.Direction.ASC, mapSortField(sort));
            Pageable pageable = PageRequest.of(page, size, sortBy);
            
            // Obtener inventarios
            Page<InventoryDto> inventoryPage = inventoryQueryService.getInventory(
                idPeriod, idWarehouse, query, pageable);
            
            return ResponseEntity.ok(inventoryPage);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    @GetMapping("/import/log/{jobId}")
    public ResponseEntity<?> downloadImportLog(@PathVariable("jobId") Long jobId) {
        try {
            Optional<InventoryImportJob> jobOpt = importJobRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            InventoryImportJob job = jobOpt.get();
            if (job.getLogFilePath() == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("No hay log disponible para este job"));
            }
            
            File logFile = new File(job.getLogFilePath());
            if (!logFile.exists()) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Archivo de log no encontrado"));
            }
            
            Resource resource = new FileSystemResource(logFile);
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + logFile.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error descargando log: " + e.getMessage()));
        }
    }
    
    @GetMapping("/export")
    public ResponseEntity<?> exportInventory(
            @RequestParam("idPeriod") Long idPeriod,
            @RequestParam(value = "idWarehouse", required = false) Long idWarehouse,
            @RequestParam(value = "format", defaultValue = "CSV") String format,
            @RequestParam(value = "q", required = false) String query) {
        
        try {
            // TODO: Implementar exportación
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(createErrorResponse("Funcionalidad de exportación aún no implementada"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    @GetMapping("/multi-warehouse")
    public ResponseEntity<?> getMultiWarehouseView(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            @RequestParam(value = "sort", defaultValue = "cveArt") String sort) {
        
        try {
            // TODO: Implementar vista multi-almacén
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(createErrorResponse("Vista multi-almacén aún no implementada"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error interno del servidor: " + e.getMessage()));
        }
    }
    
    private String mapSortField(String sort) {
        switch (sort.toLowerCase()) {
            case "cveart":
            case "cve_art":
                return "product.cveArt";
            case "description":
            case "descr":
                return "product.description";
            case "existqty":
            case "exist":
                return "existQty";
            case "warehouse":
                return "warehouse.warehouseKey";
            default:
                return "product.cveArt";
        }
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}