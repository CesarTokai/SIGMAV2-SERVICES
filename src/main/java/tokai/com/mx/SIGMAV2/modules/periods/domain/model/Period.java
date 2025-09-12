package tokai.com.mx.SIGMAV2.modules.periods.domain.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
public class Period {
    private Long id;
    private LocalDate date;
    private String comments;
    private PeriodState state;

    public enum PeriodState {
        DRAFT,
        OPEN,
        CLOSED,
        LOCKED
    }

    public LocalDate normalizeDate() {
        return date != null ? date.withDayOfMonth(1) : null;
    }
}
