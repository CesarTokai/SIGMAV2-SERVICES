package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.adapter;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper.InventoryStockMapper;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;

import java.util.Optional;

@Component
public class InventoryStockRepositoryAdapter implements InventoryStockRepository {

    private final JpaInventoryStockRepository jpaRepository;
    private final InventoryStockMapper mapper;

    public InventoryStockRepositoryAdapter(JpaInventoryStockRepository jpaRepository, InventoryStockMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<InventoryStock> findByProductAndWarehouse(Long productId, Long warehouseId) {
        return jpaRepository.findByProductIdProductAndWarehouseIdWarehouse(productId, warehouseId)
                .map(mapper::toDomain);
    }

    @Override
    public InventoryStock save(InventoryStock stock) {
        InventoryStockEntity entity = mapper.toEntity(stock);
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
