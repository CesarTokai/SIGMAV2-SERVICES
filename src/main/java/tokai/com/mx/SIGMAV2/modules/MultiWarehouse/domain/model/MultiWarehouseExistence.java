package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "multiwarehouse_existences",
        indexes = {
                @Index(name = "idx_warehouse_product", columnList = "warehouse_id,product_code")
        })
@Data
public class MultiWarehouseExistence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "warehouse_key")
    private String warehouseKey; // CVE_ALM - Clave del almacén

    @Column(name = "warehouse_name")
    private String warehouseName; // Nombre del almacén

    @Column(name = "product_code", nullable = false)
    private String productCode; // CVE_ART - Clave del producto

    @Column(name = "product_name")
    private String productName; // DESCR - Descripción del producto

    @Column(name = "stock", nullable = false)
    private BigDecimal stock;

    @Column(name = "status", length = 1)
    private String status; // A = Alta, B = Baja
}

