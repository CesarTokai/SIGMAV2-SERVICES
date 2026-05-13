package tokai.com.mx.SIGMAV2.modules.users.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad para rastrear intentos fallidos de validación de código en recuperación de contraseña
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "password_reset_attempts")
public class BeanPasswordResetAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BeanUser user;

    @Column(name = "attempt_type", nullable = false)
    private String attemptType; // "CODE_VALIDATION" o "PASSWORD_VALIDATION"

    @Column(name = "is_successful", nullable = false)
    private boolean isSuccessful;

    @Column(name = "attempt_at", nullable = false)
    private LocalDateTime attemptAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "error_message")
    private String errorMessage;
}

