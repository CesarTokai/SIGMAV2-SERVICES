package tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

@Data
public class CreatePeriodDTO {
    @NotNull(message = "El campo 'date' es obligatorio y debe tener formato YYYY-MM-DD")
    private LocalDate date;
    private String comments;
    private BeanUser user;


}
