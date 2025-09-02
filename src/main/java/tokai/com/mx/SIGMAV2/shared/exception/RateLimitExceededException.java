package tokai.com.mx.SIGMAV2.shared.exception;

/**
 * Excepción lanzada cuando se exceden los límites de rate limiting
 * para códigos de verificación u otras operaciones limitadas.
 */
public class RateLimitExceededException extends RuntimeException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
