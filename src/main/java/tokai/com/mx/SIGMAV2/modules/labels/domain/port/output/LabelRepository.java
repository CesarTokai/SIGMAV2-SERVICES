package tokai.com.mx.SIGMAV2.modules.labels.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;

import java.util.List;
import java.util.Optional;

public interface LabelRepository {

    Label save(Label label);

    Optional<Label> findByFolio(Long folio);

    List<Label> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId, int offset, int limit);

    long countByPeriodIdAndWarehouseId(Long periodId, Long warehouseId);

    List<Label> findGeneratedByRequestIdRange(Long requestId, Long startFolio, Long endFolio);

}

