package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;

import java.util.List;

public abstract class InventoryQueryUseCase {
    public abstract List<InventorySnapshot> getSnapshotsByPeriodAndWarehouse(Long periodId, Long warehouseId);
}
