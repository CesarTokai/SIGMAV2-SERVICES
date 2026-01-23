package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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
}
