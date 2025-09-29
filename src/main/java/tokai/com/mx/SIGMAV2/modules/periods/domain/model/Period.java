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

    public Period() {

    }


    public enum PeriodState {
        DRAFT,
        OPEN,
        CLOSED,
        LOCKED
    }

    // Constructor con 4 parámetros
    public Period(Long id, LocalDate date, String comments, PeriodState state) {
        this.id = id;
        this.date = date;
        this.comments = comments;
        this.state = state;
    }

    // Constructor con 3 parámetros (sin id)
    public Period(LocalDate date, String comments, PeriodState state) {
        this.date = date;
        this.comments = comments;
        this.state = state;
    }

    public static Period create(LocalDate date, String comments) {
        return new Period(date, comments, PeriodState.OPEN);
    }

    public LocalDate normalizeDate() {
        return date != null ? date.withDayOfMonth(1) : null;
    }
}
