package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryImportJobUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryImportJobRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InventoryImportJobService implements InventoryImportJobUseCase {

    private final InventoryImportJobRepository importJobRepository;


    @Override
    public List<InventoryImportJob> listImportJobs() {
        return importJobRepository.findAll();
    }

    @Override
    public Optional<InventoryImportJob> getImportJobById(Long jobId) {
        return importJobRepository.findById(jobId);
    }
}
