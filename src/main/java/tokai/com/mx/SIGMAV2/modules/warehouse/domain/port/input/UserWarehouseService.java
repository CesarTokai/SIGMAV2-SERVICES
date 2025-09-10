package tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input;

import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.AssignWarehousesDTO;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.UserWarehouseAssignment;
import java.util.List;

public interface UserWarehouseService {
    List<UserWarehouseAssignment> assignWarehouses(Long userId, AssignWarehousesDTO dto, Long assignedBy);
    void revokeWarehouse(Long userId, Long warehouseId, Long revokedBy);
    void revokeAllWarehouses(Long userId, Long revokedBy);
    List<UserWarehouseAssignment> findAssignmentsByUserId(Long userId);
    List<UserWarehouseAssignment> findAssignmentsByWarehouseId(Long warehouseId);
    boolean hasUserAccessToWarehouse(Long userId, Long warehouseId);
    boolean canRevokeWarehouse(Long userId, Long warehouseId);
    long countWarehousesByUserId(Long userId);
    long countUsersByWarehouseId(Long warehouseId);
    boolean hasUserAnyWarehouse(Long userId);
}
