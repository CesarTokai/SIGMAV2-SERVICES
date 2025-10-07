package tokai.com.mx.SIGMAV2.modules.labels.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelFolioSequence;

import java.util.Optional;

public interface LabelFolioSequenceRepository {

    Optional<LabelFolioSequence> findByPeriodIdForUpdate(Long periodId);

    LabelFolioSequence save(LabelFolioSequence sequence);
}

