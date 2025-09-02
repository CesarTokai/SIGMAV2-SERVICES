package tokai.com.mx.SIGMAV2.modules.users.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tokai.com.mx.SIGMAV2.modules.users.application.service.UserApplicationService;
import tokai.com.mx.SIGMAV2.modules.users.application.service.VerificationCodeService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.MailSender;

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
            VerificationCodeService verificationCodeService) {
        return new UserApplicationService(userRepository, passwordEncoder, mailSender, verificationCodeService);
    }
}
