package tokai.com.mx.SIGMAV2.modules.warehouse.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWarehouseAssignment {

    private Long id;
    private Long userId;
    private Long warehouseId;
    private Long assignedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Información adicional
    private String userEmail;
    private String warehouseName;
    private String warehouseKey;
    private String assignedByEmail;
    private String reason;
}