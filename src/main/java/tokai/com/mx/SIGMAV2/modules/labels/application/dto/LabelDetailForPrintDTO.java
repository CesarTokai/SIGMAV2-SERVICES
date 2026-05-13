package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO con información de un marbete para consulta antes de imprimir
 * Usado cuando el usuario quiere ver los datos de los marbetes antes de imprimirlos
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelDetailForPrintDTO {
    
    // DATOS DEL MARBETE
    private Long folio;
    private String estado;
    
    // PRODUCTO
    private Long productId;
    private String claveProducto;
    private String nombreProducto;
    private String unidadMedida;
    
    // ALMACÉN
    private Long warehouseId;
    private String claveAlmacen;
    private String nombreAlmacen;
    
    // PERÍODO
    private Long periodId;
    private java.time.LocalDate periodDate;
    
    // EXISTENCIAS
    private BigDecimal existenciasTeoricas;
    
    // CONTEOS
    private BigDecimal conteo1Valor;
    private BigDecimal conteo2Valor;
    private BigDecimal diferencia;
    
    // USUARIO QUE REGISTRÓ
    private String createdByEmail;
    private String createdByFullName;
    
    // ESTADO GENERAL
    private String resumenEstado;
    private String statusConteo;
    
    // MENSAJE PARA EL USUARIO
    private String mensaje; // ej: "Marbete 1 de 5", o advertencias
}

