package tokai.com.mx.SIGMAV2.modules.users.infrastructure.entity;

import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.UserEntity;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

public class UserEntityHelper {

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


