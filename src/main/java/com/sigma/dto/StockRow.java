package com.sigma.dto;

import lombok.Data;

@Data
public class StockRow {
    private Long warehouseId;
    private String warehouseName;
    private Double quantity;
    private Double minimumStock;
    private Double maximumStock;
}

