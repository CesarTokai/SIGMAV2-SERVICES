package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO para solicitar impresión de marbetes.
 * Dos modos de operación:
 * 1. Automático: Solo especificar periodId y warehouseId (imprime todos los pendientes)
 * 2. Selectivo: Especificar lista de folios específicos para reimprimir
 */
public class PrintRequestDTO {

    @NotNull
    private Long periodId;

    @NotNull
    private Long warehouseId;

    // Opcional: Lista específica de folios a imprimir (para reimpresión)
    private List<Long> folios;

    // Opcional: Imprimir solo marbetes de cierto producto
    private Long productId;

    // Flag para forzar reimpresión de marbetes ya impresos
    private Boolean forceReprint = false;

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public List<Long> getFolios() { return folios; }
    public void setFolios(List<Long> folios) { this.folios = folios; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Boolean getForceReprint() { return forceReprint; }
    public void setForceReprint(Boolean forceReprint) { this.forceReprint = forceReprint; }
}

