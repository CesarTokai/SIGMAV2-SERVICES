package com.sigma.dto;

import lombok.Data;
import java.util.Map;

@Data
public class StockMatrixRow {
    private Long productId;
    private String productName;
    private String unit;
    private Map<Long, Integer> warehouseQuantities;  // Map<WarehouseId, Quantity>
}
