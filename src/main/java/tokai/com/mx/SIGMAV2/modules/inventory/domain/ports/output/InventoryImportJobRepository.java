package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;

public interface InventoryImportJobRepository {
    InventoryImportJob save(InventoryImportJob job);
}