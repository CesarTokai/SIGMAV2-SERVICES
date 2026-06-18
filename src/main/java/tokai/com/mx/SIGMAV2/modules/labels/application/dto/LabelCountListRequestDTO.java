package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar la lista de marbetes disponibles para conteo o reimpresión.
 *
 * NOTA: warehouseId es OPCIONAL para todos los roles.
 * Si se proporciona, retorna marbetes de ese almacén específico.
 * Si es null, retorna marbetes de TODOS los almacenes del período.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelCountListRequestDTO {

    @NotNull(message = "El ID del periodo es obligatorio")
    private Long periodId;

    /**
     * Almacén (opcional para todos los roles)
     * Si no se proporciona, se retornan marbetes de todos los almacenes del período
     */
    private Long warehouseId;

    /**
     * Folios específicos a consultar (separados por comas)
     * Ej: "12,30,50" - si se proporciona, retorna solo esos folios
     * Si es null, retorna todos los marbetes del período/almacén
     */
    private String folios;
}

