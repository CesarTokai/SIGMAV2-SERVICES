package com.sigma.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductStockView {
    private Long productId;
    private String productName;
    private String unit;
    private List<WarehouseStock> warehouseStocks;

    @Data
    public static class WarehouseStock {
        private Long warehouseId;
        private String warehouseName;
        private Integer quantity;
    }
}
