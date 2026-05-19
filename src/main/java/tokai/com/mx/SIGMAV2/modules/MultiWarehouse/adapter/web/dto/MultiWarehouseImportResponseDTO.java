package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;

import java.util.List;

/**
 * Respuesta enriquecida de la importación de multialmacén.
 * Incluye el log de importación + warnings + detalle de productos dados de baja.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiWarehouseImportResponseDTO {

    /** Log de la importación (igual que antes) */
    private MultiWarehouseImportLog importLog;

    /** true si hubo productos marcados como baja automáticamente */
    private boolean tieneWarnings;

    /**
     * Mensaje de advertencia visible cuando tieneWarnings = true.
     * Ejemplo: "⚠️ 3 productos fueron marcados como BAJA porque no aparecieron en el Excel."
     */
    private String mensajeWarning;

    /**
     * Lista detallada de los productos marcados como BAJA automáticamente.
     * Vacía cuando tieneWarnings = false.
     * El frontend puede mostrar esta lista en una pestaña/modal separado.
     */
    private List<ProductoBajaDTO> productosDadosDeBaja;

    /** Totales del proceso */
    private int totalProcesados;
    private int totalActualizados;
    private int totalCreados;
    private int totalAlmacenesCreados;
    private int totalProductosCreados;
    private int totalDadosDeBaja;
}

