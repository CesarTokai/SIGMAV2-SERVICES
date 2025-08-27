package tokai.com.mx.SIGMAV2.modules.users.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.MailSender;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;
import tokai.com.mx.SIGMAV2.shared.exception.*;
import tokai.com.mx.SIGMAV2.shared.validation.ValidationUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servicio de aplicación que implementa los casos de uso de usuarios
 * Implementa el puerto de entrada (UserService) y orquesta las operaciones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;

    /**
     * Registrar un nuevo usuario con validaciones completas.
     */
    @Override
    @Transactional
    public User register(UserRequest request) {
        log.info("Iniciando registro de usuario: {}", request.getEmail());
        
        // Validaciones de entrada
        ValidationUtils.validateEmail(request.getEmail());
        ValidationUtils.validatePassword(request.getPassword());
        
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        
        // Verificar si ya existe el usuario
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Intento de registro con email ya existente: {}", normalizedEmail);
            throw new UserAlreadyExistsException(
                "Ya existe un usuario registrado con el correo electrónico: " + request.getEmail()
            );
        }

        try {
            User user = new User();
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            user.setStatus(false);
            user.setVerified(false);
            user.setAttempts(0);
            user.setVerificationCode(generateVerificationCode());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            User savedUser = userRepository.save(user);
            log.info("Usuario registrado exitosamente con ID: {}", savedUser.getId());
            
            // Enviar código de verificación por correo
            try {
                String subject = "SIGMAV2 - Código de Verificación";
                String message = String.format(
                    "Hola,\\n\\n" +
                    "Gracias por registrarte en SIGMAV2.\\n\\n" +
                    "Tu código de verificación es: %s\\n\\n" +
                    "Por favor, ingresa este código para activar tu cuenta.\\n\\n" +
                    "Si no solicitaste este registro, puedes ignorar este correo.\\n\\n" +
                    "Saludos,\\n" +
                    "Equipo SIGMAV2",
                    savedUser.getVerificationCode()
                );
                
                mailSender.send(savedUser.getEmail(), subject, message);
                log.info("Código de verificación enviado al correo: {}", savedUser.getEmail());
                
            } catch (Exception e) {
                log.error("Error al enviar código de verificación a {}: {}", savedUser.getEmail(), e.getMessage());
                // No lanzamos excepción porque el usuario ya fue creado exitosamente
            }
            
            return savedUser;
            
        } catch (Exception e) {
            log.error("Error al registrar usuario: {}", e.getMessage(), e);
            throw new CustomException("Error interno al registrar el usuario. Intente nuevamente.");
        }
    }

    /**
     * Verifica si ya existe un usuario por correo electrónico.
     */
    @Override
    public boolean existsByEmail(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    /**
     * Busca un usuario por email
     */
    @Override
    public Optional<User> findByEmail(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    /**
     * Busca un usuario por ID
     */
    @Override
    public Optional<User> findById(Long id) {
        if (id == null || id <= 0) {
            log.warn("ID de usuario inválido: {}", id);
            return Optional.empty();
        }
        return userRepository.findById(id);
    }

    /**
     * Elimina un usuario por email
     */
    @Override
    public void deleteByEmail(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        
        if (!userRepository.existsByEmail(normalizedEmail)) {
            throw new UserNotFoundException("No se encontró usuario con email: " + email);
        }
        
        userRepository.deleteByEmail(normalizedEmail);
        log.info("Usuario eliminado: {}", normalizedEmail);
    }

    /**
     * Verifica un usuario con email y código
     */
    @Override
    public Optional<User> verify(String email, String code) {
        log.info("Verificando usuario: {}", email);
        
        ValidationUtils.validateEmail(email);
        ValidationUtils.validateVerificationCode(code);
        
        String normalizedEmail = email.toLowerCase().trim();
        
        // Buscar usuario por email y código
        Optional<User> optionalUser = userRepository.verifyByEmailAndCode(normalizedEmail, code);
        
        if (optionalUser.isEmpty()) {
            log.warn("Código de verificación inválido para usuario: {}", normalizedEmail);
            throw new InvalidVerificationCodeException(
                "Código de verificación inválido para el usuario: " + email
            );
        }
        
        User user = optionalUser.get();
        
        // Verificar si el usuario ya está verificado
        if (user.isVerified()) {
            log.warn("Intento de verificación de usuario ya verificado: {}", normalizedEmail);
            throw new UserAlreadyExistsException("El usuario ya está verificado");
        }
        
        try {
            // Marcar como verificado usando método de dominio
            user.markAsVerified();
            User updatedUser = userRepository.save(user);
            
            log.info("Usuario verificado exitosamente: {}", normalizedEmail);
            return Optional.of(updatedUser);
            
        } catch (Exception e) {
            log.error("Error al verificar usuario {}: {}", normalizedEmail, e.getMessage(), e);
            throw new CustomException("Error interno al verificar el usuario. Intente nuevamente.");
        }
    }

    /**
     * Busca un usuario por email y código de verificación
     */
    @Override
    public Optional<User> verifyByUsernameAndCode(String email, String code) {
        return verify(email, code);
    }

    /**
     * Busca un usuario por username (alias para email)
     */
    @Override
    public Optional<User> findByUsername(String email) {
        return findByEmail(email);
    }

    /**
     * Elimina un usuario por username (alias para email)
     */
    @Override
    public void deleteByUsername(String email) {
        deleteByEmail(email);
    }

    /**
     * Genera un código de verificación de 6 dígitos.
     */
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }
}
