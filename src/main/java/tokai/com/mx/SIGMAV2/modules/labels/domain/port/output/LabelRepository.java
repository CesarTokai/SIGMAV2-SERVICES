package tokai.com.mx.SIGMAV2.modules.labels.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;

import java.util.List;
import java.util.Optional;

public interface LabelRepository {

    Label save(Label label);

    /**
     * Busca un marbete por su clave primaria compuesta (folio + periodId)
     * 
     * @param folio Número de folio
     * @param periodId ID del periodo
     * @return Optional con el marbete si existe
     */
    Optional<Label> findByFolioAndPeriodId(Long folio, Long periodId);

    List<Label> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId, int offset, int limit);

    long countByPeriodIdAndWarehouseId(Long periodId, Long warehouseId);

    List<Label> findGeneratedByRequestIdRange(Long requestId, Long startFolio, Long endFolio);

}

