package tokai.com.mx.SIGMAV2.modules.inventory.application.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryOperationsPort;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryPersistencePort;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.ImportJob;

import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService implements InventoryOperationsPort {

    private final InventoryPersistencePort persistencePort;

    public InventoryService(InventoryPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public void importInventory(MultipartFile file, String type) {
        ImportJob job = new ImportJob(UUID.randomUUID().toString(), type);
        try {
            // Lógica de importación aquí
            persistencePort.saveImportJob(job.getId(), job.getStatus(), "Procesamiento iniciado");
        } catch (Exception e) {
            job.markAsFailed(e.getMessage());
            persistencePort.saveImportJob(job.getId(), job.getStatus(), job.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, Object> getInventoryStatus(String type) {
        return persistencePort.getInventoryByType(type);
    }

    @Override
    public Map<String, Object> getImportJobStatus(String jobId) {
        return persistencePort.getImportJobById(jobId);
    }
}
