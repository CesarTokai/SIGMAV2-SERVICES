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
public class Warehouse {

    private Long id;
    private String warehouseKey;
    private String nameWarehouse;
    private String observations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    
    // Auditoría
    private Long createdBy;
    private Long updatedBy;
    private Long deletedBy;
    
    // Información adicional
    private String createdByEmail;
    private String updatedByEmail;
    
    // Estadísticas
    private Long assignedUsersCount;
    private Long inventoryItemsCount;
    
    // Métodos de conveniencia
    public boolean isDeleted() {
        return deletedAt != null;
    }
    
    public boolean isActive() {
        return deletedAt == null;
    }
}