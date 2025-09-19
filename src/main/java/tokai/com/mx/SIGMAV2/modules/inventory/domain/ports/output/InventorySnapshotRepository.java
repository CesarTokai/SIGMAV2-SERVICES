package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;

import java.util.Optional;
import java.util.Optional;
import java.util.List;

public interface InventorySnapshotRepository {
    Optional<InventorySnapshot> findByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId);
    InventorySnapshot save(InventorySnapshot s);
    int markAsInactiveNotInImport(Long periodId, Long warehouseId, List<Long> activeProductIds);
}