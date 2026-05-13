package tokai.com.mx.SIGMAV2.modules.users.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class User {
    private Long id;
    private String name;
    private String firstLastName;
    private String secondLastName;
    private String phoneNumber;
    private String comments;
    private String email;
    private String passwordHash;
    private String verificationCode;
    private Role role;
    private boolean status;
    private boolean verified;
    private int attempts;
    private LocalDateTime lastTryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastBlockedAt;

   


    // Constructor por defecto
    public User() {
    }

    // Constructor completo
    public User(Long id, String email, String passwordHash, Role role,
                boolean status, boolean verified, int attempts,
                LocalDateTime lastTryAt, String verificationCode,
                LocalDateTime createdAt, LocalDateTime updatedAt,
                String name, String firstLastName, String secondLastName,
                String phoneNumber, String comments) {
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
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
        this.comments = comments;
    }


    // Métodos de dominio
    public void markAsVerified() {
        this.verified = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementAttempts() {
        this.attempts++;
        this.lastTryAt = LocalDateTime.now();
        if (this.attempts >= 3) {
            this.status = false;
            this.lastBlockedAt = LocalDateTime.now();
            
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

    // ═══ Métodos de Información Personal ═══
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (name != null) fullName.append(name);
        if (firstLastName != null) fullName.append(" ").append(firstLastName);
        if (secondLastName != null) fullName.append(" ").append(secondLastName);
        return fullName.toString().trim();
    }

    public boolean hasCompletePersonalInfo() {
        return name != null && !name.trim().isEmpty() &&
                firstLastName != null && !firstLastName.trim().isEmpty();
    }

    public void updatePersonalInformation(String name, String firstLastName, String secondLastName, String phoneNumber) {
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
    }

}
