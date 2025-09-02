package tokai.com.mx.SIGMAV2.security.infrastructure.exception;

/**
 * Excepción para token inválido
 */
public class TokenInvalidException extends JwtException {
    
    public TokenInvalidException(String message, String details) {
        super("TOKEN_INVALID", message, details);
    }

    public TokenInvalidException(String message) {
        super("TOKEN_INVALID", message, "El token proporcionado no es válido o ha sido modificado");
    }
    
    public TokenInvalidException() {
        this("Token inválido");
    }
}
