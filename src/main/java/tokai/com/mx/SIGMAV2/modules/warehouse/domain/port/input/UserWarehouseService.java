package tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input;

import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.AssignWarehousesDTO;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.UserWarehouseAssignment;

import java.util.List;

public interface UserWarehouseService {

    // Asignación y revocación
    List<UserWarehouseAssignment> assignWarehouses(Long userId, AssignWarehousesDTO dto, Long assignedBy);
    
    void revokeWarehouse(Long userId, Long warehouseId, Long revokedBy);
    
    void revokeAllWarehouses(Long userId, Long revokedBy);
    
    // Consultas
    List<UserWarehouseAssignment> findAssignmentsByUserId(Long userId);
    
    List<UserWarehouseAssignment> findAssignmentsByWarehouseId(Long warehouseId);
    
    // Validaciones
    boolean hasUserAccessToWarehouse(Long userId, Long warehouseId);
    
    boolean canRevokeWarehouse(Long userId, Long warehouseId);
    
    long countWarehousesByUserId(Long userId);
    
    long countUsersByWarehouseId(Long warehouseId);
    
    // Verificar si el usuario tiene al menos un almacén
    boolean hasUserAnyWarehouse(Long userId);
}