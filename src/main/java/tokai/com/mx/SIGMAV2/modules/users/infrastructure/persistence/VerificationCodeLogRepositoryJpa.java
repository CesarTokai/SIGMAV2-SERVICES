package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.VerificationCodeLogRepository;

/**
 * Implementación JPA del repositorio para VerificationCodeLog
 * Simplemente extiende el puerto que ya tiene todas las queries necesarias
 */
public interface VerificationCodeLogRepositoryJpa extends VerificationCodeLogRepository {
    // Todos los métodos ya están definidos en VerificationCodeLogRepository
    // que extiende JpaRepository<VerificationCodeLog, Long>
}
