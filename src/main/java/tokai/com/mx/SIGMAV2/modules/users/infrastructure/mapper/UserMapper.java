package tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper;

import org.springframework.stereotype.Component;


import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.UserEntity;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

@Component
public class UserMapper {

    public BeanUser toDomain(UserEntity entity) {
        if (entity == null) return null;
        return new BeanUser(
                entity.getUserId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isStatus(),
                entity.isVerified(),
                entity.getAttempts(),
                entity.getLastTryAt(),
                entity.getVerificationCode(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserEntity toEntity(BeanUser user) {
        if (user == null) return null;
        UserEntity entity = new UserEntity();
        entity.setUserId(user.getUserId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getRole());
        entity.setStatus(user.isStatus());
        entity.setVerified(user.isVerified());
        entity.setAttempts(user.getAttempts());
        entity.setLastTryAt(user.getLastTryAt());
        entity.setVerificationCode(user.getVerificationCode());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}