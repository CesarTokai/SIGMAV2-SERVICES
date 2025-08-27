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

@Slf4j
@Service("userServiceImplLegacy")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;

    /**
     * Registrar un nuevo usuario con validaciones completas.
     */
    @Transactional
    public User register(UserRequest request) {
        log.info("Iniciando registro de usuario para email: {}", request.getEmail());

        // Validaciones de entrada
        validateUserRegistrationRequest(request);

        // Verificar si ya existe el usuario
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro con email ya existente: {}", request.getEmail());
            throw new UserAlreadyExistsException(
                    "Ya existe un usuario registrado con el correo electrónico: " + request.getEmail());
        }

        try {
            User user = new User(
                    null, // id será asignado por la base de datos
                    request.getEmail().toLowerCase().trim(),
                    passwordEncoder.encode(request.getPassword()),
                    Role.valueOf(request.getRole().toUpperCase()),
                    false, // status
                    false, // verified
                    0, // attempts
                    null, // lastTryAt
                    generateVerificationCode(),
                    LocalDateTime.now(), // createdAt
                    LocalDateTime.now() // updatedAt
            );

            User savedUser = userRepository.save(user);
            log.info("Usuario registrado exitosamente con ID: {}", savedUser.getId());

            // Enviar código de verificación por correo
            try {
                String subject = "SIGMAV2 - Código de Verificación";
                String message = String.format(
                        "Hola,\n\n" +
                                "Gracias por registrarte en SIGMAV2.\n\n" +
                                "Tu código de verificación es: %s\n\n" +
                                "Por favor, ingresa este código para activar tu cuenta.\n\n" +
                                "Si no solicitaste este registro, puedes ignorar este correo.\n\n" +
                                "Saludos,\n" +
                                "Equipo SIGMAV2",
                        savedUser.getVerificationCode());

                mailSender.send(savedUser.getEmail(), subject, message);
                log.info("Código de verificación enviado al correo: {}", savedUser.getEmail());

            } catch (Exception e) {
                log.error("Error al enviar código de verificación a {}: {}", savedUser.getEmail(), e.getMessage());
                // No lanzamos excepción porque el usuario ya fue creado exitosamente
                // Solo logueamos el error del envío de correo
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
                    "No se encontró un usuario con el correo electrónico: " + normalizedEmail);
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
    public Optional<User> verifyByUsernameAndCode(String email, String code) {
        log.info("Iniciando verificación de usuario: {}", email);

        // Validaciones de entrada
        ValidationUtils.validateEmail(email);
        ValidationUtils.validateVerificationCode(code);

        String normalizedEmail = email.toLowerCase().trim();

        // Buscar usuario con email y código
        Optional<User> optionalUser = userRepository.verifyByEmailAndCode(normalizedEmail, code);

        if (optionalUser.isEmpty()) {
            log.warn("Verificación fallida para {}: código inválido o usuario no encontrado", normalizedEmail);
            throw new InvalidVerificationCodeException(
                    "El código de verificación es inválido o ha expirado para el usuario: " + normalizedEmail);
        }

        User user = optionalUser.get();

        // Verificar si ya está verificado
        if (user.isVerified()) {
            log.warn("Intento de verificar usuario ya verificado: {}", normalizedEmail);
            throw new CustomException("El usuario ya ha sido verificado previamente");
        }

        try {
            // Actualizar estado de verificación usando métodos de dominio
            user.markAsVerified();
            user.setVerificationCode(null);
            user.setUpdatedAt(LocalDateTime.now());

            User updatedUser = userRepository.save(user);
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
    public Optional<User> findByUsername(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();

        Optional<User> user = userRepository.findByEmail(normalizedEmail);

        if (user.isEmpty()) {
            log.warn("Usuario no encontrado: {}", normalizedEmail);
        }

        return user;
    }

    /**
     * Busca un usuario por ID
     */
    public Optional<User> findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del usuario es obligatorio");
        }
        return userRepository.findById(id);
    }

    /**
     * Busca un usuario por email
     */
    public Optional<User> findByEmail(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        return userRepository.findByEmail(normalizedEmail);
    }

    /**
     * Verifica un usuario con su código de verificación
     */
    @Transactional
    public Optional<User> verify(String email, String code) {
        return verifyByUsernameAndCode(email, code);
    }

    /**
     * Elimina un usuario por email
     */
    @Transactional
    public void deleteByEmail(String email) {
        log.info("Iniciando eliminación de usuario: {}", email);

        ValidationUtils.validateEmail(email);

        String normalizedEmail = email.toLowerCase().trim();

        // Verificar que el usuario existe
        if (!userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Intento de eliminar usuario inexistente: {}", normalizedEmail);
            throw new UserNotFoundException(
                    "No se encontró un usuario con el correo electrónico: " + normalizedEmail);
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
                    "No se encontró un usuario con el correo electrónico: " + normalizedEmail);
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
