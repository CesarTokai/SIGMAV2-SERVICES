package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryImportRequestDTO;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;

public interface InventoryImportUseCase {
    InventoryImportJob importInventory(InventoryImportRequestDTO request);
}
