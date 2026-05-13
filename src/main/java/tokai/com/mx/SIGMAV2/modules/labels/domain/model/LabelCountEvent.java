package tokai.com.mx.SIGMAV2.modules.labels.domain.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "label_count_events", uniqueConstraints = {@UniqueConstraint(columnNames = {"folio","count_number"})})
public class LabelCountEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCountEvent;

    @Column(nullable = false)
    private Long folio;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "count_number", nullable = false)
    private Integer countNumber;

    @Column(name = "counted_value", nullable = false, precision = 18, scale = 4)
    private java.math.BigDecimal countedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_at_time", nullable = false)
    private Role roleAtTime;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Valor previo antes de la última edición (updateCountC1 / updateCountC2).
     * Null si el conteo nunca fue modificado después de registrarse.
     */
    @Column(name = "previous_value", precision = 18, scale = 4)
    private java.math.BigDecimal previousValue;

    /** Timestamp de la última modificación por updateCountC1/C2. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** ID del usuario que realizó la última modificación. */
    @Column(name = "updated_by")
    private Long updatedBy;

    public enum Role {ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO}
}
