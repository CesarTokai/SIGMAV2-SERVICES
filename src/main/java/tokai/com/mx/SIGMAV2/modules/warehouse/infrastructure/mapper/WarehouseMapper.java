package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseEntity;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "warehouseKey", source = "warehouseKey"),
        @Mapping(target = "nameWarehouse", source = "nameWarehouse"),
        @Mapping(target = "observations", source = "observations"),
        @Mapping(target = "createdAt", source = "createdAt"),
        @Mapping(target = "updatedAt", source = "updatedAt"),
        @Mapping(target = "deletedAt", source = "deletedAt"),
        @Mapping(target = "createdBy", source = "createdBy"),
        @Mapping(target = "updatedBy", source = "updatedBy"),
        @Mapping(target = "deletedBy", source = "deletedBy"),
        @Mapping(target = "assignedUsersCount", expression = "java(entity.getUserAssignments() != null ? (long) entity.getUserAssignments().size() : 0L)"),
        @Mapping(target = "deleted", expression = "java(entity.getDeletedAt() != null)")
    })
    Warehouse toDomain(WarehouseEntity entity);

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "warehouseKey", source = "warehouseKey"),
        @Mapping(target = "nameWarehouse", source = "nameWarehouse"),
        @Mapping(target = "observations", source = "observations"),
        @Mapping(target = "createdAt", source = "createdAt"),
        @Mapping(target = "updatedAt", source = "updatedAt"),
        @Mapping(target = "deletedAt", source = "deletedAt"),
        @Mapping(target = "createdBy", source = "createdBy"),
        @Mapping(target = "updatedBy", source = "updatedBy"),
        @Mapping(target = "deletedBy", source = "deletedBy"),
        // userAssignments is managed elsewhere; do not map from domain count
        @Mapping(target = "userAssignments", ignore = true)
    })
    WarehouseEntity toEntity(Warehouse domain);
}
