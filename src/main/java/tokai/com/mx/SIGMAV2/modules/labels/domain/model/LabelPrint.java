package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "label_prints")
public class LabelPrint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_label_print")
    private Long idLabelPrint;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "folio_inicial", nullable = false)
    private Long folioInicial;

    @Column(name = "folio_final", nullable = false)
    private Long folioFinal;

    @Column(name = "cantidad_impresa", nullable = false)
    private Integer cantidadImpresa;

    @Column(name = "printed_by", nullable = false)
    private Long printedBy;

    @Column(name = "printed_at", nullable = false)
    private LocalDateTime printedAt;

    public Long getIdLabelPrint() { return idLabelPrint; }
    public void setIdLabelPrint(Long idLabelPrint) { this.idLabelPrint = idLabelPrint; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getFolioInicial() { return folioInicial; }
    public void setFolioInicial(Long folioInicial) { this.folioInicial = folioInicial; }

    public Long getFolioFinal() { return folioFinal; }
    public void setFolioFinal(Long folioFinal) { this.folioFinal = folioFinal; }

    public Integer getCantidadImpresa() { return cantidadImpresa; }
    public void setCantidadImpresa(Integer cantidadImpresa) { this.cantidadImpresa = cantidadImpresa; }

    public Long getPrintedBy() { return printedBy; }
    public void setPrintedBy(Long printedBy) { this.printedBy = printedBy; }

    public LocalDateTime getPrintedAt() { return printedAt; }
    public void setPrintedAt(LocalDateTime printedAt) { this.printedAt = printedAt; }
}
