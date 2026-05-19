package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result;

import lombok.Builder;
import lombok.Value;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.ProductoBajaDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;

import java.util.List;

/**
 * Resultado del caso de uso de importación de multialmacén.
 *
 * <p>Objeto inmutable (value object) que encapsula todo lo que el adaptador
 * web necesita para construir la {@code ResponseEntity} apropiada.
 * No contiene ninguna clase del framework HTTP.
 *
 * <p>Estados posibles:
 * <ul>
 *   <li>{@code SUCCESS} — importación completada sin novedades</li>
 *   <li>{@code SUCCESS_WITH_WARNINGS} — completada, pero hay productos dados de baja</li>
 *   <li>{@code DUPLICATE} — el archivo ya fue importado previamente (idempotencia por SHA-256)</li>
 *   <li>{@code ERROR} — la importación falló; {@code errorMessage} contiene el detalle</li>
 * </ul>
 */
@Value
@Builder
public class ImportResult {

    public enum Status { SUCCESS, SUCCESS_WITH_WARNINGS, DUPLICATE, ERROR }

    /** Estado final del proceso. */
    Status status;

    /** Log persistido en BD (siempre presente, incluso en DUPLICATE). */
    MultiWarehouseImportLog importLog;

    /** Mensaje de advertencia cuando status = SUCCESS_WITH_WARNINGS. */
    String warningMessage;

    /** Detalle de productos dados de baja automáticamente. */
    List<ProductoBajaDTO> productosDadosDeBaja;

    /** Mensaje de error cuando status = ERROR. */
    String errorMessage;

    // Totales del proceso
    int totalProcesados;
    int totalActualizados;
    int totalCreados;
    int totalAlmacenesCreados;
    int totalProductosCreados;
    int totalDadosDeBaja;

    /** @return true si la importación terminó con éxito (con o sin warnings) */
    public boolean isSuccessful() {
        return status == Status.SUCCESS || status == Status.SUCCESS_WITH_WARNINGS || status == Status.DUPLICATE;
    }

    /** @return true si hay productos dados de baja que mostrar al usuario */
    public boolean hasWarnings() {
        return status == Status.SUCCESS_WITH_WARNINGS;
    }
}

