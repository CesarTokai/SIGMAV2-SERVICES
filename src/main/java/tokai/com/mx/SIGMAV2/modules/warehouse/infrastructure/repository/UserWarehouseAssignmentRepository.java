package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserWarehouseFoliosResponse;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserWarehouseSummaryResponse;
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

    /**
     * Obtiene una página de usuarios con el conteo de almacenes activos asignados
     * @param pageable Información de paginación
     * @return Página de usuarios con conteo de almacenes activos
     */
    @Query(value = "SELECT u.userId AS userId, COUNT(u.warehouseId) AS warehousesCount " +
                   "FROM UserWarehouseAssignment u " +
                   "WHERE u.isActive = true " +
                   "GROUP BY u.userId",
           countQuery = "SELECT COUNT(DISTINCT u.userId) FROM UserWarehouseAssignment u WHERE u.isActive = true")
    Page<UserWarehouseCountProjection> findUsersWithActiveWarehouses(Pageable pageable);

    /**
     * Obtiene una página de usuarios con el conteo de almacenes asignados (sin importar si están activos)
     * @param pageable Información de paginación
     * @return Página de usuarios con conteo de almacenes
     */
    @Query(value = "SELECT u.userId AS userId, COUNT(u.warehouseId) AS warehousesCount " +
                   "FROM UserWarehouseAssignment u " +
                   "GROUP BY u.userId",
           countQuery = "SELECT COUNT(DISTINCT u.userId) FROM UserWarehouseAssignment u")
    Page<UserWarehouseCountProjection> findUsersWithWarehouses(Pageable pageable);

    /**
     * OPTIMIZADO: Obtiene usuarios con almacenes en UNA SOLA QUERY con JOIN
     * Evita N+1 queries: en lugar de 1 query + N queries por usuario,
     * hace 1 query que trae todo junto
     *
     * @param pageable Información de paginación
     * @return Página de UserWarehouseSummaryResponse con todos los datos
     */
    @Query(value = """
            SELECT
                u.email                AS usuario,
                w.warehouse_key        AS claveAlmacen,
                w.name_warehouse       AS nombreAlmacen,
                MIN(l.folio)           AS primerFolio,
                MAX(l.folio)           AS ultimoFolio
            FROM labels l
            INNER JOIN users u       ON u.user_id      = l.created_by
            INNER JOIN warehouse w   ON w.id_warehouse = l.id_warehouse
            WHERE w.deleted_at IS NULL
            GROUP BY u.user_id, u.email, w.id_warehouse, w.warehouse_key, w.name_warehouse
            ORDER BY u.email ASC, w.warehouse_key ASC
            """,
           countQuery = """
            SELECT COUNT(*)
            FROM (
                SELECT l.created_by, l.id_warehouse
                FROM labels l
                INNER JOIN warehouse w ON w.id_warehouse = l.id_warehouse
                WHERE w.deleted_at IS NULL
                GROUP BY l.created_by, l.id_warehouse
            ) subq
            """,
           nativeQuery = true)
    Page<UserWarehouseSummaryResponse> findUsersWithWarehousesOptimized(Pageable pageable);

    /**
     * Muestra cada rango de marbetes generado con el usuario que lo generó,
     * clave/nombre del almacén y los folios primer/último.
     * Usa label_generation_batches como tabla base.
     *
     * @param pageable Información de paginación
     * @return Página de UserWarehouseFoliosResponse
     */
    @Query(value = """
            SELECT
                u.email                AS usuario,
                w.warehouse_key        AS claveAlmacen,
                w.name_warehouse       AS nombreAlmacen,
                lgb.primer_folio       AS primerFolio,
                lgb.ultimo_folio       AS ultimoFolio,
                lgb.generado_at        AS generadoAt
            FROM label_generation_batches lgb
            INNER JOIN users u    ON u.user_id      = lgb.generado_por
            INNER JOIN warehouse w ON w.id_warehouse = lgb.id_warehouse
            WHERE w.deleted_at IS NULL
            ORDER BY lgb.generado_at DESC
            """,
           countQuery = """
            SELECT COUNT(lgb.id_batch)
            FROM label_generation_batches lgb
            INNER JOIN warehouse w ON w.id_warehouse = lgb.id_warehouse
            WHERE w.deleted_at IS NULL
            """,
           nativeQuery = true)
    Page<UserWarehouseFoliosResponse> findUsersWarehousesWithFolios(Pageable pageable);

    interface UserWarehouseCountProjection {
        Long getUserId();
        Long getWarehousesCount();
    }
}


