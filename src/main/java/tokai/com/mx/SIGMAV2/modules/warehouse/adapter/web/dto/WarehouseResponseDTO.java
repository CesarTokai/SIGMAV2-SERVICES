package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WarehouseResponseDTO {
    private Long id;
    private String warehouseKey;
    private String nameWarehouse;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String createdByEmail;
    private String updatedByEmail;
    private Integer assignedUsersCount;
    private boolean deleted;
}
