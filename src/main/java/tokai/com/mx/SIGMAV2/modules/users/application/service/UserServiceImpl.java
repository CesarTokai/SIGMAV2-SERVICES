package tokai.com.mx.SIGMAV2.modules.users.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;
import tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registrar un nuevo usuario con datos básicos y contraseña encriptada.
     */
    @Transactional
    public BeanUser register(UserRequest request) {
        BeanUser user = new BeanUser();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(ERole.valueOf(request.getRole()));
        user.setStatus(false);
        user.setVerified(false);
        user.setAttempts(0);
        user.setVerificationCode(generateVerificationCode());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Verifica si ya existe un usuario por correo electrónico.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Verifica si ya existe un usuario por nombre de usuario (email como username).
     */
    public boolean existsByUsername(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Elimina un usuario por su email.
     */
    @Transactional
    public void deleteByUsername(String email) {
        userRepository.deleteByEmail(email);
    }

    /**
     * Verifica un usuario por email y código.
     */
    @Transactional
    public Optional<BeanUser> verifyByUsernameAndCode(String email, String code) {
        Optional<BeanUser> optionalUser = userRepository.verifyByEmailAndCode(email, code);
        optionalUser.ifPresent(user -> {
            user.setVerified(true);
            user.setStatus(true);
            user.setVerificationCode(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        });
        return optionalUser;
    }

    /**
     * Consulta un usuario por su email (username).
     */
    public Optional<BeanUser> findByUsername(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Genera un código de verificación simple (puedes mejorar con UUID o generador personalizado).
     */
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 900000 + 100000));
    }

    /**
     * Incrementa el número de intentos fallidos por email.
     */
    @Transactional
    public void incrementAttempts(String email) {
        userRepository.incrementAttempts(email);
    }
} 
