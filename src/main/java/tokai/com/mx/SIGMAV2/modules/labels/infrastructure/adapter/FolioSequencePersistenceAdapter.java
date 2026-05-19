package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelFolioSequence;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelFolioSequenceRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FolioSequencePersistenceAdapter {

    private final JpaLabelFolioSequenceRepository jpaLabelFolioSequenceRepository;
    private final JpaPeriodRepository jpaPeriodRepository;

    @Transactional
    public synchronized long[] allocateFolioRange(Long periodId, int quantity) {
        Optional<LabelFolioSequence> opt = jpaLabelFolioSequenceRepository.findById(periodId);
        LabelFolioSequence seq;

        if (opt.isPresent()) {
            seq = opt.get();
        } else {
            seq = new LabelFolioSequence();
            seq.setPeriodId(periodId);
            seq.setUltimoFolio(0L);
        }

        long primer = seq.getUltimoFolio() + 1;
        long ultimo = seq.getUltimoFolio() + quantity;
        seq.setUltimoFolio(ultimo);
        jpaLabelFolioSequenceRepository.save(seq);

        return new long[]{primer, ultimo};
    }

    public Optional<Long> findLastCreatedPeriodId() {
        return jpaPeriodRepository.findLatestPeriod()
                .map(periodEntity -> periodEntity.getId());
    }
}
