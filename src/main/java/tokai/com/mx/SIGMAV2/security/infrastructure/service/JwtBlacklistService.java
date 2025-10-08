package tokai.com.mx.SIGMAV2.security.infrastructure.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtBlacklistService {
    // Mapa de token a fecha de expiración
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    // Agrega un token a la blacklist hasta su expiración
    public void blacklist(String token, long expirationEpochSeconds) {
        blacklist.put(token, expirationEpochSeconds);
    }

    // Verifica si el token está en la blacklist y aún no expira
    public boolean isBlacklisted(String token) {
        Long exp = blacklist.get(token);
        if (exp == null) return false;
        if (Instant.now().getEpochSecond() > exp) {
            blacklist.remove(token); // Limpieza automática
            return false;
        }
        return true;
    }
}
