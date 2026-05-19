package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InventoryPeriodReportDTO {
    private String cveArt;
    private String descr;
    private String uniMed;
    private BigDecimal existQty;
    private String status;
}

