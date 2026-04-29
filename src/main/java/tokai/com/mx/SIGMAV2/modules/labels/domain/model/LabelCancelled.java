package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Entidad para registrar el historial de marbetes cancelados.
 * Guarda auditoría completa: quién canceló, cuándo, por qué y las existencias al momento de cancelar.
 * Los conteos C1/C2 son eliminados al cancelar para evitar incongruencias en reportes.
 */
@Entity
@Table(name = "labels_cancelled",
    uniqueConstraints = @UniqueConstraint(name = "uk_cancelled_folio_period", columnNames = {"folio", "id_period"}))
@Getter
@Setter
public class LabelCancelled {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_label_cancelled")
    private Long idLabelCancelled;

    @Column(name = "folio", nullable = false)
    private Long folio;

    @Column(name = "id_label_request", nullable = true)
    private Long labelRequestId;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "id_product", nullable = false)
    private Long productId;

    @Column(name = "existencias_al_cancelar")
    private Integer existenciasAlCancelar = 0;

    @Column(name = "existencias_actuales")
    private Integer existenciasActuales = 0;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "cancelado_at", nullable = false)
    private LocalDateTime canceladoAt;

    @Column(name = "cancelado_by", nullable = false)
    private Long canceladoBy;

    @Column(name = "reactivado")
    private Boolean reactivado = false;

    @Column(name = "reactivado_at")
    private LocalDateTime reactivadoAt;

    @Column(name = "reactivado_by")
    private Long reactivadoBy;

    @Column(name = "notas")
    @Lob
    private String notas;

    /**
     * Valor del conteo C1 al momento de la cancelación.
     * Se archiva aquí para no perder el dato al eliminar el LabelCountEvent.
     * Null si el marbete fue cancelado antes de registrar C1.
     */
    @Column(name = "conteo1_al_cancelar", precision = 18, scale = 4)
    private java.math.BigDecimal conteo1AlCancelar;

    /**
     * Valor del conteo C2 al momento de la cancelación.
     * Null si el marbete fue cancelado antes de registrar C2.
     */
    @Column(name = "conteo2_al_cancelar", precision = 18, scale = 4)
    private java.math.BigDecimal conteo2AlCancelar;
}

