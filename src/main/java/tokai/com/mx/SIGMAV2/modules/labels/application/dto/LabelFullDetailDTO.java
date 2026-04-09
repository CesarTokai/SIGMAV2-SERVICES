package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

/**
 * DTO Completo con TODA la información de un marbete
 * Incluye: datos del marbete, usuario, producto, almacén, período,
 * conteos, impresiones, cancelaciones, historial completo
 * 
 * NOTA: @JsonInclude(Include.NON_NULL) excluye automáticamente campos null
 * de la respuesta JSON, reduciendo significativamente el tamaño
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelFullDetailDTO {

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL MARBETE
    // ═══════════════════════════════════════════════════════════════
    private Long folio;
    private String estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime impresoAt;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL USUARIO QUE LO REGISTRÓ
    // ═══════════════════════════════════════════════════════════════
    private Long createdByUserId;
    private String createdByEmail;
    private String createdByFullName;
    private String createdByRole;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL PRODUCTO
    // ═══════════════════════════════════════════════════════════════
    private Long productId;
    private String claveProducto;
    private String nombreProducto;
    private String unidadMedida;
    private String descripcionProducto;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL ALMACÉN
    // ═══════════════════════════════════════════════════════════════
    private Long warehouseId;
    private String claveAlmacen;
    private String nombreAlmacen;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL PERÍODO
    // ═══════════════════════════════════════════════════════════════
    private Long periodId;
    private String periodName;
    private java.time.LocalDate periodDate;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DE EXISTENCIAS
    // ═══════════════════════════════════════════════════════════════
    private BigDecimal existenciasTeoricas;
    private String statusExistencias;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DE CONTEOS
    // ═══════════════════════════════════════════════════════════════
    private BigDecimal conteo1Valor;
    private LocalDateTime conteo1Fecha;
    private Long conteo1UsuarioId;
    private String conteo1UsuarioEmail;
    private String conteo1UsuarioNombre;
    private Integer conteo1Intentos;

    private BigDecimal conteo2Valor;
    private LocalDateTime conteo2Fecha;
    private Long conteo2UsuarioId;
    private String conteo2UsuarioEmail;
    private String conteo2UsuarioNombre;
    private Integer conteo2Intentos;

    private BigDecimal diferencia;
    private String diferenciaPorcentaje;
    private Boolean conteoCompleto;
    private String statusConteo;

    // ═══════════════════════════════════════════════════════════════
    // HISTORIAL DE CONTEOS (TODOS LOS CAMBIOS)
    // ═══════════════════════════════════════════════════════════════
    private List<CountEventHistoryDTO> countHistory;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DE IMPRESIÓN
    // ═══════════════════════════════════════════════════════════════
    private Boolean impreso;
    private LocalDateTime primeraImpresionAt;
    private Long primeraImpresionPorUserId;
    private String primeraImpresionPorEmail;
    private LocalDateTime ultimaReimpresionAt;
    private Long ultimaReimpresionPorUserId;
    private String ultimaReimpresionPorEmail;
    private Integer totalReimpresiones;
    private List<PrintEventDTO> printHistory;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DE CANCELACIÓN (SI APLICA)
    // ═══════════════════════════════════════════════════════════════
    private Boolean cancelado;
    private LocalDateTime canceladoAt;
    private Long canceladoPorUserId;
    private String canceladoPorEmail;
    private String motivoCancelacion;
    private BigDecimal existenciasAlCancelar;
    private BigDecimal existenciasActualesAlCancelar;
    private Boolean reactivado;
    private LocalDateTime reactivadoAt;
    private Long reactivadoPorUserId;
    private String reactivadoPorEmail;
    private String notas;

    // ═══════════════════════════════════════════════════════════════
    // INFORMACIÓN DE SOLICITUD DE FOLIOS
    // ═══════════════════════════════════════════════════════════════
    private Long labelRequestId;
    private Integer foliosSolicitados;
    private LocalDateTime folioSolicitadoAt;

    // ═══════════════════════════════════════════════════════════════
    // RESUMEN Y ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════
    private String resumenEstado;
    private String proximoAccion;
    private List<String> warnings;

    /**
     * DTO anidado para historial de conteos
     */
    @Data
    @Builder
    public static class CountEventHistoryDTO {
        private Integer countNumber;
        private BigDecimal value;
        private LocalDateTime recordedAt;
        private Long recordedByUserId;
        private String recordedByEmail;
        private String recordedByNombre;
        private String action; // "CREATED", "UPDATED"
        private String description;
    }

    /**
     * DTO anidado para historial de impresiones
     */
    @Data
    @Builder
    public static class PrintEventDTO {
        private LocalDateTime printedAt;
        private Long printedByUserId;
        private String printedByEmail;
        private String printedByNombre;
        private Boolean isExtraordinary;
        private String description;
    }
}

