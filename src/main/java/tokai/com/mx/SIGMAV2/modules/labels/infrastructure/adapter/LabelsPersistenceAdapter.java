package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelFolioSequence;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelGenerationBatch;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.LabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.LabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelFolioSequenceRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelGenerationBatchRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LabelsPersistenceAdapter implements LabelRepository, LabelRequestRepository {

    private final JpaLabelRepository jpaLabelRepository;
    private final JpaLabelRequestRepository jpaLabelRequestRepository;
    private final JpaLabelFolioSequenceRepository jpaLabelFolioSequenceRepository;
    private final JpaLabelGenerationBatchRepository jpaLabelGenerationBatchRepository;

    @Override
    public Label save(Label label) {
        return jpaLabelRepository.save(label);
    }

    @Override
    public Optional<Label> findByFolio(Long folio) {
        return jpaLabelRepository.findById(folio);
    }

    @Override
    public List<Label> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId, int offset, int limit) {
        if (limit <= 0) return List.of();
        int page = offset / limit;
        var pageReq = PageRequest.of(page, limit);
        return jpaLabelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId, pageReq).getContent();
    }

    @Override
    public long countByPeriodIdAndWarehouseId(Long periodId, Long warehouseId) {
        return jpaLabelRepository.countByPeriodIdAndWarehouseId(periodId, warehouseId);
    }

    @Override
    public List<Label> findGeneratedByRequestIdRange(Long requestId, Long startFolio, Long endFolio) {
        var pageReq = PageRequest.of(0, (int)(endFolio - startFolio + 1));
        return jpaLabelRepository.findByLabelRequestIdAndFolioBetween(requestId, startFolio, endFolio, pageReq).getContent();
    }

    // LabelRequestRepository methods
    @Override
    public LabelRequest save(LabelRequest request) {
        return jpaLabelRequestRepository.save(request);
    }

    @Override
    public Optional<LabelRequest> findByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId) {
        return jpaLabelRequestRepository.findByProductIdAndWarehouseIdAndPeriodId(productId, warehouseId, periodId);
    }

    @Override
    public boolean existsGeneratedUnprintedForProductWarehousePeriod(Long productId, Long warehouseId, Long periodId) {
        return jpaLabelRepository.existsByProductIdAndWarehouseIdAndPeriodIdAndEstado(productId, warehouseId, periodId, Label.State.GENERADO);
    }

    // Operaciones de secuencia y generación (helpers usados por el servicio)
    @Transactional
    public synchronized long[] allocateFolioRange(Long periodId, int quantity) {
        // Bloqueo PESSIMISTIC_WRITE se aplica en el repositorio JPA via findById con @Lock
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

    @Transactional
    public LabelGenerationBatch saveGenerationBatch(LabelGenerationBatch batch) {
        return jpaLabelGenerationBatchRepository.save(batch);
    }

    @Transactional
    public void saveLabelsBatch(Long requestId, Long periodId, Long warehouseId, Long productId, long primer, long ultimo, Long createdBy) {
        List<Label> labels = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (long f = primer; f <= ultimo; f++) {
            Label l = new Label();
            l.setFolio(f);
            l.setLabelRequestId(requestId);
            l.setPeriodId(periodId);
            l.setWarehouseId(warehouseId);
            l.setProductId(productId);
            l.setEstado(Label.State.GENERADO);
            l.setCreatedBy(createdBy);
            l.setCreatedAt(now);
            labels.add(l);
        }
        jpaLabelRepository.saveAll(labels);
    }
}
