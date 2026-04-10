package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para buscar marbete por folio, periodo y almacén (POST).
 * 
 * NOTA: warehouseId es OPCIONAL para AUXILIAR_DE_CONTEO que tiene acceso a todos los almacenes.
 * Para otros roles (ALMACENISTA, AUXILIAR), warehouseId es requerido para validar acceso.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelForCountRequestDTO {
    @NotNull(message = "El folio es obligatorio")
    private Long folio;
    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;
    
    /**
     * Almacén (opcional para AUXILIAR_DE_CONTEO)
     * Si no se proporciona, el sistema buscará el marbete por folio y periodo
     */
    private Long warehouseId;
}

