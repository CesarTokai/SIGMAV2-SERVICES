package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;

import java.util.Optional;

public interface JpaInventoryStockRepository extends JpaRepository<InventoryStockEntity, Long> {
    Optional<InventoryStockEntity> findByProductIdProductAndWarehouseIdWarehouse(Long productId, Long warehouseId);
}