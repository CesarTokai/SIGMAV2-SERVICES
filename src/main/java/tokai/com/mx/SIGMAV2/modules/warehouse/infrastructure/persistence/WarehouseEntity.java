package tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "warehouse",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_warehouse_key", columnNames = "warehouse_key"),
        @UniqueConstraint(name = "uk_warehouse_name", columnNames = "name_warehouse")
    },
    indexes = {
        @Index(name = "idx_warehouse_key", columnList = "warehouse_key"),
        @Index(name = "idx_warehouse_deleted", columnList = "deleted_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_warehouse")
    private Long id;

    @Column(name = "warehouse_key", nullable = false, length = 50)
    private String warehouseKey;

    @Column(name = "name_warehouse", nullable = false)
    private String nameWarehouse;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Auditoría simplificada (solo IDs)
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "deleted_by")
    private Long deletedBy;

    // Relación con usuarios asignados
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserWarehouseEntity> userAssignments;

    // Métodos de conveniencia
    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted(Long deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
    }
}