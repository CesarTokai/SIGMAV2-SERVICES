package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface InventoryOperationsPort {
    void importInventory(MultipartFile file, String type, String period, Long warehouseId);
    Map<String, Object> getInventoryStatus(String type);
    Map<String, Object> getImportJobStatus(String jobId);
    byte[] getImportLog(String jobId);
    Map<String, Object> queryInventory(String type, String period, Long warehouseId, String search, Integer page, Integer size);

    // Exportar inventario respetando filtros y formato
    byte[] exportInventory(String type, String period, Long warehouseId, String search, String format);

    // Obtener matriz multi-almacén consolidada
    Map<String, Object> getInventoryMatrix(String period, Long[] warehouseIds);

    // Importar inventario multi-almacén
    void importMultiWarehouseInventory(MultipartFile file, String period);
}
