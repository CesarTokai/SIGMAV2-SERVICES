package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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
}
