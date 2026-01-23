package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Period {

    public enum State { DRAFT, OPEN, CLOSED, LOCKED }

    private Long id;
    private LocalDate periodDate;
    private String comments;
    private State state;

    public Period() {}

    public Period(Long id, LocalDate periodDate, String comments, State state) {
        this.id = id;
        this.periodDate = periodDate;
        this.comments = comments;
        this.state = state;
    }

    public static Period of(Long id, LocalDate periodDate, String comments, State state) {
        return new Period(id, periodDate, comments, state);
    }


}
