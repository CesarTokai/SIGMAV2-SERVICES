package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence.MultiWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence.imports.MultiWarehouseImportLogRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiWarehouseServiceImpl implements MultiWarehouseService {
    private final MultiWarehouseRepository multiWarehouseRepository;
    private final MultiWarehouseImportLogRepository importLogRepository;

    @Override
    public Page<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 50);
        }
        return multiWarehouseRepository.findExistences(search, pageable);
    }

    @Override
    public ResponseEntity<?> importFile(MultipartFile file, String period) {
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().body("Periodo* es obligatorio");
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Archivo multialmacen.xlsx* es obligatorio");
        }
        // Persist simple import log entry
        MultiWarehouseImportLog log = new MultiWarehouseImportLog();
        log.setFileName(file.getOriginalFilename());
        log.setPeriod(period);
        log.setImportDate(LocalDateTime.now());
        log.setStatus("SUCCESS");
        log.setMessage("Importación iniciada");
        MultiWarehouseImportLog saved = importLogRepository.save(log);
        return ResponseEntity.ok(saved);
    }

    @Override
    public ResponseEntity<?> processWizardStep(MultiWarehouseWizardStepDTO stepDTO) {
        if (stepDTO == null) {
            return ResponseEntity.badRequest().body("Datos del wizard requeridos");
        }
        int step = stepDTO.getStepNumber();
        switch (step) {
            case 1:
                // Validar requeridos
                if (stepDTO.getPeriod() == null || stepDTO.getPeriod().isBlank()) {
                    return ResponseEntity.badRequest().body("Periodo* es obligatorio");
                }
                if (stepDTO.getFileName() == null || stepDTO.getFileName().isBlank()) {
                    return ResponseEntity.badRequest().body("Archivo multialmacen.xlsx* es obligatorio");
                }
                return ResponseEntity.ok("Paso 1 validado");
            case 2:
            case 3:
                // En un escenario real se detectan y muestran faltantes
                // Forzamos la regla: Debe resolver faltantes antes de continuar
                return ResponseEntity.status(409).body("Debe resolver faltantes antes de continuar");
            case 4:
                // Confirmación de bajas obligatoria para marcar STATUS = B
                if (!stepDTO.isConfirmBajas()) {
                    return ResponseEntity.badRequest().body("Se exige casilla ‘Confirmo las bajas’ para continuar");
                }
                return ResponseEntity.ok("Bajas confirmadas");
            case 5:
                // Finalizar: generar resumen y log descargable (devolvemos un texto/resumen simple)
                String resumen = "Importación finalizada. Registros procesados: " + 0 + ", Bajas confirmadas: " + (stepDTO.isConfirmBajas() ? "Sí" : "No");
                return ResponseEntity.ok(resumen);
            default:
                return ResponseEntity.badRequest().body("Paso del wizard no soportado");
        }
    }

    @Override
    public ResponseEntity<?> exportExistences(MultiWarehouseSearchDTO search) {
        // Export filtered results to CSV
        Page<MultiWarehouseExistence> page = multiWarehouseRepository.findExistences(search, PageRequest.of(0, Integer.MAX_VALUE));
        List<MultiWarehouseExistence> list = page.getContent();
        String header = "Almacen,Producto,Descripcion,Existencias,Estado";
        String rows = list.stream().map(e -> String.join(",",
                safe(e.getWarehouseName()),
                safe(e.getProductCode()),
                safe(e.getProductName()),
                e.getStock() == null ? "" : e.getStock().toPlainString(),
                safe(e.getStatus())
        )).collect(Collectors.joining("\n"));
        String csv = header + "\n" + rows + (rows.isEmpty() ? "" : "\n");
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=multiwarehouse_export.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String safe(String v) {
        if (v == null) return "";
        // Escape commas and quotes for CSV
        if (v.contains(",") || v.contains("\"")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    @Override
    public ResponseEntity<?> getImportLog(Long id) {
        return importLogRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

