package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;


import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventorySnapshotRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InventorySnapshotRepositoryAdapter implements InventorySnapshotRepository {

    private final JpaInventorySnapshotRepository jpaRepository;
    private final JpaProductRepository productRepository;

    public InventorySnapshotRepositoryAdapter(
            JpaInventorySnapshotRepository jpaRepository,
            JpaProductRepository productRepository) {
        this.jpaRepository = jpaRepository;
        this.productRepository = productRepository;
    }

    public List<InventorySnapshot> findByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        if (warehouseId == null) {
            // Si no hay warehouse, buscar por periodo solamente
            List<InventorySnapshotJpaEntity> entities = jpaRepository.findByPeriodId(periodId);
            return entities.stream()
                    .map(this::toDomainModel)
                    .collect(Collectors.toList());
        }
        List<InventorySnapshotJpaEntity> entities = jpaRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId);
        return entities.stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InventorySnapshot> findByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId) {
        if (warehouseId == null) {
            return findByProductPeriod(productId, periodId);
        }
        return jpaRepository.findByProductIdAndWarehouseIdAndPeriodId(productId, warehouseId, periodId)
                .map(this::toDomainModel);
    }

    @Override
    public InventorySnapshot save(InventorySnapshot snapshot) {
        InventorySnapshotJpaEntity entity = toJpaEntity(snapshot);
        InventorySnapshotJpaEntity saved = jpaRepository.save(entity);
        return toDomainModel(saved);
    }

    @Override
    public int markAsInactiveNotInImport(Long periodId, Long warehouseId, List<Long> activeProductIds) {
        if (activeProductIds == null || activeProductIds.isEmpty()) {
            return 0;
        }
        return jpaRepository.markAsInactiveNotInImport(periodId, warehouseId, activeProductIds);
    }

    @Override
    public Optional<InventorySnapshot> findById(Long id) {
        return jpaRepository.findById(id)
                .map(this::toDomainModel);
    }

    @Override
    public Optional<InventorySnapshot> findByProductPeriod(Long productId, Long periodId) {
        // Buscar snapshot por productId y periodId (sin warehouseId)
        List<InventorySnapshotJpaEntity> entities = jpaRepository.findByPeriodId(periodId);
        return entities.stream()
                .filter(e -> e.getProductId().equals(productId))
                .findFirst()
                .map(this::toDomainModel);
    }

    // Métodos auxiliares de mapeo
    private InventorySnapshot toDomainModel(InventorySnapshotJpaEntity entity) {
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setId(entity.getId());
        snapshot.setExistQty(entity.getExistQty());
        snapshot.setCreatedAt(entity.getCreatedAt());

        // Mapear producto
        if (entity.getProductId() != null) {
            Product product = new Product();
            product.setId(entity.getProductId());
            snapshot.setProduct(product);
        }

        // Mapear periodo
        if (entity.getPeriodId() != null) {
            Period period = new Period();
            period.setId(entity.getPeriodId());
            snapshot.setPeriod(period);
        }

        // Mapear warehouse
        if (entity.getWarehouseId() != null) {
            Warehouse warehouse = new Warehouse();
            warehouse.setId(entity.getWarehouseId());
            snapshot.setWarehouse(warehouse);
        }

        return snapshot;
    }

    private InventorySnapshotJpaEntity toJpaEntity(InventorySnapshot snapshot) {
        InventorySnapshotJpaEntity entity = new InventorySnapshotJpaEntity();
        entity.setId(snapshot.getId());
        entity.setExistQty(snapshot.getExistQty());
        entity.setCreatedAt(snapshot.getCreatedAt());

        if (snapshot.getProduct() != null) {
            entity.setProductId(snapshot.getProduct().getId());
        }
        if (snapshot.getPeriod() != null) {
            entity.setPeriodId(snapshot.getPeriod().getId());
        }
        if (snapshot.getWarehouse() != null) {
            entity.setWarehouseId(snapshot.getWarehouse().getId());
        }
        // Si el producto tiene status, lo asignamos
        if (snapshot.getProduct() != null && snapshot.getProduct().getStatus() != null) {
            entity.setStatus(snapshot.getProduct().getStatus().name());
        }

        return entity;
    }
}