package tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.Role;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

/**
 * Mapper entre el modelo de dominio y las entidades de infraestructura
 */
@Component
public class UserDomainMapper {

    /**
     * Convierte de entidad JPA a modelo de dominio
     */
    public User toDomain(BeanUser entity) {
        if (entity == null) return null;
        
        User user = new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                mapRoleToDomain(entity.getRole()),
                entity.isStatus(),
                entity.isVerified(),
                entity.getAttempts(),
                entity.getLastTryAt(),
                entity.getVerificationCode(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );

        // Mapear campos de auditoría
        user.setLastLoginAt(entity.getLastLoginAt());
        user.setLastActivityAt(entity.getLastActivityAt());
        user.setPasswordChangedAt(entity.getPasswordChangedAt());

        return user;
    }

    /**
     * Convierte de modelo de dominio a entidad JPA
     */
    public BeanUser toEntity(User domain) {
        if (domain == null) return null;
        
        BeanUser entity = new BeanUser();
        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(mapRoleToEntity(domain.getRole()));
        entity.setStatus(domain.isStatus());
        entity.setVerified(domain.isVerified());
        entity.setAttempts(domain.getAttempts());
        entity.setLastTryAt(domain.getLastTryAt());
        entity.setVerificationCode(domain.getVerificationCode());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setLastLoginAt(domain.getLastLoginAt());
        entity.setLastActivityAt(domain.getLastActivityAt());
        entity.setPasswordChangedAt(domain.getPasswordChangedAt());

        return entity;
    }

    /**
     * Mapea Role del dominio a ERole de la entidad
     */
    public ERole mapRoleToEntity(Role domainRole) {
        if (domainRole == null) return null;
        
        switch (domainRole) {
            case ADMINISTRADOR: return ERole.ADMINISTRADOR;
            case ALMACENISTA: return ERole.ALMACENISTA;
            case AUXILIAR: return ERole.AUXILIAR;
            case AUXILIAR_DE_CONTEO: return ERole.AUXILIAR_DE_CONTEO;
            default: throw new IllegalArgumentException("Rol no válido: " + domainRole);
        }
    }

    /**
     * Mapea ERole de la entidad a Role del dominio
     */
    private Role mapRoleToDomain(ERole entityRole) {
        if (entityRole == null) return null;
        
        switch (entityRole) {
            case ADMINISTRADOR: return Role.ADMINISTRADOR;
            case ALMACENISTA: return Role.ALMACENISTA;
            case AUXILIAR: return Role.AUXILIAR;
            case AUXILIAR_DE_CONTEO: return Role.AUXILIAR_DE_CONTEO;
            default: throw new IllegalArgumentException("Rol no válido: " + entityRole);
        }
    }
}
