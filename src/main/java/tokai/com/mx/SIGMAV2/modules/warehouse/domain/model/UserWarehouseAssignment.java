package tokai.com.mx.SIGMAV2.modules.warehouse.domain.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class UserWarehouseAssignment {
    private Long id;
    private Long userId;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseKey;
    private Long assignedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
