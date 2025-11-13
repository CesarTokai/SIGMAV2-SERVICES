package tokai.com.mx.SIGMAV2.modules.users.domain.port.input;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada - Define los casos de uso del módulo de usuarios
 * Esta interfaz representa la API del dominio (hexágono)
 */
public interface UserService {
    
    /**
     * Registra un nuevo usuario en el sistema
     */
    User register(UserRequest request);
    
    /**
     * Verifica un usuario con su código de verificación
     */
    Optional<User> verify(String email, String code);
    
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
    Optional<User> verifyByUsernameAndCode(String email, String code);
    
    /**
     * Reenvía código de verificación a un usuario no verificado
     */
    void resendVerificationCode(String email);
    
    /**
     * Busca un usuario por username (alias para email)
     */
    Optional<User> findByUsername(String email);
    
    /**
     * Elimina un usuario por username (alias para email)
     */
    void deleteByUsername(String email);
    
    /**
     * Actualiza un usuario existente
     */
    User update(User user);
    
    /**
     * Actualiza solo el rol de un usuario
     */
    User updateUserRole(Long userId, String role);

    // ============ MÉTODOS ADMINISTRATIVOS ============
    
    /**
     * Obtiene todos los usuarios con paginación
     */
    Page<User> findAllUsers(Pageable pageable);
    
    /**
     * Busca usuarios por criterios (email, rol, estado)
     */
    Page<User> searchUsers(String email, String role, Boolean verified, Boolean status, Pageable pageable);
    
    /**
     * Cuenta usuarios por estado
     */
    long countByVerified(boolean verified);
    
    /**
     * Cuenta usuarios por status
     */
    long countByStatus(boolean status);
    
    /**
     * Obtiene usuarios no verificados hace más de X días
     */
    List<User> findUnverifiedUsersOlderThan(int days);
    
    /**
     * Actualiza el estado de verificación de un usuario (solo admin)
     */
    User updateVerificationStatus(Long userId, boolean verified);
    
    /**
     * Resetea los intentos de verificación de un usuario
     */
    User resetUserAttempts(Long userId);
    
    /**
     * Bloquea o desbloquea una cuenta de usuario
     */
    User toggleUserStatus(Long userId);
    
    /**
     * Fuerza la verificación de un usuario sin código
     */
    User forceVerifyUser(Long userId);
    
    /**
     * Elimina usuarios no verificados antiguos
     */
    int cleanupUnverifiedUsers(int daysOld);
}
