package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "label_count_events", uniqueConstraints = {@UniqueConstraint(columnNames = {"folio","count_number"})})
public class LabelCountEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCountEvent;

    @Column(nullable = false)
    private Long folio;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "count_number", nullable = false)
    private Integer countNumber;

    @Column(name = "counted_value", nullable = false)
    private java.math.BigDecimal countedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_at_time", nullable = false)
    private Role roleAtTime;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Role {ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO}

    public Long getIdCountEvent() { return idCountEvent; }
    public void setIdCountEvent(Long idCountEvent) { this.idCountEvent = idCountEvent; }

    public Long getFolio() { return folio; }
    public void setFolio(Long folio) { this.folio = folio; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getCountNumber() { return countNumber; }
    public void setCountNumber(Integer countNumber) { this.countNumber = countNumber; }

    public java.math.BigDecimal getCountedValue() { return countedValue; }
    public void setCountedValue(java.math.BigDecimal countedValue) { this.countedValue = countedValue; }

    public Role getRoleAtTime() { return roleAtTime; }
    public void setRoleAtTime(Role roleAtTime) { this.roleAtTime = roleAtTime; }

    public Boolean getIsFinal() { return isFinal; }
    public void setIsFinal(Boolean isFinal) { this.isFinal = isFinal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
