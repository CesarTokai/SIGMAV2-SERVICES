package tokai.com.mx.SIGMAV2.modules.warehouse.application.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception.WarehouseAccessDeniedException;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.UserWarehouseService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseAccessValidator {

    private final UserWarehouseService userWarehouseService;

    /**
     * Valida que el usuario tenga acceso al almacén especificado
     * @param userId ID del usuario
     * @param warehouseId ID del almacén
     * @throws WarehouseAccessDeniedException si no tiene acceso
     */
    public void validateAccess(Long userId, Long warehouseId) {
        log.debug("Validando acceso de usuario {} a almacén {}", userId, warehouseId);
        
        if (!userWarehouseService.hasUserAccessToWarehouse(userId, warehouseId)) {
            log.warn("Acceso denegado: usuario {} no tiene acceso a almacén {}", userId, warehouseId);
            throw new WarehouseAccessDeniedException(userId, warehouseId);
        }
        
        log.debug("Acceso validado correctamente: usuario {} tiene acceso a almacén {}", userId, warehouseId);
    }

    /**
     * Verifica si el usuario tiene acceso al almacén
     * @param userId ID del usuario
     * @param warehouseId ID del almacén
     * @return true si tiene acceso, false en caso contrario
     */
    public boolean hasAccess(Long userId, Long warehouseId) {
        return userWarehouseService.hasUserAccessToWarehouse(userId, warehouseId);
    }

    /**
     * Valida que el usuario tenga al menos un almacén asignado
     * @param userId ID del usuario
     * @throws WarehouseAccessDeniedException si no tiene almacenes asignados
     */
    public void validateHasAnyWarehouse(Long userId) {
        log.debug("Validando que usuario {} tenga al menos un almacén", userId);
        
        if (!userWarehouseService.hasUserAnyWarehouse(userId)) {
            log.warn("Usuario {} no tiene almacenes asignados", userId);
            throw new WarehouseAccessDeniedException("No tienes almacenes asignados. Contacta al administrador.");
        }
        
        log.debug("Usuario {} tiene almacenes asignados", userId);
    }
}