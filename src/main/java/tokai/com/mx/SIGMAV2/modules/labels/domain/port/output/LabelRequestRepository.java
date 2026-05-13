package tokai.com.mx.SIGMAV2.modules.labels.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;

import java.util.Optional;

public interface LabelRequestRepository {

    LabelRequest save(LabelRequest request);

    Optional<LabelRequest> findByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId);

    void delete(LabelRequest request);

    boolean existsGeneratedUnprintedForProductWarehousePeriod(Long productId, Long warehouseId, Long periodId);

}

