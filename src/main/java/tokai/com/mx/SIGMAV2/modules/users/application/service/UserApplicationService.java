package tokai.com.mx.SIGMAV2.modules.users.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.RegisterUserCommand;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.MailSender;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaPasswordResetAttemptRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserActivityLogRepository;
import tokai.com.mx.SIGMAV2.shared.exception.*;
import tokai.com.mx.SIGMAV2.shared.validation.ValidationUtils;

import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository.IRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.UserWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicación que implementa los casos de uso de usuarios
 * Implementa el puerto de entrada (UserService) y orquesta las operaciones
 */
@Slf4j
@RequiredArgsConstructor
public class UserApplicationService implements UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final JpaUserRepository jpaUserRepository;
    private final IRequestRecoveryPassword requestRecoveryPasswordRepository;
    private final UserWarehouseRepository userWarehouseRepository;
    private final JpaUserActivityLogRepository userActivityLogRepository;
    private final JpaPasswordResetAttemptRepository passwordResetAttemptRepository;

    // ── registro ──────────────────────────────────────────────────────────

    /**
     * Registrar un nuevo usuario con validaciones completas.
     */
    @Override
    @Transactional
    public User register(RegisterUserCommand command) {
        log.info("Iniciando registro de usuario: {}", command.getEmail());

        // Validar campos obligatorios
        validateRequiredFields(command);

        // Validaciones de formato
        ValidationUtils.validateEmail(command.getEmail());
        ValidationUtils.validatePassword(command.getPassword());

        String normalizedEmail = command.getEmail().toLowerCase().trim();

        // Verificar si ya existe el usuario
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Intento de registro con email existente: {}", normalizedEmail);
            throw new UserAlreadyExistsException("Ya existe un usuario con el email: " + normalizedEmail);
        }

        try {
            // Crear y guardar usuario
            User user = createUser(command, normalizedEmail);
            User savedUser = userRepository.save(user);

            log.info("Usuario registrado exitosamente con ID: {}", savedUser.getId());


            // Enviar correo de verificación si el usuario no está pre-verificado
            if (!isPreVerified(command)) {
                sendVerificationEmail(savedUser);
            }

            return savedUser;

        } catch (Exception e) {
            log.error("Error al registrar usuario {}: {}", normalizedEmail, e.getMessage());
            throw new CustomException("Error al crear usuario: " + e.getMessage());
        }
    }

    private void validateRequiredFields(RegisterUserCommand command) {
        if (command.getName() == null || command.getName().trim().isEmpty())
            throw new CustomException("El campo 'name' es obligatorio y no puede estar vacío.");
        if (command.getFirstLastName() == null || command.getFirstLastName().trim().isEmpty())
            throw new CustomException("El campo 'firstLastName' es obligatorio y no puede estar vacío.");
        if (command.getEmail() == null || command.getEmail().trim().isEmpty())
            throw new CustomException("El campo 'email' es obligatorio y no puede estar vacío.");
        if (command.getPassword() == null || command.getPassword().trim().isEmpty())
            throw new CustomException("El campo 'password' es obligatorio y no puede estar vacío.");
        if (command.getRole() == null || command.getRole().trim().isEmpty())
            throw new CustomException("El campo 'role' es obligatorio y no puede estar vacío.");
    }

    private User createUser(RegisterUserCommand command, String normalizedEmail) {
        String encryptedPassword = passwordEncoder.encode(command.getPassword());
        boolean status     = command.getStatus()      != null && command.getStatus();
        boolean preVerified = isPreVerified(command);

        String verificationCode = null;
        if (!preVerified) {
            verificationCode = generateCode();
        }

        return new User(null, normalizedEmail, encryptedPassword,
                Role.valueOf(command.getRole().toUpperCase()),
                status, preVerified, 0, null,
                verificationCode, LocalDateTime.now(), LocalDateTime.now(),
                command.getName(), command.getFirstLastName(), command.getSecondLastName(),
                command.getPhoneNumber(), command.getComments());
    }



    private boolean isPreVerified(RegisterUserCommand command) {
        return command.getPreVerified() != null && command.getPreVerified();
    }

    private void sendVerificationEmail(User user) {
        try {
            String subject = "SIGMAV2 - Código de Verificación";
            String message = String.format(
                "Hola,\n\nGracias por registrarte en SIGMAV2.\n\n" +
                "Tu código de verificación es: %s\n\n" +
                "Por favor, ingresa este código para activar tu cuenta.\n\n" +
                "Este código tiene una validez de 24 horas.\n\n" +
                "Si no solicitaste este registro, puedes ignorar este correo.\n\n" +
                "Saludos,\nEquipo SIGMAV2",
                user.getVerificationCode());
            mailSender.send(user.getEmail(), subject, message);
            log.info("Correo de verificación enviado a: {}", user.getEmail());
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de verificación a {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // ── findByEmail ───────────────────────────────────────────────────────

    /**
     * Buscar un usuario por email
     */
    @Override
    public Optional<User> findByEmail(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    /**
     * Buscar un usuario por ID
     */
    @Override
    public Optional<User> findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID de usuario inválido");
        }
        
        return userRepository.findById(id);
    }

    /**
     * Verificar si existe un usuario por email
     */
    @Override
    public boolean existsByEmail(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    /**
     * Eliminar un usuario por email (con cascada de registros dependientes)
     */
    @Override
    @Transactional
    public void deleteByEmail(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        
        tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.BeanUser user = jpaUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + email));

        try {
            log.info("Iniciando eliminación en cascada del usuario: {}", normalizedEmail);

            // 1. Eliminar registros de actividad del usuario
            log.debug("Eliminando registros de actividad del usuario...");
            userActivityLogRepository.deleteByUser(user);

            // 2. Eliminar intentos de reset de contraseña
            log.debug("Eliminando intentos de reset de contraseña del usuario...");
            passwordResetAttemptRepository.deleteByUser(user);

            // 3. Eliminar solicitudes de recuperación de contraseña
            log.debug("Eliminando solicitudes de recuperación de contraseña del usuario...");
            requestRecoveryPasswordRepository.deleteByUser(user);

            // 4. Eliminar asignaciones de almacenes
            log.debug("Eliminando asignaciones de almacenes del usuario...");
            userWarehouseRepository.deleteByUserId(user.getId());

            // 5. Finalmente, eliminar el usuario
            log.debug("Eliminando usuario de la base de datos...");
            userRepository.deleteByEmail(normalizedEmail);

            log.info("Usuario {} eliminado exitosamente con cascada completa", normalizedEmail);
        } catch (Exception e) {
            log.error("Error al eliminar usuario en cascada {}: {}", normalizedEmail, e.getMessage(), e);
            throw new CustomException("Error al eliminar usuario: " + e.getMessage());
        }
    }

    // ── verificación ──────────────────────────────────────────────────────

    /**
     * Verifica un usuario con email y código
     */
    @Override
    public Optional<User> verify(String email, String code) {
        ValidationUtils.validateEmail(email);
        ValidationUtils.validateVerificationCode(code);
        String normalizedEmail = email.toLowerCase().trim();

        // Verificar el código directamente contra el campo del usuario
        User userToVerify = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + email));

        if (userToVerify.getVerificationCode() == null
                || !userToVerify.getVerificationCode().equals(code))
            throw new InvalidVerificationCodeException("Código de verificación inválido para: " + email);

        User user = userToVerify;

        // Verificar si el usuario ya está verificado
        if (user.isVerified())
            throw new UserAlreadyExistsException("El usuario ya está verificado");

        user.markAsVerified();
        return Optional.of(userRepository.save(user));
    }

    /**
     * Reenvía código de verificación a un usuario no verificado
     */
    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + email));

        if (user.isVerified())
            throw new UserAlreadyExistsException("El usuario ya está verificado");

        String newCode = generateCode();
        user.setVerificationCode(newCode);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        try {
            String subject = "SIGMAV2 - Nuevo Código de Verificación";
            String message = String.format(
                "Hola,\n\nHas solicitado un nuevo código de verificación.\n\n" +
                "Tu nuevo código es: %s\n\n" +
                "Válido por 24 horas.\n\nSaludos,\nEquipo SIGMAV2",
                newCode);
            mailSender.send(updatedUser.getEmail(), subject, message);
        } catch (Exception e) {
            log.warn("No se pudo enviar código de reenvío a {}: {}", normalizedEmail, e.getMessage());
        }
    }

    // ── actualización ─────────────────────────────────────────────────────

    /**
     * Actualiza un usuario existente
     */
    @Override
    @Transactional
    public User update(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("No se puede actualizar un usuario sin ID");
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Actualiza solo el rol de un usuario
     *
     * @param userId ID del usuario a actualizar
     * @param role Rol a asignar al usuario
     * @return Usuario actualizado
     */
    @Override
    public User updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        try {
            user.setRole(Role.valueOf(role.toUpperCase()));
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Rol inválido: " + role);
        }
    }

    // ── administración ────────────────────────────────────────────────────

    /**
     * Obtiene todos los usuarios con paginación
     */
    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * Busca usuarios por criterios
     */
    @Override
    public Page<User> searchUsers(String email, String role, Boolean verified, Boolean status, Pageable pageable) {
        log.info("Buscando usuarios por criterios: email={}, role={}, verified={}, status={}", 
                email, role, verified, status);
        
        Role roleEnum = null;
        if (role != null && !role.trim().isEmpty()) {
            try {
                roleEnum = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidRoleException("Rol inválido: " + role);
            }
        }
        
        return userRepository.findByCriteria(email, roleEnum, verified, status, pageable);
    }
    
    /**
     * Cuenta usuarios por estado de verificación
     */
    @Override
    public long countByVerified(boolean verified) {
        return userRepository.countByVerified(verified);
    }
    
    /**
     * Cuenta usuarios por status
     */
    @Override
    public long countByStatus(boolean status) {
        return userRepository.countByStatus(status);
    }
    
    /**
     * Obtiene usuarios no verificados hace más de X días
     */
    @Override
    public List<User> findUnverifiedUsersOlderThan(int days) {
        log.info("Buscando usuarios no verificados hace más de {} días", days);
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.findUnverifiedUsersOlderThan(cutoffDate);
    }
    
    /**
     * Actualiza el estado de verificación de un usuario (solo admin)
     */
    @Override
    @Transactional
    public User updateVerificationStatus(Long userId, boolean verified) {
        log.info("Actualizando estado de verificación del usuario ID: {} a {}", userId, verified);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        if (verified) user.markAsVerified();
        else { user.setVerified(false); user.setUpdatedAt(LocalDateTime.now()); }
        return userRepository.save(user);
    }
    
    /**
     * Resetea los intentos de verificación de un usuario
     */
    @Override
    @Transactional
    public User resetUserAttempts(Long userId) {
        log.info("Reseteando intentos del usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        user.setAttempts(0);
        user.setLastTryAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * Bloquea o desbloquea una cuenta de usuario
     */
    @Override
    @Transactional
    public User toggleUserStatus(Long userId) {
        log.info("🔄 TOGGLE STATUS - Iniciando cambio de estado para usuario ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        user.setStatus(!user.isStatus());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * Fuerza la verificación de un usuario sin código
     */
    @Override
    @Transactional
    public User forceVerifyUser(Long userId) {
        log.info("Forzando verificación del usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        user.markAsVerified();
        user.setAttempts(0);
        return userRepository.save(user);
    }
    
    /**
     * Elimina usuarios no verificados antiguos
     */
    @Override
    @Transactional
    public int cleanupUnverifiedUsers(int daysOld) {
        log.info("Iniciando limpieza de usuarios no verificados hace más de {} días", daysOld);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deletedCount = userRepository.deleteUnverifiedUsersOlderThan(cutoffDate);
        
        log.info("Limpieza completada: {} usuarios no verificados eliminados", deletedCount);
        return deletedCount;
    }

    // ── utilidades ────────────────────────────────────────────────────────

    /** Genera un código de verificación numérico de 6 dígitos usando SecureRandom. */
    private static String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

}
