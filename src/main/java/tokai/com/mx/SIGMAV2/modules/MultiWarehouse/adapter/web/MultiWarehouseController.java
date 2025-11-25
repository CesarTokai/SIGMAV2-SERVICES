package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseSearchDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseWizardStepDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.service.MultiWarehouseService;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;

@RestController
@RequestMapping("/api/sigmav2/multi-warehouse")
public class MultiWarehouseController {

    @Autowired
    private MultiWarehouseService multiWarehouseService;

    @GetMapping("/existences")
    public Page<MultiWarehouseExistence> getExistences(MultiWarehouseSearchDTO search, Pageable pageable) {
        return multiWarehouseService.findExistences(search, pageable);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/import")
    public ResponseEntity<?> importMultiWarehouse(@RequestParam("file") MultipartFile file, @RequestParam("period") String period) {
        return multiWarehouseService.importFile(file, period);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/wizard/step")
    public ResponseEntity<?> wizardStep(@RequestBody MultiWarehouseWizardStepDTO stepDTO) {
        return multiWarehouseService.processWizardStep(stepDTO);
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportExistences(MultiWarehouseSearchDTO search) {
        return multiWarehouseService.exportExistences(search);
    }

    @GetMapping("/import/log/{id}")
    public ResponseEntity<?> getImportLog(@PathVariable Long id) {
        return multiWarehouseService.getImportLog(id);
    }

    @GetMapping("/stock")
    public ResponseEntity<?> getStock(
        @RequestParam String productCode,
        @RequestParam String warehouseKey,
        @RequestParam Long periodId
    ) {
        return multiWarehouseService.getStock(productCode, warehouseKey, periodId);
    }
}
