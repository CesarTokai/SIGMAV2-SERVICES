package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;

import java.util.List;
import java.util.Optional;

public interface InventoryImportJobUseCase {
    List<InventoryImportJob> listImportJobs();
    Optional<InventoryImportJob> getImportJobById(Long jobId);
}