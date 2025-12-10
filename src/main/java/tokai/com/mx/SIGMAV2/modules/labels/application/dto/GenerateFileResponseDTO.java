package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de generación de archivo TXT de existencias.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFileResponseDTO {

    private String fileName;
    private String filePath;
    private Integer totalProductos;
    private String mensaje;
}

