package tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

@Data
public class CreatePeriodDTO {
    @NotNull(message = "El campo 'date' es obligatorio y debe tener formato YYYY-MM-DD")
    private LocalDate date;
    private String comments;
}
