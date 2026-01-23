package tokai.com.mx.SIGMAV2.modules.users.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

