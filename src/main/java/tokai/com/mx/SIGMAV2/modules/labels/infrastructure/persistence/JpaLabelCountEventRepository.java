package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaLabelCountEventRepository extends JpaRepository<LabelCountEvent, Long> {

    // ── Métodos legacy (folio solo) — usar con cautela en períodos únicos ──
    /** @deprecated Usar {@link #existsByFolioAndCountNumberAndPeriodId} */
    @Deprecated
    boolean existsByFolioAndCountNumber(Long folio, Integer countNumber);

    /** @deprecated Usar {@link #countByFolioAndPeriodId} */
    @Deprecated
    long countByFolio(Long folio);

    /** @deprecated Usar {@link #findTopByFolioAndPeriodIdOrderByCreatedAtDesc} */
    @Deprecated
    Optional<LabelCountEvent> findTopByFolioOrderByCreatedAtDesc(Long folio);

    /** @deprecated Usar {@link #findByFolioAndPeriodIdOrderByCreatedAtAsc} */
    @Deprecated
    List<LabelCountEvent> findByFolioOrderByCreatedAtAsc(Long folio);

    // ── Métodos correctos (folio + período) ──
    boolean existsByFolioAndCountNumberAndPeriodId(Long folio, Integer countNumber, Long periodId);

    long countByFolioAndPeriodId(Long folio, Long periodId);

    Optional<LabelCountEvent> findTopByFolioAndPeriodIdOrderByCreatedAtDesc(Long folio, Long periodId);

    List<LabelCountEvent> findByFolioAndPeriodIdOrderByCreatedAtAsc(Long folio, Long periodId);

    /**
     * Carga todos los conteos de una lista de folios en UNA SOLA query.
     * Uso: agrupar resultado por folio con Collectors.groupingBy(LabelCountEvent::getFolio)
     */
    @Query("SELECT e FROM LabelCountEvent e WHERE e.folio IN :folios ORDER BY e.folio ASC, e.countNumber ASC")
    List<LabelCountEvent> findByFolioInOrderByFolioAscCountNumberAsc(@Param("folios") Collection<Long> folios);

    /**
     * Carga conteos de una lista de folios filtrados por período.
     */
    @Query("SELECT e FROM LabelCountEvent e WHERE e.folio IN :folios AND e.periodId = :periodId ORDER BY e.folio ASC, e.countNumber ASC")
    List<LabelCountEvent> findByFolioInAndPeriodIdOrderByFolioAscCountNumberAsc(@Param("folios") Collection<Long> folios, @Param("periodId") Long periodId);

    /**
     * Elimina todos los conteos de un folio en un período específico (para cancelación).
     */
    void deleteByFolioAndPeriodId(Long folio, Long periodId);

    /**
     * @deprecated Usar {@link #deleteByFolioAndPeriodId}
     */
    @Deprecated
    void deleteByFolio(Long folio);
}
