package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.Data;
import lombok.Builder;

/**
 * DTO para filtrar y paginar la lista de marbetes
 */
@Data
@Builder
public class LabelListFilterDTO {
    
    // Filtros
    private Long periodId;
    private Long warehouseId;
    private Long productId;
    private String estado; // GENERADO, IMPRESO, CANCELADO
    private Boolean impreso;
    private Boolean conteoCompleto;
    private Boolean cancelado;
    
    // Búsqueda de texto
    private String searchText; // Busca en folio, producto, almacén
    
    // Paginación
    private int page; // default: 0
    private int size; // default: 20
    
    // Ordenamiento
    private String sortBy; // folio, createdAt, estado, producto, almacen
    private String sortDirection; // ASC, DESC
}

