
package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class InventorySnapshot {
    private Long id;
    private Product product;
    private Warehouse warehouse;
    private Period period;
    private BigDecimal existQty;
    private LocalDateTime createdAt;



    public InventorySnapshot() {
    }
}
