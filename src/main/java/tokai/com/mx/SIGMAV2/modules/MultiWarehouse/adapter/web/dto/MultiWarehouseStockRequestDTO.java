package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.Data;

@Data
public class MultiWarehouseStockRequestDTO {
    private String productCode;
    private String warehouseKey;
    private Long periodId;
}

