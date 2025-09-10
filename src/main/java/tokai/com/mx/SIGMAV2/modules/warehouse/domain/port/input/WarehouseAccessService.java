package tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input;

import java.util.Set;

public interface WarehouseAccessService {
    Set<Long> getAccessibleWarehouses();

    boolean canAccess(Long warehouseId);

}