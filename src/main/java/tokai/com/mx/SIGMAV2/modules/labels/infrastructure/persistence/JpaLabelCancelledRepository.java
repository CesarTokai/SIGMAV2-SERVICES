package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaLabelCancelledRepository extends JpaRepository<LabelCancelled, Long> {

    Optional<LabelCancelled> findByFolio(Long folio);

    List<LabelCancelled> findByPeriodIdAndWarehouseIdAndReactivado(Long periodId, Long warehouseId, Boolean reactivado);

    List<LabelCancelled> findByProductIdAndPeriodIdAndWarehouseId(Long productId, Long periodId, Long warehouseId);

    long countByPeriodIdAndWarehouseIdAndReactivado(Long periodId, Long warehouseId, Boolean reactivado);

    // Métodos adicionales para reportes
    List<LabelCancelled> findByPeriodIdAndReactivado(Long periodId, Boolean reactivado);

    List<LabelCancelled> findByPeriodId(Long periodId);
}



