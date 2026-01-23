package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseSearchDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseStockRequestDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseWizardStepDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.service.MultiWarehouseService;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/sigmav2/multi-warehouse")
public class MultiWarehouseController {

    private static final Logger log = LoggerFactory.getLogger(MultiWarehouseController.class);

    private final MultiWarehouseService multiWarehouseService;

    public MultiWarehouseController(MultiWarehouseService multiWarehouseService) {
        this.multiWarehouseService = multiWarehouseService;
    }

    @PostMapping("/existences")
    public Page<MultiWarehouseExistence> getExistences(
            @RequestBody MultiWarehouseSearchDTO searchDTO,
            Pageable pageable) {
        log.info("POST /existences - Search params: periodId={}, period={}, search={}, pageSize={}, orderBy={}, ascending={}",
                 searchDTO.getPeriodId(),
                 searchDTO.getPeriod(),
                 searchDTO.getSearch(),
                 searchDTO.getPageSize(),
                 searchDTO.getOrderBy(),
                 searchDTO.getAscending());
        return multiWarehouseService.findExistences(searchDTO, pageable);
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

    @PostMapping("/stock")
    public ResponseEntity<?> getStock(@RequestBody MultiWarehouseStockRequestDTO request) {
        return multiWarehouseService.getStock(
            request.getProductCode(),
            request.getWarehouseKey(),
            request.getPeriodId()
        );
    }
}
