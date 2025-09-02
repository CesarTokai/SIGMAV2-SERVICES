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
     * Actualiza un usuario existente
     */
    @Override
    @Transactional
    public User update(User user) {
        log.info("Actualizando usuario con ID: {}", user.getId());
        
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("El usuario y su ID son obligatorios");
        }
        
        try {
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            log.info("Usuario actualizado exitosamente: {}", user.getEmail());
            return updatedUser;
            
        } catch (Exception e) {
            log.error("Error al actualizar usuario {}: {}", user.getEmail(), e.getMessage(), e);
            throw new CustomException("Error interno al actualizar el usuario. Intente nuevamente.");
        }
    }

    /**
     * Genera un código de verificación de 6 dígitos.
     */
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }

    /**
     * Reenvía código de verificación a un usuario no verificado
     */
    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        log.info("Reenviando código de verificación para usuario: {}", email);
        
        // Validar email
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        
        // Buscar usuario
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);
        if (optionalUser.isEmpty()) {
            log.warn("Intento de reenvío de código para usuario inexistente: {}", normalizedEmail);
            throw new UserNotFoundException("Usuario no encontrado: " + email);
        }
        
        User user = optionalUser.get();
        
        // Verificar si el usuario ya está verificado
        if (user.isVerified()) {
            log.warn("Intento de reenvío de código para usuario ya verificado: {}", normalizedEmail);
            throw new UserAlreadyExistsException("El usuario ya está verificado");
        }
        
        // Generar nuevo código de verificación
        String newVerificationCode = generateVerificationCode();
        
        try {
            // Actualizar el código en la base de datos
            user.setVerificationCode(newVerificationCode);
            user.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userRepository.save(user);
            
            // Enviar nuevo código por correo
            String subject = "SIGMAV2 - Nuevo Código de Verificación";
            String message = String.format(
                "Hola,\\n\\n" +
                "Has solicitado un nuevo código de verificación para tu cuenta en SIGMAV2.\\n\\n" +
                "Tu nuevo código de verificación es: %s\\n\\n" +
                "Por favor, ingresa este código para activar tu cuenta.\\n\\n" +
                "Este código tiene una validez de 24 horas.\\n\\n" +
                "Si no solicitaste este reenvío, puedes ignorar este correo.\\n\\n" +
                "Saludos,\\n" +
                "Equipo SIGMAV2",
                newVerificationCode
            );
            
            mailSender.send(updatedUser.getEmail(), subject, message);
            log.info("Nuevo código de verificación enviado exitosamente a: {}", updatedUser.getEmail());
            
        } catch (Exception e) {
            log.error("Error al reenviar código de verificación a {}: {}", normalizedEmail, e.getMessage(), e);
            throw new CustomException("Error interno al reenviar el código de verificación. Intente nuevamente.");
        }
    }

    // ============ MÉTODOS ADMINISTRATIVOS FALTANTES ============
    
    /**
     * Obtiene todos los usuarios con paginación
     */
    @Override
    public org.springframework.data.domain.Page<User> findAllUsers(org.springframework.data.domain.Pageable pageable) {
        log.info("Obteniendo usuarios con paginación: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }
    
    /**
     * Busca usuarios por criterios
     */
    @Override
    public org.springframework.data.domain.Page<User> searchUsers(String email, String role, Boolean verified, Boolean status, org.springframework.data.domain.Pageable pageable) {
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
    public java.util.List<User> findUnverifiedUsersOlderThan(int days) {
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
        
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        
        User user = optionalUser.get();
        
        if (verified) {
            user.markAsVerified();
            log.info("Usuario ID: {} marcado como verificado por administrador", userId);
        } else {
            user.setVerified(false);
            user.setUpdatedAt(LocalDateTime.now());
            log.info("Usuario ID: {} marcado como no verificado por administrador", userId);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * Resetea los intentos de verificación de un usuario
     */
    @Override
    @Transactional
    public User resetUserAttempts(Long userId) {
        log.info("Reseteando intentos del usuario ID: {}", userId);
        
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        
        User user = optionalUser.get();
        user.setAttempts(0);
        user.setLastTryAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        log.info("Intentos reseteados para usuario ID: {}", userId);
        
        return updatedUser;
    }
    
    /**
     * Bloquea o desbloquea una cuenta de usuario
     */
    @Override
    @Transactional
    public User toggleUserStatus(Long userId) {
        log.info("Cambiando estado del usuario ID: {}", userId);
        
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        
        User user = optionalUser.get();
        boolean newStatus = !user.isStatus();
        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        log.info("Estado del usuario ID: {} cambiado a {}", userId, newStatus ? "ACTIVO" : "INACTIVO");
        
        return updatedUser;
    }
    
    /**
     * Fuerza la verificación de un usuario sin código
     */
    @Override
    @Transactional
    public User forceVerifyUser(Long userId) {
        log.info("Forzando verificación del usuario ID: {}", userId);
        
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        
        User user = optionalUser.get();
        user.markAsVerified();
        user.setAttempts(0); // Resetear intentos también
        
        User updatedUser = userRepository.save(user);
        log.info("Usuario ID: {} verificado forzosamente por administrador", userId);
        
        return updatedUser;
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
}
