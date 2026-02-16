package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanPasswordResetAttempt;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JpaPasswordResetAttemptRepository extends JpaRepository<BeanPasswordResetAttempt, Long> {

    /**
     * Contar intentos fallidos de validación de código en los últimos X minutos
     */
    @Query("SELECT COUNT(a) FROM BeanPasswordResetAttempt a " +
           "WHERE a.user = :user " +
           "AND a.attemptType = 'CODE_VALIDATION' " +
           "AND a.isSuccessful = false " +
           "AND a.attemptAt > :since")
    long countFailedCodeAttemptsFromUser(@Param("user") BeanUser user, @Param("since") LocalDateTime since);

    /**
     * Obtener intentos fallidos recientes de un usuario
     */
    List<BeanPasswordResetAttempt> findByUserAndAttemptTypeAndIsSuccessfulOrderByAttemptAtDesc(
        BeanUser user, String attemptType, boolean isSuccessful);

    /**
     * Limpiar intentos más antiguos que una fecha
     */
    long deleteByAttemptAtBefore(LocalDateTime cutoffDate);
}

