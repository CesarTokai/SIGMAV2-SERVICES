package tokai.com.mx.SIGMAV2.modules.warehouse.domain.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class Warehouse {
    private Long id;
    private String warehouseKey;
    private String nameWarehouse;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long createdBy;
    private Long updatedBy;
    private Long deletedBy;
    private Long assignedUsersCount;
    private boolean deleted;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
