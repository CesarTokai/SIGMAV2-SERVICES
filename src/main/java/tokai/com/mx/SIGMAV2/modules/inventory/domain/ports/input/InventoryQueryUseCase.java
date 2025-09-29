package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import java.util.List;

public interface InventoryQueryUseCase {
    List<InventorySnapshot> getSnapshotsByPeriodAndWarehouse(Long periodId, Long warehouseId);
    InventoryStock getCurrentStock(Long productId, Long warehouseId);
}
