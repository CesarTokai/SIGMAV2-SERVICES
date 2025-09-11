package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.Data;

@Data
public class MultiWarehouseSearchDTO {
    private String search;
    private String orderBy;
    private Boolean ascending;
    // Puedes agregar más filtros según necesidades del negocio
}

