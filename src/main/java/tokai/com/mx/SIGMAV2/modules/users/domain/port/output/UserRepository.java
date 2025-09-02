package tokai.com.mx.SIGMAV2.modules.users.domain.port.output;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida - Define el contrato para persistencia de usuarios
 * Esta interfaz NO debe tener anotaciones de framework
 * Solo usa tipos de dominio (User), nunca tipos de infraestructura (BeanUser)
 */
public interface UserRepository {

    /**
     * Guarda un usuario
     */
    User save(User user);

    /**
     * Busca un usuario por email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Busca un usuario por ID
     */
    Optional<User> findById(Long id);

    /**
     * Verifica si existe un usuario por email
     */
    boolean existsByEmail(String email);

    /**
     * Elimina un usuario por email
     */
    void deleteByEmail(String email);

    /**
     * Busca un usuario por email y código de verificación
     */
    Optional<User> verifyByEmailAndCode(String email, String code);

    /**
     * Incrementa los intentos fallidos de un usuario
     */
    void incrementAttempts(String email);
    
    // ============ MÉTODOS ADMINISTRATIVOS ============
    
    /**
     * Encuentra todos los usuarios con paginación
     */
    Page<User> findAll(Pageable pageable);
    
    /**
     * Busca usuarios por criterios con paginación
     */
    Page<User> findByCriteria(String email, Role role, Boolean verified, Boolean status, Pageable pageable);
    
    /**
     * Cuenta usuarios por estado de verificación
     */
    long countByVerified(boolean verified);
    
    /**
     * Cuenta usuarios por status
     */
    long countByStatus(boolean status);
    
    /**
     * Encuentra usuarios no verificados más antiguos que X días
     */
    List<User> findUnverifiedUsersOlderThan(LocalDateTime cutoffDate);
    
    /**
     * Encuentra usuarios por rol
     */
    List<User> findByRole(Role role);
    
    /**
     * Elimina usuarios no verificados más antiguos que X días
     */
    int deleteUnverifiedUsersOlderThan(LocalDateTime cutoffDate);
    
    /**
     * Cuenta total de usuarios
     */
    long count();
    
    /**
     * Encuentra usuarios creados entre fechas
     */
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
