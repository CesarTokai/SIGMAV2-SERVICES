package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de generación de archivo TXT de existencias.
 * fileBytes contiene el contenido del archivo para descarga directa (se ignora en JSON).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFileResponseDTO {

    private String fileName;
    private String filePath;
    private Integer totalProductos;
    private String mensaje;

    /** Contenido del archivo para descarga directa. No se serializa en JSON. */
    @JsonIgnore
    private byte[] fileBytes;
}

