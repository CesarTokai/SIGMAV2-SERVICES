package tokai.com.mx.SIGMAV2.security.infrastructure.exception;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Excepción para token expirado
 */
@Getter
public class TokenExpiredException extends JwtException {
    private final LocalDateTime expiredAt;

    public TokenExpiredException(String message, LocalDateTime expiredAt) {
        super("TOKEN_EXPIRED", message, "Su sesión ha expirado, por favor inicie sesión nuevamente");
        this.expiredAt = expiredAt;
    }

    public TokenExpiredException(LocalDateTime expiredAt) {
        this("Token expirado", expiredAt);
    }
}
