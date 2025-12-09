package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para actualizar un conteo existente (C1 o C2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCountDTO {

    @NotNull(message = "El folio es obligatorio")
    private Long folio;

    @NotNull(message = "El valor del conteo es obligatorio")
    private BigDecimal countedValue;

    private String observaciones;
}

