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
                .periodId(entity.getPeriodId())
                .existQty(entity.getExistQty() != null ? entity.getExistQty() : BigDecimal.ZERO)
                .status(entity.getStatus() != null ? entity.getStatus().name() : InventoryStock.STATUS_ACTIVE)
                .createdAt(entity.getCreatedAt())
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

        entity.setPeriodId(domain.getPeriodId());
        entity.setExistQty(domain.getExistQty() != null ? domain.getExistQty() : BigDecimal.ZERO);

        // Convertir String a Enum
        if (domain.getStatus() != null) {
            entity.setStatus(InventoryStockEntity.Status.valueOf(domain.getStatus()));
        } else {
            entity.setStatus(InventoryStockEntity.Status.A);
        }

        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    
}
