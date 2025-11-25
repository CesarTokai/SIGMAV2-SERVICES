package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;

public interface MultiWarehouseService {
    Page<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search, Pageable pageable);
    ResponseEntity<?> importFile(MultipartFile file, String period);
    ResponseEntity<?> processWizardStep(MultiWarehouseWizardStepDTO stepDTO);
    ResponseEntity<?> exportExistences(MultiWarehouseSearchDTO search);
    ResponseEntity<?> getImportLog(Long id);
    ResponseEntity<?> getStock(String productCode, String warehouseKey, Long periodId);
}
