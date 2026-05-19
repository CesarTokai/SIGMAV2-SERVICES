package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.output;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Puerto de salida para operaciones de catálogo de productos e inventory_stock
 * que necesita el módulo MultiWarehouse.
 */
public interface MultiWarehouseInventoryPort {

    // -------------------------------------------------------------------------
    // Productos
    // -------------------------------------------------------------------------

    /**
     * Busca un producto por su clave de artículo.
     *
     * @return Optional con el ID del producto si existe
     */
    Optional<Long> findProductIdByCveArt(String cveArt);

    /**
     * Obtiene la descripción de un producto dado su ID.
     */
    Optional<String> findProductDescrById(Long productId);

    /**
     * Crea un producto nuevo y retorna su ID generado.
     *
     * @param cveArt      clave del artículo
     * @param description descripción del producto
     */
    Long createProduct(String cveArt, String description);

    /**
     * Cuenta el total de productos activos.
     */
    long countProducts();

    // -------------------------------------------------------------------------
    // Inventory Stock
    // -------------------------------------------------------------------------

    /**
     * Busca el stock actual de un producto en un almacén para un periodo.
     *
     * @return Optional con la cantidad si existe el registro
     */
    Optional<BigDecimal> findStock(Long productId, Long warehouseId, Long periodId);

    /**
     * Crea o actualiza el stock en la tabla inventory_stock.
     *
     * @param productId   ID del producto
     * @param warehouseId ID del almacén
     * @param periodId    ID del periodo
     * @param qty         cantidad de existencias
     * @param status      estado (A=alta, B=baja)
     */
    void upsertStock(Long productId, Long warehouseId, Long periodId, BigDecimal qty, String status);
}

