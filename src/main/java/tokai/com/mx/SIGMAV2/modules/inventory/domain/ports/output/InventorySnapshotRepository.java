package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;

import java.util.List;
import java.util.Optional;

public interface InventorySnapshotRepository {
    List<InventorySnapshot> findByPeriodAndWarehouse(Long periodId, Long warehouseId);
    Optional<InventorySnapshot> findByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId);
    InventorySnapshot save(InventorySnapshot snapshot);
    int markAsInactiveNotInImport(Long periodId, Long warehouseId, List<Long> activeProductIds);
    Optional<InventorySnapshot> findById(Long id);

    Optional<InventorySnapshot> findByProductPeriod(Long id, Long id1);
}
