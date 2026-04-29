package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CountEventPersistenceAdapter {

    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;

    @Transactional
    public LabelCountEvent saveCountEvent(Long folio, Long userId, Integer countNumber, BigDecimal countedValue,
                                          LabelCountEvent.Role roleAtTime, Boolean isFinal, String comment) {
        LabelCountEvent ev = new LabelCountEvent();
        ev.setFolio(folio);
        ev.setUserId(userId);
        ev.setCountNumber(countNumber);
        ev.setCountedValue(countedValue);
        ev.setRoleAtTime(roleAtTime);
        ev.setIsFinal(isFinal != null ? isFinal : false);
        ev.setCreatedAt(LocalDateTime.now());

        log.debug("saveCountEvent: folio={}, countNumber={}, comment={}, isEmpty={}",
            folio, countNumber, comment, (comment != null ? comment.isEmpty() : "null"));

        if (comment != null && !comment.trim().isEmpty()) {
            log.info("📝 Guardando comentario para folio {}, C{}: '{}'", folio, countNumber, comment.trim());
            ev.setComment(comment.trim());
        } else {
            log.warn("⚠️ SIN comentario para folio {}, C{}", folio, countNumber);
        }

        LabelCountEvent saved = jpaLabelCountEventRepository.save(ev);
        log.debug("✓ Guardado. ID={}, comment en BD={}", saved.getIdCountEvent(), saved.getComment());
        return saved;
    }

    public boolean hasCountNumber(Long folio, Integer countNumber) {
        return jpaLabelCountEventRepository.existsByFolioAndCountNumber(folio, countNumber);
    }

    public long countEventsForFolio(Long folio) {
        return jpaLabelCountEventRepository.countByFolio(folio);
    }

    public Optional<LabelCountEvent> findLatestCountEvent(Long folio) {
        return jpaLabelCountEventRepository.findTopByFolioOrderByCreatedAtDesc(folio);
    }

    public List<LabelCountEvent> findAllCountEvents(Long folio) {
        return jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
    }
}
