package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

// Clase POJO de utilidad para el módulo inventory; no es una entidad JPA para evitar colisiones con el módulo periods.

import java.time.LocalDate;

public class PeriodEntity {

    private Long id;
    private String state;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate periodDate;
    private String comments;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getPeriodDate() { return periodDate; }
    public void setPeriodDate(LocalDate periodDate) { this.periodDate = periodDate; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
