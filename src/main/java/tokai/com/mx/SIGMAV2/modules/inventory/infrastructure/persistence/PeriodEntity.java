package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class PeriodEntity {

    private Long id;
    private String state;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate periodDate;
    private String comments;

}
