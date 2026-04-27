package tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period.PeriodState;


@Entity(name = "InventoryPeriodEntity")
@Table(name = "period")
@Getter
@Setter
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