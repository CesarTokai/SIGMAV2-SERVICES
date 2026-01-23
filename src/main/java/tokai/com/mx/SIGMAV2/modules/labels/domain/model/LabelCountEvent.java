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

    @Column(name = "counted_value", nullable = false)
    private java.math.BigDecimal countedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_at_time", nullable = false)
    private Role roleAtTime;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Role {ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO}
}
