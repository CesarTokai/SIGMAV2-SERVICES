package com.sigma.dto;

import lombok.Data;

@Data
public class StockChange {
    private Long productId;
    private Integer quantity;
    private String unit;
}
