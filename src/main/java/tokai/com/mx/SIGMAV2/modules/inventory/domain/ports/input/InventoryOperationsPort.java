package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

public interface InventoryOperationsPort {
    void importInventory(MultipartFile file, String type, String period, Long warehouseId);
    Map<String, Object> getInventoryStatus(String type);
    Map<String, Object> getImportJobStatus(String jobId);
}
