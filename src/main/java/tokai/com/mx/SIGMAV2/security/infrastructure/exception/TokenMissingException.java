package tokai.com.mx.SIGMAV2.security.infrastructure.exception;

/**
 * Excepción para token faltante
 */
public class TokenMissingException extends JwtException {
    
    public TokenMissingException(String message, String details) {
        super("TOKEN_MISSING", message, details);
    }

    public TokenMissingException() {
        super("TOKEN_MISSING", 
              "Token de autenticación requerido", 
              "Debe proporcionar un token válido en el header Authorization");
    }
}
