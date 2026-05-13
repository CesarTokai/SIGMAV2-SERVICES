package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;

import java.util.Optional;

@Repository
public interface JpaLabelRequestRepository extends JpaRepository<LabelRequest, Long> {

    Optional<LabelRequest> findByProductIdAndWarehouseIdAndPeriodId(Long productId, Long warehouseId, Long periodId);

}

