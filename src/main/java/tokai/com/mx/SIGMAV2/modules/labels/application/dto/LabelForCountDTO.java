package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para mostrar la información de un marbete en la interfaz de conteo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelForCountDTO {

    private Long folio;
    private Long periodId;
    private Long warehouseId;
    private String claveAlmacen;
    private String nombreAlmacen;
    private String claveProducto;
    private String descripcionProducto;
    private String unidadMedida;
    private Boolean cancelado;
    private BigDecimal conteo1;
    private BigDecimal conteo2;
    private BigDecimal diferencia;
    private String estado;
    private Boolean impreso;
    private String mensaje;
    private BigDecimal existQty;
    private String existQtyUnidad;
    
    // Comentarios de conteos
    private String conteo1Comentario;
    private String conteo1UsuarioNombre;
    private String conteo2Comentario;
    private String conteo2UsuarioNombre;
}

