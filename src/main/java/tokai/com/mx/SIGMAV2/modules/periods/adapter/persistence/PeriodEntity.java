package tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period.PeriodState;


@Entity
@Table(name = "periods")
@Data
public class PeriodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_period")
    private Long id;


    @Column(name = "period", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "comments")
    private String comments;



    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private PeriodState state;

}