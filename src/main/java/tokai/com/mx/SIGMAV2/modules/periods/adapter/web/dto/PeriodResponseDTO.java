package tokai.com.mx.SIGMAV2.modules.periods.adapter.web.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period.PeriodState;

@Data
@Builder
public class PeriodResponseDTO {
    private Long id;
    private LocalDate date;
    private String comments;
    private PeriodState state;
    private boolean hasDependencies;
}
