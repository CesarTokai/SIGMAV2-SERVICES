package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.AuthenticatedUserService;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;

/**
 * Adaptador de infraestructura que implementa AuthenticatedUserService.
 * Mantiene el acceso a JpaUserRepository confinado a la capa de infraestructura.
 */
@Service
@RequiredArgsConstructor
public class AuthenticatedUserServiceImpl implements AuthenticatedUserService {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public Long getUserIdByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con email: " + email))
                .getId();
    }
}

