package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

/**
 * Puerto de salida para consultar estado de marbetes desde el módulo de inventario.
 * Implementado por el módulo de labels (infrastructure), evitando acoplamiento directo.
 */
public interface LabelQueryPort {

    /**
     * Verifica si existen marbetes generados o impresos para un producto en un periodo.
     */
    boolean hasActiveLabelsForProduct(Long productId, Long periodId);

    /**
     * Verifica si existen conteos (C1 o C2) registrados para algún producto en un periodo.
     */
    boolean hasCountEventsForPeriod(Long periodId);

    /**
     * Cuenta marbetes activos (no cancelados) en un periodo.
     */
    long countActiveLabelsForPeriod(Long periodId);
}

