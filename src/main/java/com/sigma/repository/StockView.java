package com.sigma.repository;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "v_inventory_multiwarehouse")
@SqlResultSetMapping(
    name = "StockViewMapping",
    classes = @ConstructorResult(
        targetClass = StockView.class,
        columns = {
            @ColumnResult(name = "id", type = Long.class),
            @ColumnResult(name = "product_id", type = Long.class),
            @ColumnResult(name = "product_name", type = String.class),
            @ColumnResult(name = "warehouse_id", type = Long.class),
            @ColumnResult(name = "warehouse_name", type = String.class),
            @ColumnResult(name = "quantity", type = Integer.class),
            @ColumnResult(name = "unit", type = String.class)
        }
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockView {
    @Id
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    private Integer quantity;

    private String unit;
}
