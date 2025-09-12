package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "multiwarehouse_existences",
        indexes = {
                @Index(name = "idx_warehouse_product", columnList = "warehouseId,productCode")
        })
@Data
public class MultiWarehouseExistence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, unique = true)
    private Long id;

    @Column(nullable = false)
    private Long warehouseId;

    private String warehouseName;

    @Column(nullable = false)
    private String productCode;

    private String productName;

    @Column(nullable = false)
    private BigDecimal stock;

    @Column(length = 1)
    private String status; // A = Alta, B = Baja
}

