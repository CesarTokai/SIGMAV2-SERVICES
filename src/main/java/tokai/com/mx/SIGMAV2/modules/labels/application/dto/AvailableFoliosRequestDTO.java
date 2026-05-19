package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar folios disponibles en un período.
 * El almacenista no necesita especificar warehouseId; el backend obtiene sus almacenes asignados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableFoliosRequestDTO {
    
    @NotNull(message = "El periodo es obligatorio")
    private Long periodId;
}

