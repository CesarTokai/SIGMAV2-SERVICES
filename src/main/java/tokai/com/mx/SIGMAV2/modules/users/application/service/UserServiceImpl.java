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
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.repository.UserWarehouseAssignmentRepository;
import tokai.com.mx.SIGMAV2.shared.exception.*;
import tokai.com.mx.SIGMAV2.shared.validation.ValidationUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @deprecated Reemplazado por UserApplicationService.
 * No registrado como bean de Spring para evitar ambigüedad de inyección.
 */
@Deprecated
@Slf4j
@SuppressWarnings("DeprecatedIsStillUsed")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final UserWarehouseAssignmentRepository userWarehouseAssignmentRepository;

    @Override
    @Transactional
    public User register(RegisterUserCommand command) {
        log.info("Iniciando registro de usuario para email: {}", command.getEmail());

        if (userRepository.existsByEmail(command.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Ya existe un usuario registrado con el correo: " + command.getEmail());
        }

        try {
            User user = new User(
                    null,
                    command.getEmail().toLowerCase().trim(),
                    passwordEncoder.encode(command.getPassword()),
                    Role.valueOf(command.getRole().toUpperCase()),
                    false, false, 0, null,
                    generateVerificationCode(),
                    LocalDateTime.now(), LocalDateTime.now()
            );

            User savedUser = userRepository.save(user);
            log.info("Usuario registrado con ID: {}", savedUser.getId());

            try {
                mailSender.send(savedUser.getEmail(),
                        "SIGMAV2 - Código de Verificación",
                        String.format(
                            "Hola,\n\nTu código de verificación es: %s\n\nSaludos,\nEquipo SIGMAV2",
                            savedUser.getVerificationCode()));
            } catch (Exception e) {
                log.error("Error al enviar correo a {}: {}", savedUser.getEmail(), e.getMessage());
            }

            return savedUser;

        } catch (Exception e) {
            log.error("Error al registrar usuario: {}", e.getMessage(), e);
            throw new CustomException("Error interno al registrar el usuario. Intente nuevamente.");
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) throw new IllegalArgumentException("El ID del usuario es obligatorio");
        return userRepository.findById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        ValidationUtils.validateEmail(email);
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    @Override
    @Transactional
    public void deleteByEmail(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("No se encontró usuario: " + normalizedEmail));

        if (hasWarehouseAssignments(user.getId())) {
            throw new CustomException("No se puede eliminar: el usuario tiene almacenes asignados.");
        }

        userRepository.deleteByEmail(normalizedEmail);
    }

    @Override
    public Optional<User> verify(String email, String code) {
        ValidationUtils.validateEmail(email);
        ValidationUtils.validateVerificationCode(code);
        String normalizedEmail = email.toLowerCase().trim();

        Optional<User> optionalUser = userRepository.verifyByEmailAndCode(normalizedEmail, code);
        if (optionalUser.isEmpty()) {
            throw new InvalidVerificationCodeException("Código inválido o expirado para: " + normalizedEmail);
        }

        User user = optionalUser.get();
        if (user.isVerified()) throw new CustomException("El usuario ya ha sido verificado");

        user.markAsVerified();
        return Optional.of(userRepository.save(user));
    }

    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        ValidationUtils.validateEmail(email);
        String normalizedEmail = email.toLowerCase().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + email));

        if (user.isVerified()) throw new UserAlreadyExistsException("El usuario ya está verificado");

        String newCode = generateVerificationCode();
        user.setVerificationCode(newCode);
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        try {
            mailSender.send(updatedUser.getEmail(),
                    "SIGMAV2 - Nuevo Código de Verificación",
                    String.format(
                        "Hola,\n\nTu nuevo código de verificación es: %s\n\nVálido por 24 horas.\n\nSaludos,\nEquipo SIGMAV2",
                        newCode));
        } catch (Exception e) {
            log.warn("No se pudo enviar correo de reenvío a {}: {}", normalizedEmail, e.getMessage());
        }
    }

    @Override
    @Transactional
    public User update(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("El usuario y su ID son obligatorios");
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserRole(Long userId, String role) {
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Rol inválido: " + role);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        user.setRole(roleEnum);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    // ── Administración ────────────────────────────────────────────────────

    @Override
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchUsers(String email, String role, Boolean verified, Boolean status, Pageable pageable) {
        Role roleEnum = null;
        if (role != null && !role.trim().isEmpty()) {
            try { roleEnum = Role.valueOf(role.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new InvalidRoleException("Rol inválido: " + role); }
        }
        return userRepository.findByCriteria(email, roleEnum, verified, status, pageable);
    }

    @Override public long countByVerified(boolean verified) { return userRepository.countByVerified(verified); }
    @Override public long countByStatus(boolean status)     { return userRepository.countByStatus(status); }

    @Override
    public List<User> findUnverifiedUsersOlderThan(int days) {
        return userRepository.findUnverifiedUsersOlderThan(LocalDateTime.now().minusDays(days));
    }

    @Override
    @Transactional
    public User updateVerificationStatus(Long userId, boolean verified) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        if (verified) user.markAsVerified();
        else { user.setVerified(false); user.setUpdatedAt(LocalDateTime.now()); }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User resetUserAttempts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        user.setAttempts(0);
        user.setLastTryAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        user.setStatus(!user.isStatus());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User forceVerifyUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + userId));
        user.markAsVerified();
        user.setAttempts(0);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public int cleanupUnverifiedUsers(int daysOld) {
        return userRepository.deleteUnverifiedUsersOlderThan(LocalDateTime.now().minusDays(daysOld));
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private String generateVerificationCode() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    private boolean hasWarehouseAssignments(Long userId) {
        try {
            var assignments = userWarehouseAssignmentRepository.findByUserIdAndIsActiveTrue(userId);
            return assignments != null && !assignments.isEmpty();
        } catch (Exception e) {
            log.warn("Error al verificar asignaciones para usuario {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
