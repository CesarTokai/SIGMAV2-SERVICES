package com.sigma.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StockMatrixRow {
    private Long productId;
    private String productName;
    private String sku;
    private Map<Long, Double> stockByWarehouse; // warehouseId -> quantity
}

