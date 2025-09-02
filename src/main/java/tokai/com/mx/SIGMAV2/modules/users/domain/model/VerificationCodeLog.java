package tokai.com.mx.SIGMAV2.modules.users.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entidad para rastrear reenvíos de códigos de verificación
 */
@Entity
@Table(name = "verification_code_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String verificationCode;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodeStatus status;
    
    @Column
    private String requestReason;
    
    public enum CodeStatus {
        ACTIVE,
        USED,
        EXPIRED,
        REPLACED
    }
    
    // Constructor de conveniencia
    public VerificationCodeLog(String email, String verificationCode, String requestReason) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.requestReason = requestReason;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(24); // 24 horas de validez
        this.status = CodeStatus.ACTIVE;
    }
    
    /**
     * Verifica si el código ha expirado
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * Marca el código como usado
     */
    public void markAsUsed() {
        this.status = CodeStatus.USED;
    }
    
    /**
     * Marca el código como expirado
     */
    public void markAsExpired() {
        this.status = CodeStatus.EXPIRED;
    }
    
    /**
     * Marca el código como reemplazado
     */
    public void markAsReplaced() {
        this.status = CodeStatus.REPLACED;
    }
}
