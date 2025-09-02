package tokai.com.mx.SIGMAV2.security.infrastructure.exception;

/**
 * Excepción para token malformado
 */
public class TokenMalformedException extends JwtException {
    
    public TokenMalformedException(String message, String details) {
        super("TOKEN_MALFORMED", message, details);
    }

    public TokenMalformedException() {
        super("TOKEN_MALFORMED", 
              "Formato de token inválido", 
              "El token debe estar en formato: Bearer <token>");
    }
}
