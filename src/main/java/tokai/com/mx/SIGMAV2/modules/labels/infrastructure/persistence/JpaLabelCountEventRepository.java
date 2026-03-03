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

    boolean existsByFolioAndCountNumber(Long folio, Integer countNumber);

    long countByFolio(Long folio);

    Optional<LabelCountEvent> findTopByFolioOrderByCreatedAtDesc(Long folio);

    List<LabelCountEvent> findByFolioOrderByCreatedAtAsc(Long folio);

    /**
     * Carga todos los conteos de una lista de folios en UNA SOLA query.
     * Elimina el patrón N+1 en todos los reportes.
     * Uso: agrupar resultado por folio con Collectors.groupingBy(LabelCountEvent::getFolio)
     */
    @Query("SELECT e FROM LabelCountEvent e WHERE e.folio IN :folios ORDER BY e.folio ASC, e.countNumber ASC")
    List<LabelCountEvent> findByFolioInOrderByFolioAscCountNumberAsc(@Param("folios") Collection<Long> folios);

    /**
     * Elimina todos los conteos de un folio (solo para cancelación).
     * Mantener para compatibilidad — preferir archivarlos en LabelCancelled.
     */
    void deleteByFolio(Long folio);
}
