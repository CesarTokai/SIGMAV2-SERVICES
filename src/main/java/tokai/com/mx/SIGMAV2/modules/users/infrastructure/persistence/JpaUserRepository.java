package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

@Repository
public interface JpaUserRepository extends JpaRepository<BeanUser, Long> {

    Optional<BeanUser> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Método para encontrar usuario por email y código de verificación
    Optional<BeanUser> findByEmailAndVerificationCode(String email, String verificationCode);
    
    // Método para eliminar usuario por email
    @Modifying
    @Transactional
    void deleteByEmail(String email);
    
    // Método para incrementar intentos fallidos
    @Modifying
    @Transactional
    @Query("UPDATE BeanUser u SET u.attempts = u.attempts + 1, u.lastTryAt = CURRENT_TIMESTAMP WHERE u.email = :email")
    void incrementAttemptsByEmail(@Param("email") String email);
}
