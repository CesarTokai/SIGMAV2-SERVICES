package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Resultado de un paso del wizard de importación multialmacén.
 *
 * <p>Usa un mapa genérico para los datos del paso porque cada step
 * retorna una estructura diferente. El adaptador web serializa esto a JSON.
 *
 * <p>Cuando {@code valid = false}, el adaptador debe responder con 400 Bad Request
 * o 409 Conflict según el código de error.
 */
@Value
@Builder
public class WizardStepResult {

    public enum ErrorCode { INVALID_INPUT, PERIOD_LOCKED, PERIOD_CLOSED, STEP_NOT_SUPPORTED }

    /** Número del paso procesado. */
    int stepNumber;

    /** true si el paso fue válido y puede continuar. */
    boolean valid;

    /** Código de error cuando valid = false. */
    ErrorCode errorCode;

    /** Mensaje descriptivo (éxito o error). */
    String message;

    /**
     * Datos específicos del paso (e.g. lista de almacenes activos, total de bajas...).
     * null cuando valid = false.
     */
    Map<String, Object> data;
}

