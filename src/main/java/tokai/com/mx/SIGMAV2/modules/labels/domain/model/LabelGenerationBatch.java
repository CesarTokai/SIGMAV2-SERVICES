package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "label_generation_batches")
public class LabelGenerationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_batch")
    private Long idBatch;

    @Column(name = "id_label_request", nullable = false)
    private Long labelRequestId;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "primer_folio", nullable = false)
    private Long primerFolio;

    @Column(name = "ultimo_folio", nullable = false)
    private Long ultimoFolio;

    @Column(name = "total_generados", nullable = false)
    private Integer totalGenerados;

    @Column(name = "generado_por", nullable = false)
    private Long generadoPor;

    @Column(name = "generado_at", nullable = false)
    private LocalDateTime generadoAt;

    public Long getIdBatch() { return idBatch; }
    public void setIdBatch(Long idBatch) { this.idBatch = idBatch; }

    public Long getLabelRequestId() { return labelRequestId; }
    public void setLabelRequestId(Long labelRequestId) { this.labelRequestId = labelRequestId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getPrimerFolio() { return primerFolio; }
    public void setPrimerFolio(Long primerFolio) { this.primerFolio = primerFolio; }

    public Long getUltimoFolio() { return ultimoFolio; }
    public void setUltimoFolio(Long ultimoFolio) { this.ultimoFolio = ultimoFolio; }

    public Integer getTotalGenerados() { return totalGenerados; }
    public void setTotalGenerados(Integer totalGenerados) { this.totalGenerados = totalGenerados; }

    public Long getGeneradoPor() { return generadoPor; }
    public void setGeneradoPor(Long generadoPor) { this.generadoPor = generadoPor; }

    public LocalDateTime getGeneradoAt() { return generadoAt; }
    public void setGeneradoAt(LocalDateTime generadoAt) { this.generadoAt = generadoAt; }
}
