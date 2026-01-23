package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;

public interface InventoryQueryUseCase {
    /**
     * Obtener el stock actual de un producto en un almacén y periodo específico
     * @param productId ID del producto
     * @param warehouseId ID del almacén
     * @param periodId ID del periodo
     * @return Stock actual del producto
     */
    InventoryStock getCurrentStock(Long productId, Long warehouseId, Long periodId);
}
