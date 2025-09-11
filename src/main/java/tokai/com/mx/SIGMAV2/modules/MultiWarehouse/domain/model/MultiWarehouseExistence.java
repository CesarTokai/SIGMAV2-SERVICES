package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "multiwarehouse_existences")
@Data
public class MultiWarehouseExistence {
    @Id
    private Long id;

    private Long warehouseId;
    private String warehouseName;
    private String productCode;
    private String productName;
    private BigDecimal stock;
    private String status; // A = Alta, B = Baja
}

