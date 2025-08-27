package tokai.com.mx.SIGMAV2.modules.users.domain.port.input;

import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;

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
     * Busca un usuario por username (alias para email)
     */
    Optional<User> findByUsername(String email);
    
    /**
     * Elimina un usuario por username (alias para email)
     */
    void deleteByUsername(String email);
}
