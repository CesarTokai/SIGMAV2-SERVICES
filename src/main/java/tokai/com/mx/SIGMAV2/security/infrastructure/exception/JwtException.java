package tokai.com.mx.SIGMAV2.security.infrastructure.exception;

import lombok.Getter;

/**
 * Excepción base para errores relacionados con JWT
 */
@Getter
public class JwtException extends RuntimeException {
    private final String errorCode;
    private final String details;

    public JwtException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public JwtException(String errorCode, String message) {
        this(errorCode, message, null);
    }
}
