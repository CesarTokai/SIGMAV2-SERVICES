package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;


import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;

import java.math.BigDecimal;

@Component
public class InventoryStockMapper {

    public InventoryStock toDomain(InventoryStockEntity entity) {
        if (entity == null) {
            return null;
        }
        return InventoryStock.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getIdProduct() : null)
                .warehouseId(entity.getWarehouse() != null ? entity.getWarehouse().getIdWarehouse() : null)
                .existQty(entity.getExistQty() != null ? new BigDecimal(entity.getExistQty()) : null)
                .status(entity.getStatus())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public InventoryStockEntity toEntity(InventoryStock domain) {
        if (domain == null) {
            return null;
        }
        InventoryStockEntity entity = new InventoryStockEntity();
        entity.setId(domain.getId());

        if (domain.getProductId() != null) {
            ProductEntity productEntity = new ProductEntity();
            productEntity.setIdProduct(domain.getProductId());
            entity.setProduct(productEntity);
        }

        if (domain.getWarehouseId() != null) {
            WarehouseEntity warehouseEntity = new WarehouseEntity();
            warehouseEntity.setIdWarehouse(domain.getWarehouseId());
            entity.setWarehouse(warehouseEntity);
        }

        if (domain.getExistQty() != null) {
            entity.setExistQty(domain.getExistQty().intValue());
        }

        entity.setStatus(domain.getStatus());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    
}
