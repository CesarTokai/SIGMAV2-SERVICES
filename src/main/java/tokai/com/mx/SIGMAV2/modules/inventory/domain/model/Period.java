package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import java.time.LocalDate;

public class Period {
    public enum State { DRAFT, OPEN, CLOSED, LOCKED }

    private Long id;
    private LocalDate periodDate;
    private String comments;
    private State state;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getPeriodDate() { return periodDate; }
    public void setPeriodDate(LocalDate periodDate) { this.periodDate = periodDate; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
}
