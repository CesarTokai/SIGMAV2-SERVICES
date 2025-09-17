package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import java.util.Map;

public interface InventoryPersistencePort {
    void saveInventoryData(Map<String, Object> data);
    Map<String, Object> getInventoryByType(String type);
    void saveImportJob(String jobId, String status, String message);
    Map<String, Object> getImportJobById(String jobId);
    void saveImportLog(String jobId, byte[] csvLog);
    byte[] getImportLog(String jobId);

    // New query to fetch inventory entries by type + period + optional warehouse
    Map<String, Object> getInventoryByTypeAndPeriod(String type, String period, Long warehouseId);

    // Obtener datos exportables según filtros y formato
    Map<String, Object> getExportableInventory(String type, String period, Long warehouseId, String search);

    // Obtener matriz multi-almacén consolidada
    Map<String, Object> getInventoryMatrix(String period, Long[] warehouseIds);

    // Guardar log de exportación
    void saveExportLog(String exportId, byte[] logData);

    // Métodos para importación multi-almacén
    void saveMultiWarehouseInventoryData(Map<String, Object> data);
}
