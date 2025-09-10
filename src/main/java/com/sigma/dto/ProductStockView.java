package com.sigma.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductStockView {
    private Long productId;
    private String productName;
    private String sku;
    private List<StockRow> stockByWarehouse;
}

