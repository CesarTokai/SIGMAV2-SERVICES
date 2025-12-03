package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;

import java.util.List;

@Repository
public interface JpaLabelRepository extends JpaRepository<Label, Long> {

    boolean existsByProductIdAndWarehouseIdAndPeriodIdAndEstado(Long productId, Long warehouseId, Long periodId, Label.State estado);

    Page<Label> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId, Pageable pageable);

    Page<Label> findByLabelRequestIdAndFolioBetween(Long labelRequestId, Long startFolio, Long endFolio, Pageable pageable);

    long countByPeriodIdAndWarehouseId(Long periodId, Long warehouseId);

    List<Label> findByFolioBetween(Long startFolio, Long endFolio);

    List<Label> findByProductIdAndPeriodIdAndWarehouseId(Long productId, Long periodId, Long warehouseId);
}
