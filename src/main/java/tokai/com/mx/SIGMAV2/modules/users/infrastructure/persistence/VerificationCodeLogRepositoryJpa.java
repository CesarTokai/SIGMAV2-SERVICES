package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.VerificationCodeLog;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.VerificationCodeLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JPA del puerto VerificationCodeLogRepository.
 * Extiende JpaRepository para obtener las operaciones CRUD básicas
 * e implementa el puerto de dominio con las queries específicas.
 *
 * <p>Al ser la ÚNICA clase que implementa el puerto, Spring la inyectará
 * sin ambigüedad cuando se solicite VerificationCodeLogRepository.
 */
@Repository
public interface VerificationCodeLogRepositoryJpa
        extends JpaRepository<VerificationCodeLog, Long>, VerificationCodeLogRepository {

    @Override
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.email = :email AND v.status = 'ACTIVE'")
    List<VerificationCodeLog> findActiveCodesByEmail(@Param("email") String email);

    @Override
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.email = :email AND v.status = 'ACTIVE' ORDER BY v.createdAt DESC")
    Optional<VerificationCodeLog> findLastActiveCodeByEmail(@Param("email") String email);

    @Override
    @Query("SELECT COUNT(v) FROM VerificationCodeLog v WHERE v.email = :email AND v.createdAt >= :fromTime")
    long countResentCodesInTimeRange(@Param("email") String email, @Param("fromTime") LocalDateTime fromTime);

    @Override
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.expiresAt < :currentTime AND v.status = 'ACTIVE'")
    List<VerificationCodeLog> findExpiredCodes(@Param("currentTime") LocalDateTime currentTime);

    @Override
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.email = :email AND v.verificationCode = :code AND v.status = 'ACTIVE' AND v.expiresAt > :currentTime")
    Optional<VerificationCodeLog> findValidCode(@Param("email") String email,
                                                @Param("code") String code,
                                                @Param("currentTime") LocalDateTime currentTime);
}
