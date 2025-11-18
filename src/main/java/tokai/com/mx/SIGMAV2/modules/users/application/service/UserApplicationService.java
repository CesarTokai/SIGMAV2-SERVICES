package tokai.com.mx.SIGMAV2.modules.users.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.mapper.BeanPersonalInformationMapper;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.PersonalInformation;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.MailSender;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;
import tokai.com.mx.SIGMAV2.shared.exception.*;
import tokai.com.mx.SIGMAV2.shared.validation.ValidationUtils;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.output.PersonalInformationRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.UserEntity;

import java.time.LocalDateTime;
import java.util.List;
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
    private final VerificationCodeService verificationCodeService;
    private final PersonalInformationRepository personalInformationRepository;

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
            log.warn("Intento de registro con email existente: {}", normalizedEmail);
            throw new UserAlreadyExistsException("Ya existe un usuario con el email: " + normalizedEmail);
        }
        
        try {
            // Encriptar contraseña
            String encryptedPassword = passwordEncoder.encode(request.getPassword());
            
            // Crear entidad de dominio
            User user = new User(
                    null, // ID será asignado por la base de datos
                    normalizedEmail,
                    encryptedPassword,
                    Role.valueOf(request.getRole().toUpperCase()),
                    true, // status activo
                    false, // no verificado inicialmente
                    0, // attempts
                    null, // lastTryAt
                    null, // verificationCode - se genera después
                    LocalDateTime.now(), // createdAt
                    LocalDateTime.now() // updatedAt
            );
            
            // Guardar usuario
            User savedUser = userRepository.save(user);
            log.info("Usuario registrado exitosamente con ID: {}", savedUser.getId());

            // Crear y guardar información personal asociada
            BeanPersonalInformation personalInfo = new BeanPersonalInformation();
            personalInfo.setName(request.getName());
            personalInfo.setFirstLastName(request.getFirstLastName());
            personalInfo.setSecondLastName(request.getSecondLastName());
            personalInfo.setPhoneNumber(request.getPhoneNumber());
            personalInfo.setComments(request.getComments());
            // Relacionar con el usuario (UserEntity)
            UserEntity userEntity = new UserEntity();
            userEntity.setUserId(savedUser.getId());
            personalInfo.setUser(userEntity);
            PersonalInformation personalInformationEntity = BeanPersonalInformationMapper.toEntity(personalInfo);
            personalInformationRepository.save(personalInformationEntity);
            log.info("Información personal creada para el usuario ID: {}", savedUser.getId());

            // Generar y enviar código de verificación usando el nuevo servicio
            try {
                String verificationCode = verificationCodeService.generateVerificationCode(
                    savedUser.getEmail(), "Registro inicial");
                
                // Actualizar el usuario con el código
                savedUser.setVerificationCode(verificationCode);
                savedUser = userRepository.save(savedUser);
                String subject = "SIGMAV2 - Código de Verificación";
                String message = String.format(
                    "Hola,\n\n" +
                    "Gracias por registrarte en SIGMAV2.\n\n" +
                    "Tu código de verificación es: %s\n\n" +
                    "Por favor, ingresa este código para activar tu cuenta.\n\n" +
                    "Este código tiene una validez de 24 horas.\n\n" +
                    "Si no solicitaste este registro, puedes ignorar este correo.\n\n" +
                    "Saludos,\n" +
                    "Equipo SIGMAV2",
                    verificationCode
                );
                
                mailSender.send(savedUser.getEmail(), subject, message);
                log.info("Código de verificación enviado al correo: {}", savedUser.getEmail());
            } catch (Exception e) {
                log.error("Error al enviar código de verificación a {}: {}", savedUser.getEmail(), e.getMessage());
                // No lanzamos excepción porque el usuario ya fue creado exitosamente
            }
            return savedUser;
        } catch (Exception e) {
            log.error("Error al registrar usuario {}: {}", normalizedEmail, e.getMessage(), e);
            throw new CustomException("Error interno al registrar el usuario. Intente nuevamente.");
        }
    }

    /**
     * Buscar un usuario por email
     */
    @Override
    public Optional<User> findByEmail(String email) {
        log.info("Buscando usuario por email: {}", email);
        
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        
        return userRepository.findByEmail(normalizedEmail);
    }

    /**
     * Buscar un usuario por ID
     */
    @Override
    public Optional<User> findById(Long id) {
        log.info("Buscando usuario por ID: {}", id);
        
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
        String normalizedEmail = email.toLowerCase().trim();
        
        return userRepository.existsByEmail(normalizedEmail);
    }

    /**
     * Eliminar un usuario por email
     */
    @Override
    @Transactional
    public void deleteByEmail(String email) {
        log.info("Eliminando usuario: {}", email);
        
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();
        
        if (!userRepository.existsByEmail(normalizedEmail)) {
            throw new UserNotFoundException("Usuario no encontrado: " + email);
        }
        
        userRepository.deleteByEmail(normalizedEmail);
        log.info("Usuario eliminado exitosamente: {}", normalizedEmail);
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
        
        // Verificar el código usando el nuevo servicio
        if (!verificationCodeService.validateVerificationCode(normalizedEmail, code)) {
            throw new InvalidVerificationCodeException(
                "Código de verificación inválido para el usuario: " + email
            );
        }
        
        // Buscar usuario
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("Usuario no encontrado: " + email);
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
        
        try {
            // Generar nuevo código usando el servicio especializado
            String newVerificationCode = verificationCodeService.generateVerificationCode(
                normalizedEmail, "Reenvío solicitado por usuario");
            
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
     * Actualiza un usuario existente
     */
    @Override
    @Transactional
    public User update(User user) {
        log.info("Actualizando usuario ID: {}", user.getId());
        
        if (user.getId() == null) {
            throw new IllegalArgumentException("No se puede actualizar un usuario sin ID");
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Actualiza solo el rol de un usuario
     *
     * @param userId
     * @param role
     */
    @Override
    public User updateUserRole(Long userId, String role) {
        log.info("Actualizando rol del usuario ID: {} a {}", userId, role);
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new UserNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        User user = optionalUser.get();
        try {
            Role newRole = Role.valueOf(role.toUpperCase());
            user.setRole(newRole);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Rol inválido: " + role);
        }
    }

    // ============ MÉTODOS ADMINISTRATIVOS ============
    
    /**
     * Obtiene todos los usuarios con paginación
     */
    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        log.info("Obteniendo usuarios con paginación: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
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
