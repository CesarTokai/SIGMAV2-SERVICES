package tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;

@Entity
@Table(name = "users")
public class UserEntity {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "user_id")
private Long userId;

@Column(name = "email")
private String email;

@Column(name = "password_hash")
private String passwordHash;

@Enumerated(EnumType.STRING)
@Column(name = "role")
private ERole role;

@Column(name = "status")
private boolean status;

@Column(name = "is_verified")
private boolean isVerified;

@Column(name = "attempts")
private int attempts;

@Column(name = "last_try_at")
private LocalDateTime lastTryAt;

@Column(name = "verification_code")
private String verificationCode;

@Column(name = "created_at")
private LocalDateTime createdAt;

@Column(name = "updated_at")
private LocalDateTime updatedAt;


public Long getUserId() {
    return userId;
}
public String getEmail() {
    return email;
}
public String getPasswordHash() {
    return passwordHash;
}
public ERole getRole() {
    return role;
}
public boolean isStatus() {
    return status;
}
public boolean isVerified() {
    return isVerified;
}
public int getAttempts() {
    return attempts;
}
public LocalDateTime getLastTryAt() {
    return lastTryAt;
}
public String getVerificationCode() {
    return verificationCode;
}
public LocalDateTime getCreatedAt() {
    return createdAt;
}
public LocalDateTime getUpdatedAt() {
    return updatedAt;
}
public void setUserId(Long userId) {
    this.userId = userId;
}
public void setEmail(String email) {
    this.email = email;
}
public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
}
public void setRole(ERole role) {
    this.role = role;
}
public void setStatus(boolean status) {
    this.status = status;
}
public void setVerified(boolean isVerified) {
    this.isVerified = isVerified;
}
public void setAttempts(int attempts) {
    this.attempts = attempts;
}
public void setLastTryAt(LocalDateTime lastTryAt) {
    this.lastTryAt = lastTryAt;
}
public void setVerificationCode(String verificationCode) {
    this.verificationCode = verificationCode;
}
public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
}
public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
}
@Override
public String toString() {
    return "UserEntity [userId=" + userId + ", email=" + email + ", passwordHash=" + passwordHash + ", role=" + role
            + ", status=" + status + ", isVerified=" + isVerified + ", attempts=" + attempts + ", lastTryAt="
            + lastTryAt + ", verificationCode=" + verificationCode + ", createdAt=" + createdAt + ", updatedAt="
            + updatedAt + "]";
}



}


