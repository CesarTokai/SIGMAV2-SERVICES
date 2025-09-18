package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;


import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryImportJobUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryImportJobRepository;

import java.util.List;
import java.util.Optional;

public class InventoryImportJobService implements InventoryImportJobUseCase {

    private final InventoryImportJobRepository importJobRepository;

    public InventoryImportJobService(InventoryImportJobRepository importJobRepository) {
        this.importJobRepository = importJobRepository;
    }

    @Override
    public List<InventoryImportJob> listImportJobs() {
        return importJobRepository.findAll();
    }

    @Override
    public Optional<InventoryImportJob> getImportJobById(Long jobId) {
        return importJobRepository.findById(jobId);
    }
}
