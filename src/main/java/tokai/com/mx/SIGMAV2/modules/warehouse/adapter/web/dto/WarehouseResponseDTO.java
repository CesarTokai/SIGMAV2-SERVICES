package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponseDTO {

    private Long id;
    private String warehouseKey;
    private String nameWarehouse;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Información de auditoría (opcional)
    private String createdByEmail;
    private String updatedByEmail;
    
    // Estadísticas (opcional)
    private Long assignedUsersCount;
    private Long inventoryItemsCount;
}