package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entidad para registrar marbetes cancelados por falta de existencias.
 * Estos marbetes pueden ser reactivados si posteriormente se registran existencias.
 */
@Entity
@Table(name = "labels_cancelled")
@Data
public class LabelCancelled {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_label_cancelled")
    private Long idLabelCancelled;

    @Column(name = "folio", nullable = false, unique = true)
    private Long folio;

    @Column(name = "id_label_request", nullable = false)
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
}

