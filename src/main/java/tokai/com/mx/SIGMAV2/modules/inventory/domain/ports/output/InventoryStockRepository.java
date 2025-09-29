package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import java.util.Optional;

public interface InventoryStockRepository {
    Optional<InventoryStock> findByProductAndWarehouse(Long productId, Long warehouseId);
    InventoryStock save(InventoryStock stock);
}
