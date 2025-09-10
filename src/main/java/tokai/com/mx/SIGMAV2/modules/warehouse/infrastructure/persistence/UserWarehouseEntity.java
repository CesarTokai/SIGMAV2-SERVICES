
package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_warehouses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_warehouse", columnNames = {"user_id", "warehouse_id"})
        },
        indexes = {
                @Index(name = "idx_uw_user", columnList = "user_id"),
                @Index(name = "idx_uw_warehouse", columnList = "warehouse_id"),
                @Index(name = "idx_uw_assigned_by", columnList = "assigned_by")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWarehouseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, foreignKey = @ForeignKey(name = "fk_uw_warehouse"))
    private WarehouseEntity warehouse;

    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserWarehouseEntity(Long userId, WarehouseEntity warehouse, Long assignedBy) {
        this.userId = userId;
        this.warehouse = warehouse;
        this.assignedBy = assignedBy;

        // Inicializar fechas en caso de que la auditoría no esté configurada
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }

    // Este método se usará solo si la auditoría JPA no está configurada
    @PreUpdate
    protected void onUpdate() {
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }
}