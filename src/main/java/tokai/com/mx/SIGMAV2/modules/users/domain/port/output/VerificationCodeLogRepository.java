package tokai.com.mx.SIGMAV2.modules.users.domain.port.output;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.VerificationCodeLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para logs de códigos de verificación
 */
@Repository
public interface VerificationCodeLogRepository extends JpaRepository<VerificationCodeLog, Long> {
    
    /**
     * Busca códigos activos por email
     */
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.email = :email AND v.status = 'ACTIVE'")
    List<VerificationCodeLog> findActiveCodesByEmail(@Param("email") String email);
    
    /**
     * Busca el último código activo por email
     */
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.email = :email AND v.status = 'ACTIVE' ORDER BY v.createdAt DESC")
    Optional<VerificationCodeLog> findLastActiveCodeByEmail(@Param("email") String email);
    
    /**
     * Cuenta reenvíos en las últimas N horas
     */
    @Query("SELECT COUNT(v) FROM VerificationCodeLog v WHERE v.email = :email AND v.createdAt >= :fromTime")
    long countResentCodesInTimeRange(@Param("email") String email, @Param("fromTime") LocalDateTime fromTime);
    
    /**
     * Busca códigos expirados para limpiar
     */
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.expiresAt < :currentTime AND v.status = 'ACTIVE'")
    List<VerificationCodeLog> findExpiredCodes(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Verifica si un código específico es válido
     */
    @Query("SELECT v FROM VerificationCodeLog v WHERE v.email = :email AND v.verificationCode = :code AND v.status = 'ACTIVE' AND v.expiresAt > :currentTime")
    Optional<VerificationCodeLog> findValidCode(@Param("email") String email, 
                                              @Param("code") String code, 
                                              @Param("currentTime") LocalDateTime currentTime);
}
