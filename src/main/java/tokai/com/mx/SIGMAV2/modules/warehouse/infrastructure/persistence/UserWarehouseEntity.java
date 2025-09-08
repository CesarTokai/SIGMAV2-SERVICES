package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_warehouses",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_warehouse", columnNames = {"user_id", "id_warehouse"})
    },
    indexes = {
        @Index(name = "idx_uw_user", columnList = "user_id"),
        @Index(name = "idx_uw_warehouse", columnList = "id_warehouse"),
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_warehouse", nullable = false, foreignKey = @ForeignKey(name = "fk_uw_warehouse"))
    private WarehouseEntity warehouse;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructor de conveniencia
    public UserWarehouseEntity(Long userId, WarehouseEntity warehouse, Long assignedBy) {
        this.userId = userId;
        this.warehouse = warehouse;
        this.assignedBy = assignedBy;
    }
}