package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;

@Repository
public interface JpaLabelPrintRepository extends JpaRepository<LabelPrint, Long> {
    java.util.List<LabelPrint> findByPeriodIdAndWarehouseIdAndFolioInicialLessThanEqualAndFolioFinalGreaterThanEqual(Long periodId, Long warehouseId, Long folioInicial, Long folioFinal);
}
