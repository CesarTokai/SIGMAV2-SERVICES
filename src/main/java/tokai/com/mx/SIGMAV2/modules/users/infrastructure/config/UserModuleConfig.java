package tokai.com.mx.SIGMAV2.modules.users.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tokai.com.mx.SIGMAV2.modules.users.application.service.UserApplicationService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.MailSender;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserActivityLogRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaPasswordResetAttemptRepository;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository.IRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.UserWarehouseRepository;

/**
 * Configuración para la inyección de dependencias del módulo de usuarios
 * Conecta los puertos con sus implementaciones
 */
@Configuration
public class UserModuleConfig {

    @Bean
    public UserService userService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            MailSender mailSender,
            JpaUserRepository jpaUserRepository,
            IRequestRecoveryPassword requestRecoveryPasswordRepository,
            UserWarehouseRepository userWarehouseRepository,
            JpaUserActivityLogRepository userActivityLogRepository,
            JpaPasswordResetAttemptRepository passwordResetAttemptRepository) {
        return new UserApplicationService(
                userRepository,
                passwordEncoder,
                mailSender,
                jpaUserRepository,
                requestRecoveryPasswordRepository,
                userWarehouseRepository,
                userActivityLogRepository,
                passwordResetAttemptRepository);
    }
}
