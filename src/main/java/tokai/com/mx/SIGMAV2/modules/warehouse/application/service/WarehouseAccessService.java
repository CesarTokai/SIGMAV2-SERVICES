package tokai.com.mx.SIGMAV2.modules.warehouse.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.repository.UserWarehouseAssignmentRepository;

import java.util.List;

/**
 * Servicio para validar el acceso de usuarios a almacenes.
 * Implementa las reglas de negocio de contexto informativo:
 * - ADMINISTRADOR y AUXILIAR: Acceso a todos los almacenes
 * - ALMACENISTA y AUXILIAR_DE_CONTEO: Solo almacenes asignados
 */
@Service
@RequiredArgsConstructor
public class WarehouseAccessService {

    private final UserWarehouseAssignmentRepository assignmentRepository;

    // Roles con acceso total a todos los almacenes
    private static final List<String> ROLES_WITH_FULL_ACCESS = List.of("ADMINISTRADOR", "AUXILIAR");

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

        // Administradores y Auxiliares tienen acceso a todos los almacenes
        if (ROLES_WITH_FULL_ACCESS.contains(roleUpper)) {
            return;
        }

        // Para otros roles (ALMACENISTA, AUXILIAR_DE_CONTEO), verificar asignación
        boolean hasAccess = assignmentRepository.existsByUserIdAndWarehouseIdAndIsActiveTrue(userId, warehouseId);

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

        // Administradores y Auxiliares tienen acceso a todos los almacenes
        if (ROLES_WITH_FULL_ACCESS.contains(roleUpper)) {
            return null; // null indica acceso total
        }

        // Para otros roles, devolver solo los almacenes asignados
        return assignmentRepository.findWarehouseIdsByUserId(userId);
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

