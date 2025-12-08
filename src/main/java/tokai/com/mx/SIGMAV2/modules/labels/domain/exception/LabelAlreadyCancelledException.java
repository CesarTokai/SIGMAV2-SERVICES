package tokai.com.mx.SIGMAV2.modules.labels.domain.exception;

/**
 * Excepción lanzada cuando se intenta cancelar un marbete que ya está cancelado.
 */
public class LabelAlreadyCancelledException extends RuntimeException {
    
    public LabelAlreadyCancelledException(String message) {
        super(message);
    }
    
    public LabelAlreadyCancelledException(Long folio) {
        super(String.format("El marbete con folio %d ya está cancelado", folio));
    }
}

