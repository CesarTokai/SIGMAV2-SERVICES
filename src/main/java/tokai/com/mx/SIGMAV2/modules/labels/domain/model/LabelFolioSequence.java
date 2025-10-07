package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "label_folio_sequence")
public class LabelFolioSequence {

    @Id
    @Column(name = "id_period")
    private Long periodId;

    @Column(name = "ultimo_folio", nullable = false)
    private Long ultimoFolio = 0L;

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getUltimoFolio() { return ultimoFolio; }
    public void setUltimoFolio(Long ultimoFolio) { this.ultimoFolio = ultimoFolio; }
}
