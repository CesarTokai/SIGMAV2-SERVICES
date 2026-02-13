package tokai.com.mx.SIGMAV2.security.infrastructure.adapter;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

/**
 * Adaptador para el módulo de seguridad
 * Convierte entre el modelo de dominio y el modelo legacy usado en seguridad
 */
@Component
public class SecurityUserAdapter {

    /**
     * Convierte de modelo de dominio a BeanUser legacy
     */
    public BeanUser toLegacyUser(User domainUser) {
        if (domainUser == null) return null;
        
        BeanUser beanUser = new BeanUser();
        beanUser.setId(domainUser.getId());
        beanUser.setEmail(domainUser.getEmail());
        beanUser.setPasswordHash(domainUser.getPasswordHash());
        beanUser.setRole(mapRoleToLegacy(domainUser.getRole()));
        beanUser.setStatus(domainUser.isStatus());
        beanUser.setVerified(domainUser.isVerified());
        beanUser.setAttempts(domainUser.getAttempts());
        beanUser.setLastTryAt(domainUser.getLastTryAt());
        beanUser.setVerificationCode(domainUser.getVerificationCode());
        beanUser.setCreatedAt(domainUser.getCreatedAt());
        beanUser.setUpdatedAt(domainUser.getUpdatedAt());
        beanUser.setLastLoginAt(domainUser.getLastLoginAt());
        beanUser.setLastActivityAt(domainUser.getLastActivityAt());
        beanUser.setPasswordChangedAt(domainUser.getPasswordChangedAt());

        return beanUser;
    }

    /**
     * Convierte de BeanUser legacy a modelo de dominio
     */
    public User toDomainUser(BeanUser beanUser) {
        if (beanUser == null) return null;
        
        User user = new User(
                beanUser.getId(),
                beanUser.getEmail(),
                beanUser.getPasswordHash(),
                mapRoleToDomain(beanUser.getRole()),
                beanUser.isStatus(),
                beanUser.isVerified(),
                beanUser.getAttempts(),
                beanUser.getLastTryAt(),
                beanUser.getVerificationCode(),
                beanUser.getCreatedAt(),
                beanUser.getUpdatedAt()
        );

        // Mapear campos adicionales que no están en el constructor
        user.setLastLoginAt(beanUser.getLastLoginAt());
        user.setLastActivityAt(beanUser.getLastActivityAt());
        user.setPasswordChangedAt(beanUser.getPasswordChangedAt());

        return user;
    }

    private ERole mapRoleToLegacy(Role domainRole) {
        if (domainRole == null) return null;
        
        switch (domainRole) {
            case ADMINISTRADOR: return ERole.ADMINISTRADOR;
            case ALMACENISTA: return ERole.ALMACENISTA;
            case AUXILIAR: return ERole.AUXILIAR;
            case AUXILIAR_DE_CONTEO: return ERole.AUXILIAR_DE_CONTEO;
            default: throw new IllegalArgumentException("Rol no válido: " + domainRole);
        }
    }

    private Role mapRoleToDomain(ERole legacyRole) {
        if (legacyRole == null) return null;
        
        switch (legacyRole) {
            case ADMINISTRADOR: return Role.ADMINISTRADOR;
            case ALMACENISTA: return Role.ALMACENISTA;
            case AUXILIAR: return Role.AUXILIAR;
            case AUXILIAR_DE_CONTEO: return Role.AUXILIAR_DE_CONTEO;
            default: throw new IllegalArgumentException("Rol no válido: " + legacyRole);
        }
    }
}
