package tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper;

import org.springframework.stereotype.Component;

import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

@Component
public class UserMapper {

    /**
     * Convierte de BeanUser (infraestructura) a User (dominio)
     */
    public User toDomain(BeanUser beanUser) {
        if (beanUser == null) return null;
        
        User user = new User(
                beanUser.getId(),
                beanUser.getEmail(),
                beanUser.getPasswordHash(),
                mapToRole(beanUser.getRole()),
                beanUser.isStatus(),
                beanUser.isVerified(),
                beanUser.getAttempts(),
                beanUser.getLastTryAt(),
                beanUser.getVerificationCode(),
                beanUser.getCreatedAt(),
                beanUser.getUpdatedAt()
        );

        // Agregar nuevos campos
        user.setLastLoginAt(beanUser.getLastLoginAt());
        user.setLastActivityAt(beanUser.getLastActivityAt());
        user.setPasswordChangedAt(beanUser.getPasswordChangedAt());

        return user;
    }

    /**
     * Convierte de User (dominio) a BeanUser (infraestructura)
     */
    public BeanUser toBean(User user) {
        if (user == null) return null;
        
        BeanUser beanUser = new BeanUser();
        beanUser.setId(user.getId());
        beanUser.setEmail(user.getEmail());
        beanUser.setPasswordHash(user.getPasswordHash());
        beanUser.setRole(mapToERole(user.getRole()));
        beanUser.setStatus(user.isStatus());
        beanUser.setVerified(user.isVerified());
        beanUser.setAttempts(user.getAttempts());
        beanUser.setLastTryAt(user.getLastTryAt());
        beanUser.setVerificationCode(user.getVerificationCode());
        beanUser.setCreatedAt(user.getCreatedAt());
        beanUser.setUpdatedAt(user.getUpdatedAt());
        beanUser.setLastLoginAt(user.getLastLoginAt());
        beanUser.setLastActivityAt(user.getLastActivityAt());
        beanUser.setPasswordChangedAt(user.getPasswordChangedAt());

        return beanUser;
    }

    /**
     * Mapea ERole (infraestructura) a Role (dominio)
     */
    private Role mapToRole(ERole eRole) {
        if (eRole == null) return null;
        
        switch (eRole) {
            case ADMINISTRADOR:
                return Role.ADMINISTRADOR;
            case ALMACENISTA:
                return Role.ALMACENISTA;
            case AUXILIAR:
                return Role.AUXILIAR;
            case AUXILIAR_DE_CONTEO:
                return Role.AUXILIAR_DE_CONTEO;
            default:
                throw new IllegalArgumentException("ERole no soportado: " + eRole);
        }
    }

    /**
     * Mapea Role (dominio) a ERole (infraestructura)
     */
    public ERole mapToERole(Role role) {
        if (role == null) return null;
        
        switch (role) {
            case ADMINISTRADOR:
                return ERole.ADMINISTRADOR;
            case ALMACENISTA:
                return ERole.ALMACENISTA;
            case AUXILIAR:
                return ERole.AUXILIAR;
            case AUXILIAR_DE_CONTEO:
                return ERole.AUXILIAR_DE_CONTEO;
            default:
                throw new IllegalArgumentException("Role no soportado: " + role);
        }
    }
}