package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.output.persistence;

import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryPersistencePort;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InventoryPersistenceAdapter implements InventoryPersistencePort {

    private final Map<String, Map<String, Object>> inventoryData = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> importJobs = new ConcurrentHashMap<>();

    private String key(String type, String period, Long warehouseId) {
        return type + "|" + period + "|" + (warehouseId == null ? "*" : warehouseId);
    }

    @Override
    public void saveInventoryData(Map<String, Object> data) {
        String type = (String) data.get("type");
        String period = (String) data.get("period");
        Long warehouseId = (Long) data.get("warehouseId");
        // keep last by type for backward-compatible status
        inventoryData.put(type, data);
        // also store by composite key for querying
        inventoryData.put(key(type, period, warehouseId), data);
    }

    @Override
    public Map<String, Object> getInventoryByType(String type) {
        return inventoryData.getOrDefault(type, new ConcurrentHashMap<>());
    }

    @Override
    public void saveImportJob(String jobId, String status, String message) {
        Map<String, Object> jobData = importJobs.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>());
        jobData.put("status", status);
        jobData.put("message", message);
    }

    @Override
    public Map<String, Object> getImportJobById(String jobId) {
        return importJobs.getOrDefault(jobId, new ConcurrentHashMap<>());
    }

    @Override
    public void saveImportLog(String jobId, byte[] csvLog) {
        Map<String, Object> jobData = importJobs.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>());
        jobData.put("log", csvLog);
    }

    @Override
    public byte[] getImportLog(String jobId) {
        Map<String, Object> jobData = importJobs.get(jobId);
        if (jobData == null) return null;
        Object log = jobData.get("log");
        return log instanceof byte[] ? (byte[]) log : null;
    }

    @Override
    public Map<String, Object> getInventoryByTypeAndPeriod(String type, String period, Long warehouseId) {
        Map<String, Object> data = inventoryData.get(key(type, period, warehouseId));
        if (data != null) return data;
        // fallback: try wildcard warehouse
        data = inventoryData.get(key(type, period, null));
        return data != null ? data : new ConcurrentHashMap<>();
    }

    @Override
    public Map<String, Object> getExportableInventory(String type, String period, Long warehouseId, String search) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getInventoryMatrix(String period, Long[] warehouseIds) {
        return Map.of();
    }

    @Override
    public void saveExportLog(String exportId, byte[] logData) {

    }

    @Override
    public void saveMultiWarehouseInventoryData(Map<String, Object> data) {

    }
}
