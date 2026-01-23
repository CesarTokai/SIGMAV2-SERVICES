package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventorySnapshotJpaEntity;



@Component
public class InventorySnapshotMapper {
    // Java
    public InventorySnapshot toDomain(InventorySnapshotJpaEntity entity) {
        if (entity == null) return null;
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setId(entity.getId());
        snapshot.setProductId(entity.getProductId());
        snapshot.setWarehouseId(entity.getWarehouseId());
        snapshot.setPeriodId(entity.getPeriodId());
        snapshot.setExistQty(entity.getExistQty());
        snapshot.setCreatedAt(entity.getCreatedAt());
        return snapshot;
    }

    public InventorySnapshotJpaEntity toEntity(InventorySnapshot domain) {
        if (domain == null) return null;
        InventorySnapshotJpaEntity entity = new InventorySnapshotJpaEntity();
        entity.setId(domain.getId());
        entity.setProductId(domain.getProductId());
        entity.setWarehouseId(domain.getWarehouseId());
        entity.setPeriodId(domain.getPeriodId());
        entity.setExistQty(domain.getExistQty());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

}