package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.Data;

@Data
public class LabelSummaryRequestDTO {
    private Long periodId;
    private Long warehouseId;

    // Paginación
    private Integer page = 0;
    private Integer size = 10; // Default 10 registros por página (10, 25, 50, 100)

    // Búsqueda
    private String searchText;

    // Ordenamiento
    private String sortBy = "claveProducto"; // Default ordenar por clave de producto
    private String sortDirection = "ASC"; // ASC o DESC
}
