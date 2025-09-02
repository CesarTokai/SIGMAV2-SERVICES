package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO para operaciones masivas en usuarios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserActionRequest {
    
    @NotEmpty(message = "La lista de IDs de usuarios no puede estar vacía")
    private List<Long> userIds;
    
    @NotNull(message = "La acción es obligatoria")
    private BulkAction action;
    
    // Parámetros opcionales según la acción
    private String newRole; // Para CHANGE_ROLE
    private Boolean newStatus; // Para CHANGE_STATUS
    private String reason; // Motivo de la acción masiva
    
    public enum BulkAction {
        FORCE_VERIFY,
        RESET_ATTEMPTS,
        ACTIVATE,
        DEACTIVATE,
        DELETE,
        CHANGE_ROLE,
        RESEND_VERIFICATION
    }
}
