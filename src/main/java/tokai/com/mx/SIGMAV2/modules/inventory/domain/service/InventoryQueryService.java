package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryQueryUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventorySnapshotRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventorySnapshotJpaEntity;

import java.util.List;

@Service
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


    private InventorySnapshot toDomainModel(InventorySnapshotJpaEntity entity) {
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setId(entity.getId());
        snapshot.setExistQty(entity.getExistQty());
        snapshot.setCreatedAt(entity.getCreatedAt());
        return snapshot;
    }

    private InventorySnapshot toDomainModel(InventorySnapshot entity) {
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setId(entity.getId());
        snapshot.setExistQty(entity.getExistQty());
        snapshot.setCreatedAt(entity.getCreatedAt());
        // Mapea los objetos anidados si es necesario
        // snapshot.setProduct(...);
        // snapshot.setWarehouse(...);
        // snapshot.setPeriod(...);
        return snapshot;
    }

    @Override
    public List<InventorySnapshot> getSnapshotsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        return snapshotRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId)
                .stream()
                .map(this::toDomainModel)
                .toList();
    }


    @Override
    public InventoryStock getCurrentStock(Long productId, Long warehouseId) {
        return stockRepository.findByProductAndWarehouse(productId, warehouseId)
                .orElse(null);
    }
}
