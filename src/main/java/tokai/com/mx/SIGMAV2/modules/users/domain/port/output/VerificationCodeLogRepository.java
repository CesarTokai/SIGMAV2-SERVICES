package tokai.com.mx.SIGMAV2.modules.users.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.users.domain.model.VerificationCodeLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (output port) — define el contrato de persistencia para
 * logs de códigos de verificación.
 *
 * <p>Este puerto pertenece al dominio y NO debe importar nada de
 * Spring/JPA. Las implementaciones concretas viven en infrastructure.
 */
public interface VerificationCodeLogRepository {

    VerificationCodeLog save(VerificationCodeLog codeLog);

    Optional<VerificationCodeLog> findById(Long id);

    /** Busca códigos activos por email */
    List<VerificationCodeLog> findActiveCodesByEmail(String email);

    /** Busca el último código activo por email */
    Optional<VerificationCodeLog> findLastActiveCodeByEmail(String email);

    /** Cuenta los reenvíos realizados desde {@code fromTime} */
    long countResentCodesInTimeRange(String email, LocalDateTime fromTime);

    /** Busca códigos activos que ya expiraron */
    List<VerificationCodeLog> findExpiredCodes(LocalDateTime currentTime);

    /** Valida que el código sea activo y no haya expirado */
    Optional<VerificationCodeLog> findValidCode(String email, String code, LocalDateTime currentTime);
}
