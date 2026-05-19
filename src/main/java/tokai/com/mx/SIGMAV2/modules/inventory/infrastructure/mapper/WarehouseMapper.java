package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;

@Component
public class WarehouseMapper {
    public Warehouse toDomain(WarehouseEntity entity) {
        if (entity == null) return null;
        Warehouse warehouse = new Warehouse();
        warehouse.setId(entity.getIdWarehouse());
        warehouse.setName(entity.getNameWarehouse());
        // Si necesitas mapear más campos, agrégalos aquí y en el modelo
        return warehouse;
    }

    public WarehouseEntity toEntity(Warehouse domain) {
        if (domain == null) return null;
        WarehouseEntity entity = new WarehouseEntity();
        entity.setIdWarehouse(domain.getId());
        entity.setNameWarehouse(domain.getName());
        // Si necesitas mapear más campos, agrégalos aquí y en el modelo
        return entity;
    }
}
