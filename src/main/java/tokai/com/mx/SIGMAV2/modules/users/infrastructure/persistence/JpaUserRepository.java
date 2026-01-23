package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

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
    
    // ============ MÉTODOS ADMINISTRATIVOS ============
    
    /**
     * Busca usuarios por criterios dinámicos
     */
    @Query("SELECT u FROM BeanUser u WHERE " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:verified IS NULL OR u.isVerified = :verified) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<BeanUser> findByCriteria(@Param("email") String email, 
                                  @Param("role") ERole role,
                                  @Param("verified") Boolean verified,
                                  @Param("status") Boolean status, 
                                  Pageable pageable);
    
    /**
     * Cuenta usuarios por estado de verificación
     */
    long countByIsVerified(boolean isVerified);
    
    /**
     * Cuenta usuarios por status
     */
    long countByStatus(boolean status);
    
    /**
     * Encuentra usuarios no verificados más antiguos que la fecha especificada
     */
    @Query("SELECT u FROM BeanUser u WHERE u.isVerified = false AND u.createdAt < :cutoffDate")
    List<BeanUser> findUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Encuentra usuarios por rol
     */
    List<BeanUser> findByRole(ERole role);

    /**
     * Elimina usuarios no verificados más antiguos que la fecha especificada
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM BeanUser u WHERE u.isVerified = false AND u.createdAt < :cutoffDate")
    int deleteUnverifiedUsersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Encuentra usuarios creados entre fechas
     */
    List<BeanUser> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
