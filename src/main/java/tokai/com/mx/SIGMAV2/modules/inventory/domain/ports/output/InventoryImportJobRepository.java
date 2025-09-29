package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;

import java.util.List;
import java.util.Optional;

public interface InventoryImportJobRepository {
    InventoryImportJob save(InventoryImportJob job);
    List<InventoryImportJob> findAll();
    Optional<InventoryImportJob> findById(Long id);
}