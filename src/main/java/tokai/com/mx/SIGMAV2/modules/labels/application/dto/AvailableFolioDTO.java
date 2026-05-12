package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa un folio disponible para conteo.
 * Muestra si ya tiene conteos C1/C2 registrados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableFolioDTO {
    
    private Long folio;
    private String nombreAlmacen;
    private String claveProducto;
    private String descripcionProducto;
    private String unidadMedida;
    private Boolean tieneC1;      // true si C1 ya fue registrado
    private Boolean tieneC2;      // true si C2 ya fue registrado
    private String estado;        // ACTIVO, CANCELADO, etc.
    private Boolean impreso;      // Si fue impreso
}

