package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.UserWarehouseAssignment;

import java.util.List;

/**
 * Repositorio para gestionar las asignaciones de usuarios a almacenes.
 * Soporta las reglas de negocio de contexto informativo del módulo de Marbetes.
 */
@Repository
public interface UserWarehouseAssignmentRepository extends JpaRepository<UserWarehouseAssignment, UserWarehouseAssignment.UserWarehouseId> {

    /**
     * Verifica si un usuario tiene acceso a un almacén específico
     * @param userId ID del usuario
     * @param warehouseId ID del almacén
     * @return true si el usuario tiene acceso al almacén
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM UserWarehouseAssignment u " +
           "WHERE u.userId = :userId AND u.warehouseId = :warehouseId AND u.isActive = true")
    boolean existsByUserIdAndWarehouseIdAndIsActiveTrue(@Param("userId") Long userId, @Param("warehouseId") Long warehouseId);

    /**
     * Obtiene todos los almacenes asignados a un usuario
     * @param userId ID del usuario
     * @return Lista de IDs de almacenes asignados
     */
    @Query("SELECT u.warehouseId FROM UserWarehouseAssignment u " +
           "WHERE u.userId = :userId AND u.isActive = true")
    List<Long> findWarehouseIdsByUserId(@Param("userId") Long userId);

    /**
     * Obtiene todos los usuarios asignados a un almacén
     * @param warehouseId ID del almacén
     * @return Lista de IDs de usuarios asignados
     */
    @Query("SELECT u.userId FROM UserWarehouseAssignment u " +
           "WHERE u.warehouseId = :warehouseId AND u.isActive = true")
    List<Long> findUserIdsByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * Obtiene todas las asignaciones activas de un usuario
     * @param userId ID del usuario
     * @return Lista de asignaciones
     */
    List<UserWarehouseAssignment> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * Obtiene todas las asignaciones activas de un almacén
     * @param warehouseId ID del almacén
     * @return Lista de asignaciones
     */
    List<UserWarehouseAssignment> findByWarehouseIdAndIsActiveTrue(Long warehouseId);
}

