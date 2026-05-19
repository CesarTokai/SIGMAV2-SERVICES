package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "label_folio_sequence")
public class LabelFolioSequence {

    @Id
    @Column(name = "id_period")
    private Long periodId;

    @Column(name = "ultimo_folio", nullable = false)
    private Long ultimoFolio = 0L;
}
