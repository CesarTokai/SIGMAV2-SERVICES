package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    java.util.Optional<Label> findByFolioAndPeriodIdAndWarehouseId(Long folio, Long periodId, Long warehouseId);

    // Métodos para reportes
    List<Label> findByPeriodId(Long periodId);

    List<Label> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId);

    List<Label> findByPeriodIdAndEstado(Long periodId, Label.State estado);

    List<Label> findByPeriodIdAndWarehouseIdAndEstado(Long periodId, Long warehouseId, Label.State estado);

    // Query para marbetes con impresión (para distribución)
    @Query("SELECT l FROM Label l WHERE l.periodId = :periodId AND l.estado IN ('IMPRESO', 'GENERADO') " +
           "ORDER BY l.warehouseId, l.folio")
    List<Label> findPrintedLabelsByPeriod(@Param("periodId") Long periodId);

    @Query("SELECT l FROM Label l WHERE l.periodId = :periodId AND l.warehouseId = :warehouseId " +
           "AND l.estado IN ('IMPRESO', 'GENERADO') ORDER BY l.folio")
    List<Label> findPrintedLabelsByPeriodAndWarehouse(@Param("periodId") Long periodId,
                                                       @Param("warehouseId") Long warehouseId);
}
