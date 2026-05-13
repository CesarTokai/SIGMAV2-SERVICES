package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Label {
    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long periodId;
    private LocalDateTime createdAt;

}