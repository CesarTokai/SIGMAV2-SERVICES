package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.Data;

@Data
public class MultiWarehouseSearchDTO {
    private String search;
    private String orderBy;
    private Boolean ascending;
    private Integer pageSize; // Para tamaños de paginación específicos: 10, 25, 50, 100
    // Filtros adicionales
    private Long periodId;       // ID del periodo (preferido)
    private String period;       // Periodo en formato MM-yyyy o yyyy-MM (se resolverá a periodId)
}
