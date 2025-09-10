package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import java.util.Map;

public interface InventoryPersistencePort {
    void saveInventoryData(Map<String, Object> data);
    Map<String, Object> getInventoryByType(String type);
    void saveImportJob(String jobId, String status, String message);
    Map<String, Object> getImportJobById(String jobId);
}
