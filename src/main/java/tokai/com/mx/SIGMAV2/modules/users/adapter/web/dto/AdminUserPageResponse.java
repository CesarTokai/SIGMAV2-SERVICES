package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta paginada de usuarios
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserPageResponse {
    
    private List<AdminUserResponse> users;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // Estadísticas adicionales
    private long totalVerifiedUsers;
    private long totalUnverifiedUsers;
    private long totalActiveUsers;
    private long totalInactiveUsers;
}
