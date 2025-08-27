package tokai.com.mx.SIGMAV2.modules.users.domain.model;

import java.time.LocalDateTime;

/**
 * Entidad de dominio pura - Sin dependencias de frameworks externos
 */
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

    // Getters y Setters
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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
