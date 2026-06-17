package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LabelId implements Serializable {

    private Long folio;
    private Long periodId;

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
