package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.output;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de catálogo de almacenes
 * que necesita el módulo MultiWarehouse.
 *
 * <p>Separa la lógica de negocio de MultiWarehouse del módulo Warehouse,
 * evitando la dependencia directa a {@code WarehouseRepository} JPA.
 */
public interface MultiWarehouseWarehousePort {

    /**
     * Busca un almacén activo (no eliminado) por su clave.
     *
     * @return Optional con el ID del almacén si existe, vacío si no
     */
    Optional<Long> findIdByWarehouseKey(String warehouseKey);

    /**
     * Busca almacenes activos por nombre exacto.
     *
     * @return lista de IDs que tienen ese nombre
     */
    List<Long> findIdsByName(String name);

    /**
     * Obtiene el nombre de un almacén dado su ID.
     */
    Optional<String> findNameById(Long warehouseId);

    /**
     * Crea un almacén nuevo y retorna su ID generado.
     *
     * @param key          clave del almacén (CVE_ALM)
     * @param name         nombre del almacén
     * @param observations observaciones (e.g. leyenda de creación automática)
     * @return ID del almacén creado
     */
    Long createWarehouse(String key, String name, String observations);

    /**
     * Cuenta el total de almacenes activos (para calcular cuántos se crearon).
     */
    long countActive();
}

