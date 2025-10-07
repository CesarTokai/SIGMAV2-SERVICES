package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaLabelCountEventRepository extends JpaRepository<LabelCountEvent, Long> {

    boolean existsByFolioAndCountNumber(Long folio, Integer countNumber);

    long countByFolio(Long folio);

    Optional<LabelCountEvent> findTopByFolioOrderByCreatedAtDesc(Long folio);

    List<LabelCountEvent> findByFolioOrderByCreatedAtAsc(Long folio);

}
