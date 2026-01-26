package tokai.com.mx.SIGMAV2.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.security.infrastructure.entity.RevokedToken;

import java.time.Instant;

@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByJti(String jti);

    @Query("SELECT COUNT(r) FROM RevokedToken r WHERE r.username = :username AND r.expiresAt > :now")
    long countByUsernameAndExpiresAtAfter(@Param("username") String username, @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}

