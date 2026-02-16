package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUserActivityLog;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JpaUserActivityLogRepository extends JpaRepository<BeanUserActivityLog, Long> {

    /**
     * Obtiene el historial de actividad de un usuario
     */
    Page<BeanUserActivityLog> findByUserOrderByTimestampDesc(BeanUser user, Pageable pageable);

    /**
     * Obtiene el último login de un usuario
     */
    @Query("SELECT a FROM BeanUserActivityLog a WHERE a.user = :user AND a.actionType = 'LOGIN' ORDER BY a.timestamp DESC LIMIT 1")
    BeanUserActivityLog findLastLogin(@Param("user") BeanUser user);

    /**
     * Obtiene el último bloqueo de un usuario
     */
    @Query("SELECT a FROM BeanUserActivityLog a WHERE a.user = :user AND a.actionType = 'BLOCKED' ORDER BY a.timestamp DESC LIMIT 1")
    BeanUserActivityLog findLastBlock(@Param("user") BeanUser user);

    /**
     * Obtiene intentos fallidos en los últimos X minutos
     */
    @Query("SELECT COUNT(a) FROM BeanUserActivityLog a WHERE a.user = :user AND a.actionType = 'FAILED_LOGIN' AND a.timestamp > :since")
    long countFailedLoginsSince(@Param("user") BeanUser user, @Param("since") LocalDateTime since);

    /**
     * Obtiene acciones de un usuario en un rango de fechas
     */
    Page<BeanUserActivityLog> findByUserAndTimestampBetweenOrderByTimestampDesc(
        BeanUser user, LocalDateTime start, LocalDateTime end, Pageable pageable);
}

