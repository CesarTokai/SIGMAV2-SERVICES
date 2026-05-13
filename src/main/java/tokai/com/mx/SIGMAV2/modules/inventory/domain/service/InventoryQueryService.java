package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryQueryUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryStockRepository;

@Service
@RequiredArgsConstructor
public class InventoryQueryService implements InventoryQueryUseCase {

    private final InventoryStockRepository stockRepository;

    @Override
    public InventoryStock getCurrentStock(Long productId, Long warehouseId, Long periodId) {
        return stockRepository.findByProductAndWarehouseAndPeriod(productId, warehouseId, periodId)
                .orElse(null);
    }
}
