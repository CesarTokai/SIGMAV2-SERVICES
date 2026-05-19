package tokai.com.mx.SIGMAV2.modules.warehouse.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.UserWarehouseRepository;

import java.util.List;

/**
 * Servicio para validar el acceso de usuarios a almacenes.
 * Implementa las reglas de negocio de contexto informativo:
 * - ADMINISTRADOR, AUXILIAR y AUXILIAR_DE_CONTEO: Acceso a todos los almacenes (sin restricción)
 * - ALMACENISTA: Solo almacenes asignados
 */
@Service
@RequiredArgsConstructor
public class WarehouseAccessService {

    private final UserWarehouseRepository userWarehouseRepository;

    // Roles con acceso total a todos los almacenes (sin necesidad de asignación manual)
    private static final List<String> ROLES_WITH_FULL_ACCESS = List.of("ADMINISTRADOR", "AUXILIAR", "AUXILIAR_DE_CONTEO");

    /**
     * Valida que un usuario tenga acceso a un almacén específico
     * @param userId ID del usuario
     * @param warehouseId ID del almacén
     * @param userRole Rol del usuario
     * @throws PermissionDeniedException si el usuario no tiene acceso
     */
    public void validateWarehouseAccess(Long userId, Long warehouseId, String userRole) {
        if (userRole == null) {
            throw new PermissionDeniedException("El rol del usuario es requerido");
        }

        String roleUpper = userRole.toUpperCase();

        // ADMINISTRADOR, AUXILIAR, AUXILIAR_DE_CONTEO: Acceso a todos los almacenes sin validación
        if (ROLES_WITH_FULL_ACCESS.contains(roleUpper)) {
            return;
        }

        // ALMACENISTA: Verificar asignación en user_warehouses
        boolean hasAccess = userWarehouseRepository.existsByUserIdAndWarehouseIdAndWarehouseDeletedAtIsNull(userId, warehouseId);

        if (!hasAccess) {
            throw new PermissionDeniedException(
                String.format("El usuario no tiene acceso al almacén %d", warehouseId)
            );
        }
    }

    /**
     * Obtiene los IDs de todos los almacenes accesibles por un usuario
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return Lista de IDs de almacenes (null si tiene acceso total)
     */
    public List<Long> getAccessibleWarehouses(Long userId, String userRole) {
        if (userRole == null) {
            throw new IllegalArgumentException("El rol del usuario es requerido");
        }

        String roleUpper = userRole.toUpperCase();

        // ADMINISTRADOR, AUXILIAR, AUXILIAR_DE_CONTEO: Acceso a todos los almacenes
        if (ROLES_WITH_FULL_ACCESS.contains(roleUpper)) {
            return null; // null indica acceso total
        }

        // ALMACENISTA: Devolver solo los almacenes asignados activos
        return userWarehouseRepository.findByUserIdWithActiveWarehouses(userId)
                .stream()
                .map(uw -> uw.getWarehouse().getId())
                .toList();
    }

    /**
     * Verifica si un usuario tiene acceso total (todos los almacenes)
     * @param userRole Rol del usuario
     * @return true si tiene acceso total
     */
    public boolean hasFullAccess(String userRole) {
        if (userRole == null) {
            return false;
        }
        return ROLES_WITH_FULL_ACCESS.contains(userRole.toUpperCase());
    }
}

