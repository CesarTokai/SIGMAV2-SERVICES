package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.output.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryPersistencePort;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InventoryPersistenceAdapter implements InventoryPersistencePort {

    private final Map<String, Map<String, Object>> inventoryData = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> importJobs = new ConcurrentHashMap<>();

    @Override
    public void saveInventoryData(Map<String, Object> data) {
        String type = (String) data.get("type");
        inventoryData.put(type, data);
    }

    @Override
    public Map<String, Object> getInventoryByType(String type) {
        return inventoryData.getOrDefault(type, new ConcurrentHashMap<>());
    }

    @Override
    public void saveImportJob(String jobId, String status, String message) {
        Map<String, Object> jobData = new ConcurrentHashMap<>();
        jobData.put("status", status);
        jobData.put("message", message);
        importJobs.put(jobId, jobData);
    }

    @Override
    public Map<String, Object> getImportJobById(String jobId) {
        return importJobs.getOrDefault(jobId, new ConcurrentHashMap<>());
    }
}
