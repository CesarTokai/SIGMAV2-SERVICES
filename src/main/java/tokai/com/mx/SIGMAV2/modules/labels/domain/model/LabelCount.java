package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "label_counts", uniqueConstraints = {@UniqueConstraint(columnNames = {"folio"})})
public class LabelCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_label_count")
    private Long idLabelCount;

    @Column(nullable = false)
    private Long folio;

    @Column(name = "one_count")
    private BigDecimal oneCount;

    @Column(name = "one_count_by")
    private Long oneCountBy;

    @Column(name = "one_count_at")
    private LocalDateTime oneCountAt;

    @Column(name = "second_count")
    private BigDecimal secondCount;

    @Column(name = "second_count_by")
    private Long secondCountBy;

    @Column(name = "second_count_at")
    private LocalDateTime secondCountAt;

    public Long getIdLabelCount() { return idLabelCount; }
    public void setIdLabelCount(Long idLabelCount) { this.idLabelCount = idLabelCount; }

    public Long getFolio() { return folio; }
    public void setFolio(Long folio) { this.folio = folio; }

    public BigDecimal getOneCount() { return oneCount; }
    public void setOneCount(BigDecimal oneCount) { this.oneCount = oneCount; }

    public Long getOneCountBy() { return oneCountBy; }
    public void setOneCountBy(Long oneCountBy) { this.oneCountBy = oneCountBy; }

    public LocalDateTime getOneCountAt() { return oneCountAt; }
    public void setOneCountAt(LocalDateTime oneCountAt) { this.oneCountAt = oneCountAt; }

    public BigDecimal getSecondCount() { return secondCount; }
    public void setSecondCount(BigDecimal secondCount) { this.secondCount = secondCount; }

    public Long getSecondCountBy() { return secondCountBy; }
    public void setSecondCountBy(Long secondCountBy) { this.secondCountBy = secondCountBy; }

    public LocalDateTime getSecondCountAt() { return secondCountAt; }
    public void setSecondCountAt(LocalDateTime secondCountAt) { this.secondCountAt = secondCountAt; }
}
