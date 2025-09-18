package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportRequestDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportResultDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;

public interface InventoryImportUseCase {
    InventoryImportResultDTO importInventory(InventoryImportRequestDTO request);

}
