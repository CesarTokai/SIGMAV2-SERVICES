package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de generación de archivo TXT de existencias.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFileRequestDTO {

    @NotNull(message = "El ID del periodo es obligatorio")
    private Long periodId;
}

