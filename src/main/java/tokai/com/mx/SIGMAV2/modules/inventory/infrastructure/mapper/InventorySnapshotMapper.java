package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventorySnapshot;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventorySnapshotJpaEntity;

@Component
public class InventorySnapshotMapper {

    public InventorySnapshot toDomain(InventorySnapshotJpaEntity entity) {
        if (entity == null) return null;

        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setId(entity.getId());
        snapshot.setExistQty(entity.getExistQty());
        snapshot.setCreatedAt(entity.getCreatedAt());

        if (entity.getProductId() != null) {
            Product product = new Product();
            product.setId(entity.getProductId());
            snapshot.setProduct(product);
        }

        if (entity.getWarehouseId() != null) {
            Warehouse warehouse = new Warehouse();
            warehouse.setId(entity.getWarehouseId());
            snapshot.setWarehouse(warehouse);
        }

        if (entity.getPeriodId() != null) {
            Period period = new Period();
            period.setId(entity.getPeriodId());
            snapshot.setPeriod(period);
        }

        return snapshot;
    }

    public InventorySnapshotJpaEntity toEntity(InventorySnapshot domain) {
        if (domain == null) return null;

        InventorySnapshotJpaEntity entity = new InventorySnapshotJpaEntity();
        entity.setId(domain.getId());
        entity.setExistQty(domain.getExistQty());
        entity.setCreatedAt(domain.getCreatedAt());

        if (domain.getProduct() != null) {
            entity.setProductId(domain.getProduct().getId());
        }
        if (domain.getWarehouse() != null) {
            entity.setWarehouseId(domain.getWarehouse().getId());
        }
        if (domain.getPeriod() != null) {
            entity.setPeriodId(domain.getPeriod().getId());
        }

        return entity;
    }
}
