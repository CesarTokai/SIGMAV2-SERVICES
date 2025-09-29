package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;


import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventorySnapshotRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class InventorySnapshotRepositoryAdapter implements InventorySnapshotRepository {

    private final JpaInventorySnapshotRepository jpaRepository;

    public InventorySnapshotRepositoryAdapter(JpaInventorySnapshotRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<InventorySnapshot> findByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        // Implementa la lógica usando jpaRepository
        return List.of();
    }

    @Override
    public Optional<InventorySnapshot> findByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId) {
        return Optional.empty();
    }

    @Override
    public InventorySnapshot save(InventorySnapshot snapshot) {
        return null;
    }

    @Override
    public int markAsInactiveNotInImport(Long periodId, Long warehouseId, List<Long> activeProductIds) {
        return 0;
    }

    @Override
    public Optional<InventorySnapshot> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public List<InventorySnapshot> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId) {
        return List.of();
    }
}