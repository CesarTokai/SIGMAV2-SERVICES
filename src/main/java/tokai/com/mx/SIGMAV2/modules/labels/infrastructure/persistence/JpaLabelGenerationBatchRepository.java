package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelGenerationBatch;

@Repository
public interface JpaLabelGenerationBatchRepository extends JpaRepository<LabelGenerationBatch, Long> {

}

