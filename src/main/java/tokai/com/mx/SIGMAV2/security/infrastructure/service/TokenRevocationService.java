package tokai.com.mx.SIGMAV2.security.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.security.infrastructure.entity.RevokedToken;
import tokai.com.mx.SIGMAV2.security.infrastructure.repository.RevokedTokenRepository;

import java.time.Instant;

/**
 * Servicio para manejar la revocación de tokens JWT
 * Persiste tokens revocados en base de datos y proporciona verificación
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    /**
     * Revoca un token guardándolo en la base de datos
     * @param jti ID único del JWT
     * @param expiresAt Fecha de expiración del token
     * @param reason Razón de la revocación (LOGOUT, SECURITY_BREACH, etc.)
     * @param username Usuario asociado (opcional)
     */
    @Transactional
    public void revokeToken(String jti, Instant expiresAt, String reason, String username) {
        if (jti == null || jti.isBlank()) {
            log.warn("Intento de revocar token sin JTI");
            return;
        }

        if (revokedTokenRepository.existsByJti(jti)) {
            log.debug("Token ya estaba revocado: jti={}", jti);
            return;
        }

        RevokedToken revoked = new RevokedToken();
        revoked.setJti(jti);
        revoked.setRevokedAt(Instant.now());
        revoked.setExpiresAt(expiresAt);
        revoked.setReason(reason);
        revoked.setUsername(username);

        revokedTokenRepository.save(revoked);
        log.info("Token revocado exitosamente: jti={}, reason={}, username={}", jti, reason, username);
    }

    /**
     * Verifica si un token está revocado
     * @param jti ID único del JWT
     * @return true si el token está revocado, false en caso contrario
     */
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return revokedTokenRepository.existsByJti(jti);
    }

    /**
     * Purga automática de tokens expirados de la base de datos
     * Se ejecuta según el intervalo configurado (por defecto cada hora)
     */
    @Scheduled(fixedDelayString = "${security.revocation.purge-interval-ms:3600000}")
    @Transactional
    public void purgeExpiredTokens() {
        try {
            int deleted = revokedTokenRepository.deleteExpired(Instant.now());
            if (deleted > 0) {
                log.info("Purgados {} tokens expirados de la lista de revocación", deleted);
            }
        } catch (Exception e) {
            log.error("Error al purgar tokens expirados: {}", e.getMessage(), e);
        }
    }
}

