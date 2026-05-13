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

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "last_blocked_at")
    private LocalDateTime lastBlockedAt;

    // ═══ Información Personal (consolidada en users) ═══
    @Column(name = "name")
    private String name;

    @Column(name = "first_last_name")
    private String firstLastName;

    @Column(name = "second_last_name")
    private String secondLastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "comments", columnDefinition = "LONGTEXT")
    private String comments;


    @Override
    public String toString() {
        return "BeanUser [id=" + id + ", email=" + email + ", passwordHash=" + passwordHash + ", role=" + role
                + ", status=" + status + ", isVerified=" + isVerified + ", attempts=" + attempts + ", lastTryAt="
                + lastTryAt + ", verificationCode=" + verificationCode + ", createdAt=" + createdAt + ", updatedAt="
                + updatedAt + ", name=" + name + ", firstLastName=" + firstLastName + ", secondLastName=" 
                + secondLastName + ", phoneNumber=" + phoneNumber + "]";
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

