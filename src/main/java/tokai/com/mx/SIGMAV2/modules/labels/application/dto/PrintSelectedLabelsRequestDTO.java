package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * DTO para solicitar impresión de marbetes específicos con su información
 */
@Data
@Builder
public class PrintSelectedLabelsRequestDTO {
    
    /**
     * Lista de folios que el usuario desea imprimir
     */
    private List<Long> folios;
    
    /**
     * Período del cual son los marbetes
     */
    private Long periodId;
    
    /**
     * Almacén del cual son los marbetes
     */
    private Long warehouseId;
    
    /**
     * Tipo de información a incluir en la impresión
     * "COMPLETA" = Toda la información (conteos, existencias, etc)
     * "BASICA" = Solo lo esencial (folio, producto, almacén)
     */
    private String infoType; // COMPLETA, BASICA (default: BASICA)
}

