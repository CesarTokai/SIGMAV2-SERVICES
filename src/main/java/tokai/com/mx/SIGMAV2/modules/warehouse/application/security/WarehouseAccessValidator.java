package tokai.com.mx.SIGMAV2.modules.warehouse.application.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseAccessDeniedException;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.UserWarehouseService;

@Component
@RequiredArgsConstructor
public class WarehouseAccessValidator {

    private final UserWarehouseService userWarehouseService;

    public void validateAccess(Long userId, Long warehouseId) {
        if (!userWarehouseService.hasUserAccessToWarehouse(userId, warehouseId)) {
            throw new WarehouseAccessDeniedException("El usuario no tiene acceso a este almacén");
        }
    }
}
