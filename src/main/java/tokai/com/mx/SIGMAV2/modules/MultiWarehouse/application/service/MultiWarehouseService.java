package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;

/**
 * @deprecated Usar {@link tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.input.MultiWarehouseUseCase}.
 * Esta interfaz se mantiene solo por compatibilidad transitoria y será eliminada
 * cuando el controlador complete la migración al nuevo puerto de entrada.
 */
@Deprecated(since = "2.1", forRemoval = true)
public interface MultiWarehouseService {

    Page<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search, Pageable pageable);

    // Métodos legacy renombrados para dejar claro que no deben usarse directamente
    ResponseEntity<?> importFile_legacy(MultipartFile file, String period);
    ResponseEntity<?> processWizardStep_legacy(MultiWarehouseWizardStepDTO stepDTO);
    ResponseEntity<?> exportExistences_legacy(MultiWarehouseSearchDTO search);
    ResponseEntity<?> getImportLog_legacy(Long id);
    ResponseEntity<?> getStock_legacy(String productCode, String warehouseKey, Long periodId);
    ResponseEntity<?> getProductosDadosDeBaja_legacy(Long periodId);
}
