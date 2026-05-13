package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.Data;

@Data
public class MultiWarehouseExportDTO {
    private String search;
    private String orderBy;
    private Boolean ascending;
    // Puedes agregar filtros adicionales si es necesario
}

