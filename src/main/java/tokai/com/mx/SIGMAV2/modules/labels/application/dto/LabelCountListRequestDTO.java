package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar la lista de marbetes disponibles para conteo.
 * 
 * NOTA: warehouseId es OPCIONAL para AUXILIAR_DE_CONTEO que tiene acceso a todos los almacenes.
 * Para otros roles (ALMACENISTA, AUXILIAR), warehouseId es requerido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCountListRequestDTO {

    @NotNull(message = "El ID del periodo es obligatorio")
    private Long periodId;

    /**
     * Almacén (opcional para AUXILIAR_DE_CONTEO)
     * Si no se proporciona y el usuario es AUXILIAR_DE_CONTEO, se retornan marbetes de todos los almacenes
     */
    private Long warehouseId;
}

