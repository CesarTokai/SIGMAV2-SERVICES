package tokai.com.mx.SIGMAV2.modules.labels.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;

import java.util.List;
import java.util.Optional;

public interface LabelRepository {

    Label save(Label label);

    /**
     * @deprecated Usar {@link #findByFolioAndPeriodId(Long, Long)} para evitar ambigüedad
     * cuando el mismo folio existe en múltiples períodos.
     */
    @Deprecated
    Optional<Label> findByFolio(Long folio);

    /**
     * Búsqueda correcta: folio + período para identificación única.
     */
    Optional<Label> findByFolioAndPeriodId(Long folio, Long periodId);

    List<Label> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId, int offset, int limit);

    long countByPeriodIdAndWarehouseId(Long periodId, Long warehouseId);

    List<Label> findGeneratedByRequestIdRange(Long requestId, Long startFolio, Long endFolio);

}

