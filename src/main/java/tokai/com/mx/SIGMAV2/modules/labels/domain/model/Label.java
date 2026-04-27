package tokai.com.mx.SIGMAV2.modules.labels.domain.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "labels")
@IdClass(Label.LabelId.class)
public class Label {

    @Id
    private Long folio;

    @Id
    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "id_label_request")
    private Long labelRequestId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "id_product", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State estado = State.GENERADO;

    @Column(name = "qr_content", length = 255)
    private String qrContent;

    @Column(name = "impreso_at")
    private LocalDateTime impresoAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "comment", length = 600)
    private String comment;

    public enum State {GENERADO, IMPRESO, CANCELADO}

    /**
     * Clase de clave primaria compuesta para Label
     * Necesaria porque la PK es (folio, periodId)
     */
    @Getter
    @Setter
    public static class LabelId implements Serializable {
        private Long folio;
        private Long periodId;

        public LabelId() {}

        public LabelId(Long folio, Long periodId) {
            this.folio = folio;
            this.periodId = periodId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabelId labelId = (LabelId) o;
            return Objects.equals(folio, labelId.folio) &&
                   Objects.equals(periodId, labelId.periodId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(folio, periodId);
        }
    }

}
