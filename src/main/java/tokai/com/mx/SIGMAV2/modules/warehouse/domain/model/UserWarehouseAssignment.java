package tokai.com.mx.SIGMAV2.modules.warehouse.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad que representa la asignación de un usuario a un almacén.
 * Implementa las reglas de negocio de contexto informativo del módulo de Marbetes.
 */
@Entity
@Table(name = "user_warehouse_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UserWarehouseAssignment.UserWarehouseId.class)
public class UserWarehouseAssignment {

    @Id
    @Column(name = "id_user", nullable = false)
    private Long userId;

    @Id
    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Clase para clave primaria compuesta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWarehouseId implements Serializable {
        private Long userId;
        private Long warehouseId;
    }
}
