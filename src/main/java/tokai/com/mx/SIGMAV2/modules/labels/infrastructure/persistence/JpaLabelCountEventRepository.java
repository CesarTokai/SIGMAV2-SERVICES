package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;

@Repository
public interface JpaLabelCountEventRepository extends JpaRepository<LabelCountEvent, Long> {

}

