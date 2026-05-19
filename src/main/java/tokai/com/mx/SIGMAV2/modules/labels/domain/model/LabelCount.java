package tokai.com.mx.SIGMAV2.modules.labels.domain.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "label_counts", uniqueConstraints = {@UniqueConstraint(columnNames = {"folio"})})
public class LabelCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_label_count")
    private Long idLabelCount;

    @Column(nullable = false)
    private Long folio;

    @Column(name = "one_count")
    private BigDecimal oneCount;

    @Column(name = "one_count_by")
    private Long oneCountBy;

    @Column(name = "one_count_at")
    private LocalDateTime oneCountAt;

    @Column(name = "second_count")
    private BigDecimal secondCount;

    @Column(name = "second_count_by")
    private Long secondCountBy;

    @Column(name = "second_count_at")
    private LocalDateTime secondCountAt;


}
