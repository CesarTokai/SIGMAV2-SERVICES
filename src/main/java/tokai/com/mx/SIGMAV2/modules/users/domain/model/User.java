package tokai.com.mx.SIGMAV2.modules.users.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean status;
    private boolean verified;
    private int attempts;
    private LocalDateTime lastTryAt;
    private String verificationCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime passwordChangedAt;

    // Constructor por defecto
    public User() {}

    // Constructor completo
    public User(Long id, String email, String passwordHash, Role role, 
                boolean status, boolean verified, int attempts, 
                LocalDateTime lastTryAt, String verificationCode, 
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.verified = verified;
        this.attempts = attempts;
        this.lastTryAt = lastTryAt;
        this.verificationCode = verificationCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    // Métodos de dominio
    public void markAsVerified() {
        this.verified = true;
        this.verificationCode = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementAttempts() {
        this.attempts++;
        this.lastTryAt = LocalDateTime.now();
        if (this.attempts >= 3) {
            this.status = false;
            this.attempts = 0;
        }
    }

    public void resetAttempts() {
        this.attempts = 0;
        this.status = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isBlocked() {
        return !this.status && this.lastTryAt != null && 
               this.lastTryAt.plusMinutes(5).isAfter(LocalDateTime.now());
    }
}
