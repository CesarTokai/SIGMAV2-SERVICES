package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuesta completa de usuario en operaciones de administrador
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    
    private Long id;
    private String email;
    private String role;
    private boolean status;
    private boolean verified;
    private int attempts;
    private LocalDateTime lastTryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Información adicional para admin
    private String verificationCode;
    private int totalVerificationCodes;
    private LocalDateTime lastVerificationCodeSent;
    private boolean accountLocked;

    // Información de la cuenta y actividad
    private String comments;
    private List<String> assignedWarehouses;
    private boolean isSessionActive;
    private LocalDateTime lastActivityAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastAccountLockAt;
    private LocalDateTime lastPasswordChangeAt;
}
