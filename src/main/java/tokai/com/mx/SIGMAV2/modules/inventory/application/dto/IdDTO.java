package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IdDTO {
    @NotNull
    private Long id;
}
