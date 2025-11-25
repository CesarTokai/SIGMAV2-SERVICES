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

    @Column(nullable = false)
    private Long periodId;

    @Id
    private Long id;
    @Column(nullable = false)
    private Long warehouseId;

    private String warehouseKey; // CVE_ALM - Clave del almacén

    private String warehouseName; // Nombre del almacén

    @Column(nullable = false)
    private String productCode; // CVE_ART - Clave del producto

    private String productName; // DESCR - Descripción del producto

    @Column(nullable = false)
    private BigDecimal stock;

    @Column(length = 1)
    private String status; // A = Alta, B = Baja
}

