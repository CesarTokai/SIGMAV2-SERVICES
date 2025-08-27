package tokai.com.mx.SIGMAV2.modules.users.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;
import tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;
import tokai.com.mx.SIGMAV2.shared.exception.*;
import tokai.com.mx.SIGMAV2.shared.validation.ValidationUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registrar un nuevo usuario con validaciones completas.
     */
    @Transactional
    public BeanUser register(UserRequest request) {
        log.info("Iniciando registro de usuario para email: {}", request.getEmail());
        
        // Validaciones de entrada
        validateUserRegistrationRequest(request);
        
        // Verificar si ya existe el usuario
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro con email ya existente: {}", request.getEmail());
            throw new UserAlreadyExistsException(
                "Ya existe un usuario registrado con el correo electrónico: " + request.getEmail()
            );
        }

        try {
            BeanUser user = new BeanUser();
            user.setEmail(request.getEmail().toLowerCase().trim());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole(ERole.valueOf(request.getRole().toUpperCase()));
            user.setStatus(false);
            user.setVerified(false);
            user.setAttempts(0);
            user.setVerificationCode(generateVerificationCode());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            BeanUser savedUser = userRepository.save(user);
            log.info("Usuario registrado exitosamente con ID: {}", savedUser.getId());
            return savedUser;
            
        } catch (Exception e) {
            log.error("Error al registrar usuario: {}", e.getMessage(), e);
            throw new CustomException("Error interno al registrar el usuario. Intente nuevamente.");
        }
    }

    /**
     * Verifica si ya existe un usuario por correo electrónico.
     */
    public boolean existsByEmail(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    /**
     * Verifica si ya existe un usuario por nombre de usuario (email como username).
     */
    public boolean existsByUsername(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.findByEmail(email.toLowerCase().trim()).isPresent();
    }

    /**
     * Elimina un usuario por su email con validaciones.
     */
    @Transactional
    public void deleteByUsername(String email) {
        log.info("Iniciando eliminación de usuario: {}", email);
        
        ValidationUtils.validateEmail(email);
        
        String normalizedEmail = email.toLowerCase().trim();
        
        // Verificar que el usuario existe
        if (!userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Intento de eliminar usuario inexistente: {}", normalizedEmail);
            throw new UserNotFoundException(
                "No se encontró un usuario con el correo electrónico: " + normalizedEmail
            );
        }

        try {
            userRepository.deleteByEmail(normalizedEmail);
            log.info("Usuario eliminado exitosamente: {}", normalizedEmail);
        } catch (Exception e) {
            log.error("Error al eliminar usuario {}: {}", normalizedEmail, e.getMessage(), e);
            throw new CustomException("Error interno al eliminar el usuario. Intente nuevamente.");
        }
    }

    /**
     * Verifica un usuario por email y código con validaciones completas.
     */
    @Transactional
    public Optional<BeanUser> verifyByUsernameAndCode(String email, String code) {
        log.info("Iniciando verificación de usuario: {}", email);
        
        // Validaciones de entrada
        ValidationUtils.validateEmail(email);
        ValidationUtils.validateVerificationCode(code);
        
        String normalizedEmail = email.toLowerCase().trim();
        
        // Buscar usuario con email y código
        Optional<BeanUser> optionalUser = userRepository.verifyByEmailAndCode(normalizedEmail, code);
        
        if (optionalUser.isEmpty()) {
            log.warn("Verificación fallida para {}: código inválido o usuario no encontrado", normalizedEmail);
            throw new InvalidVerificationCodeException(
                "El código de verificación es inválido o ha expirado para el usuario: " + normalizedEmail
            );
        }

        BeanUser user = optionalUser.get();
        
        // Verificar si ya está verificado
        if (user.isVerified()) {
            log.warn("Intento de verificar usuario ya verificado: {}", normalizedEmail);
            throw new CustomException("El usuario ya ha sido verificado previamente");
        }

        try {
            // Actualizar estado de verificación
            user.setVerified(true);
            user.setStatus(true);
            user.setVerificationCode(null);
            user.setUpdatedAt(LocalDateTime.now());
            
            BeanUser updatedUser = userRepository.save(user);
            log.info("Usuario verificado exitosamente: {}", normalizedEmail);
            
            return Optional.of(updatedUser);
            
        } catch (Exception e) {
            log.error("Error al verificar usuario {}: {}", normalizedEmail, e.getMessage(), e);
            throw new CustomException("Error interno al verificar el usuario. Intente nuevamente.");
        }
    }

    /**
     * Consulta un usuario por su email con validación.
     */
    public Optional<BeanUser> findByUsername(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        
        Optional<BeanUser> user = userRepository.findByEmail(normalizedEmail);
        
        if (user.isEmpty()) {
            log.warn("Usuario no encontrado: {}", normalizedEmail);
        }
        
        return user;
    }

    /**
     * Incrementa el número de intentos fallidos por email con validaciones.
     */
    @Transactional
    public void incrementAttempts(String email) {
        log.info("Incrementando intentos para usuario: {}", email);
        
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        
        // Verificar que el usuario existe
        if (!userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Intento de incrementar intentos de usuario inexistente: {}", normalizedEmail);
            throw new UserNotFoundException(
                "No se encontró un usuario con el correo electrónico: " + normalizedEmail
            );
        }

        try {
            userRepository.incrementAttempts(normalizedEmail);
            log.info("Intentos incrementados para usuario: {}", normalizedEmail);
        } catch (Exception e) {
            log.error("Error al incrementar intentos para {}: {}", normalizedEmail, e.getMessage(), e);
            throw new CustomException("Error interno al actualizar intentos del usuario.");
        }
    }

    /**
     * Validaciones completas para el registro de usuario.
     */
    private void validateUserRegistrationRequest(UserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del usuario son obligatorios");
        }

        // Validar email
        ValidationUtils.validateEmail(request.getEmail());
        
        // Validar contraseña
        ValidationUtils.validatePassword(request.getPassword());
        
        // Validar rol
        ValidationUtils.validateRole(request.getRole());
    }

    /**
     * Genera un código de verificación de 6 dígitos.
     */
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }
} 
