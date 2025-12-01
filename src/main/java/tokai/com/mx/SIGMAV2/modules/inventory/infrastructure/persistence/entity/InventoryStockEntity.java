package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
 import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_stock",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_product", "id_warehouse", "id_period"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_stock")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_warehouse", nullable = false)
    private WarehouseEntity warehouse;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "exist_qty", nullable = false, precision = 10, scale = 2)
    private BigDecimal existQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 1)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        A, // Alta
        B  // Baja
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (existQty == null) {
            existQty = BigDecimal.ZERO;
        }
        if (status == null) {
            status = Status.A;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
