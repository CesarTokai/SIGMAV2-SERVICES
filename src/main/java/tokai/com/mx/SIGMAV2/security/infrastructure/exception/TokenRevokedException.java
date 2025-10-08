package tokai.com.mx.SIGMAV2.security.infrastructure.exception;

public class TokenRevokedException extends JwtException {
    public TokenRevokedException() {
        super("TOKEN_REVOKED", "El token ha sido revocado (logout)", "El token JWT fue invalidado por cierre de sesión");
    }
}
