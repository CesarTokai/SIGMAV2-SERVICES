package com.sigma.dto;

import lombok.Data;

@Data
public class StockChange {
    private Long productId;
    private Long warehouseId;
    private Double quantity;
    private String reason;
}

