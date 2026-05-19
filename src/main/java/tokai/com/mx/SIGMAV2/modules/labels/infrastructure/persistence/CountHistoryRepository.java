package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.CountHistory;

import java.util.List;

/**
 * Repositorio para acceder al historial de conteos (tabla folio_request_history)
 */
@Repository
public interface CountHistoryRepository extends JpaRepository<CountHistory, Long> {

    Page<CountHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<CountHistory> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId, Pageable pageable);

    Page<CountHistory> findByPeriodIdOrderByCreatedAtDesc(Long periodId, Pageable pageable);

    List<CountHistory> findByFolioOrderByCreatedAtAsc(Long folio);

    Page<CountHistory> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<CountHistory> findByUserIdAndPeriodIdOrderByCreatedAtDesc(Long userId, Long periodId, Pageable pageable);

    Long countByUserIdAndPeriodId(Long userId, Long periodId);

    CountHistory findTopByUserIdAndPeriodIdOrderByCreatedAtDesc(Long userId, Long periodId);

    /**
     * Obtiene todos los conteos registrados ordenados por fecha descendente
     */
    Page<CountHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
