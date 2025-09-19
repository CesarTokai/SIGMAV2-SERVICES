package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryQueryUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryStockRepository;

import java.util.List;


public class InventoryQueryService implements InventoryQueryUseCase {

    private final InventorySnapshotRepository snapshotRepository;
    private final InventoryStockRepository stockRepository;

    public InventoryQueryService(
            InventorySnapshotRepository snapshotRepository,
            InventoryStockRepository stockRepository
    ) {
        this.snapshotRepository = snapshotRepository;
        this.stockRepository = stockRepository;
    }

    @Override
    public List<InventorySnapshot> getSnapshotsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        return snapshotRepository.findByPeriodAndWarehouse(periodId, warehouseId);
    }

    @Override
    public InventoryStock getCurrentStock(Long productId, Long warehouseId) {
        return stockRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElse(null);
    }
}