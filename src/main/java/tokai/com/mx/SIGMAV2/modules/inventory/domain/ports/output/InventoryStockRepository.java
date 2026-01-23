package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import java.util.Optional;

public interface InventoryStockRepository {
    /**
     * Buscar existencias por producto, almacén y periodo
     * @param productId ID del producto
     * @param warehouseId ID del almacén
     * @param periodId ID del periodo
     * @return Optional con el stock encontrado
     */
    Optional<InventoryStock> findByProductAndWarehouseAndPeriod(Long productId, Long warehouseId, Long periodId);

    InventoryStock save(InventoryStock stock);

}
