package tokai.com.mx.SIGMAV2.modules.users.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;


@Entity
@Table(name = "users")
public class BeanUser {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "user_id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private ERole role;

    @Column(name = "status", nullable = false)
    private boolean status;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_try_at")
    private LocalDateTime lastTryAt;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public ERole getRole() {
        return role;
    }

    public void setRole(ERole role) {
        this.role = role;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean isVerified) {
        this.isVerified = isVerified;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getLastTryAt() {
        return lastTryAt;
    }

    public void setLastTryAt(LocalDateTime lastTryAt) {
        this.lastTryAt = lastTryAt;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    
    @Override
    public String toString() {
        return "BeanUser [id=" + id + ", email=" + email + ", passwordHash=" + passwordHash + ", role=" + role
                + ", status=" + status + ", isVerified=" + isVerified + ", attempts=" + attempts + ", lastTryAt="
                + lastTryAt + ", verificationCode=" + verificationCode + ", createdAt=" + createdAt + ", updatedAt="
                + updatedAt + "]";
    }

    public BeanUser(Long id, String email, String passwordHash, ERole role, boolean status, boolean isVerified,
            int attempts, LocalDateTime lastTryAt, String verificationCode, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.isVerified = isVerified;
        this.attempts = attempts;
        this.lastTryAt = lastTryAt;
        this.verificationCode = verificationCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public BeanUser() {
    }



    
}

