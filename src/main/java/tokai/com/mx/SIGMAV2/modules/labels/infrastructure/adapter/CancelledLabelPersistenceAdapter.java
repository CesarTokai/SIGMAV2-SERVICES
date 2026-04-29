package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCancelledRepository;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelledLabelPersistenceAdapter {

    private final JpaLabelCancelledRepository jpaLabelCancelledRepository;

    public List<LabelCancelled> findByPeriodAndWarehouse(Long periodId, Long warehouseId, Boolean reactivado) {
        return jpaLabelCancelledRepository.findByPeriodIdAndWarehouseIdAndReactivado(periodId, warehouseId, reactivado);
    }

    public Optional<LabelCancelled> findByFolio(Long folio) {
        return jpaLabelCancelledRepository.findByFolio(folio);
    }

    public Optional<LabelCancelled> findByFolioAndPeriodId(Long folio, Long periodId) {
        return jpaLabelCancelledRepository.findByFolioAndPeriodId(folio, periodId);
    }

    @Transactional
    public LabelCancelled save(LabelCancelled cancelled) {
        return jpaLabelCancelledRepository.save(cancelled);
    }

    public long countByPeriodAndWarehouseAndReactivado(Long periodId, Long warehouseId, Boolean reactivado) {
        return jpaLabelCancelledRepository.countByPeriodIdAndWarehouseIdAndReactivado(periodId, warehouseId, reactivado);
    }
}
