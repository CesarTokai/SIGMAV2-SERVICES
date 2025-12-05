package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LabelSummaryResponseDTO {
    private Long productId;
    private String claveProducto;
    private String nombreProducto;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Integer foliosSolicitados;
    private Integer foliosExistentes;
    private String estado;
    private Integer existencias;
    private Boolean impreso; // true si está impreso
    private String fechaImpresion; // fecha de impresión en formato ISO

    // NUEVOS CAMPOS para rangos de folios
    private Long primerFolio; // Primer folio generado para este producto
    private Long ultimoFolio; // Último folio generado para este producto
    private List<Long> folios; // Lista de todos los folios individuales
}
