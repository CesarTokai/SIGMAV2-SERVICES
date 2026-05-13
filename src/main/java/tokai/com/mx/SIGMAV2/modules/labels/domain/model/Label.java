package tokai.com.mx.SIGMAV2.modules.labels.domain.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "labels")
public class Label {

    @Id
    private Long folio;

    @Column(name = "id_label_request")
    private Long labelRequestId;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "id_product", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State estado = State.GENERADO;

    @Column(name = "impreso_at")
    private LocalDateTime impresoAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum State {GENERADO, IMPRESO, CANCELADO}

}
