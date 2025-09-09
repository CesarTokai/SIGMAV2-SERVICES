package tokai.com.mx.SIGMAV2.modules.inventory.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "periods")
public class Period {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_period")
    private Long idPeriod;
    
    @Column(name = "period", unique = true, nullable = false)
    private LocalDate period;
    
    @Column(name = "comments", nullable = false)
    private String comments = "";
    
    // Constructors
    public Period() {}
    
    public Period(LocalDate period, String comments) {
        this.period = period;
        this.comments = comments;
    }
    
    // Getters and Setters
    public Long getIdPeriod() {
        return idPeriod;
    }
    
    public void setIdPeriod(Long idPeriod) {
        this.idPeriod = idPeriod;
    }
    
    public LocalDate getPeriod() {
        return period;
    }
    
    public void setPeriod(LocalDate period) {
        this.period = period;
    }
    
    public String getComments() {
        return comments;
    }
    
    public void setComments(String comments) {
        this.comments = comments;
    }
}