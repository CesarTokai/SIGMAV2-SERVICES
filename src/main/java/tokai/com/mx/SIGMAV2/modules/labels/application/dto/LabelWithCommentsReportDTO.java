package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * DTO para Reporte de Marbetes con Comentarios en lista
 * Diseñado para consultas y reportes que muestren:
 * - Información del marbete
 * - Conteos C1 y C2
 * - Comentarios de los conteos
 * 
 * @JsonInclude(Include.NON_NULL) excluye campos null de la respuesta JSON
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelWithCommentsReportDTO {

    // ═══════════════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL MARBETE
    // ═══════════════════════════════════════════════════════════════════════
    private Long folio;
    private String estado;
    private LocalDateTime createdAt;
    private Boolean impreso;
    private Boolean cancelado;

    // ═══════════════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL PRODUCTO
    // ═══════════════════════════════════════════════════════════════════════
    private String claveProducto;
    private String nombreProducto;
    private String unidadMedida;
    private String descripcionProducto;

    // ═══════════════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL ALMACÉN
    // ═══════════════════════════════════════════════════════════════════════
    private String claveAlmacen;
    private String nombreAlmacen;

    // ═══════════════════════════════════════════════════════════════════════
    // INFORMACIÓN DEL PERÍODO
    // ═══════════════════════════════════════════════════════════════════════
    private Long periodId;
    private String periodName;

    // ═══════════════════════════════════════════════════════════════════════
    // EXISTENCIAS TEÓRICAS
    // ═══════════════════════════════════════════════════════════════════════
    private BigDecimal existenciasTeoricas;

    // ═══════════════════════════════════════════════════════════════════════
    // CONTEO C1 - CON COMENTARIO
    // ═══════════════════════════════════════════════════════════════════════
    private BigDecimal conteo1Valor;
    private LocalDateTime conteo1Fecha;
    private String conteo1UsuarioEmail;
    private String conteo1UsuarioNombre;
    private String conteo1Comentario;  // ← NUEVO: Comentario del C1

    // ═══════════════════════════════════════════════════════════════════════
    // CONTEO C2 - CON COMENTARIO
    // ═══════════════════════════════════════════════════════════════════════
    private BigDecimal conteo2Valor;
    private LocalDateTime conteo2Fecha;
    private String conteo2UsuarioEmail;
    private String conteo2UsuarioNombre;
    private String conteo2Comentario;  // ← NUEVO: Comentario del C2

    // ═══════════════════════════════════════════════════════════════════════
    // ANÁLISIS DE DIFERENCIA
    // ═══════════════════════════════════════════════════════════════════════
    private BigDecimal diferencia;
    private String diferenciaPorcentaje;
    private Boolean conteoCompleto;
    private String statusConteo;

    // ═══════════════════════════════════════════════════════════════════════
    // RESUMEN
    // ═══════════════════════════════════════════════════════════════════════
    private String resumenEstado;
    private Integer proximoAccion;
}

