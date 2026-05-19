package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateBatchResponseDTO {

    private int totalGenerados;
    private int generadosConExistencias;
    private int generadosSinExistencias;
    private long primerFolio;
    private long ultimoFolio;
    private String mensaje;
}

