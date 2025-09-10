package com.sigma.dto;

import lombok.Data;

@Data
public class StockRow {
    private Long productId;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private Integer quantity;
    private String unit;
}
