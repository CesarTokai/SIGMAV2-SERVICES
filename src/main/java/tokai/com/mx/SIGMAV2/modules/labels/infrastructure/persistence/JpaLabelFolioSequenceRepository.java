package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelFolioSequence;

import java.util.Optional;

@Repository
public interface JpaLabelFolioSequenceRepository extends JpaRepository<LabelFolioSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @NonNull
    Optional<LabelFolioSequence> findById(@NonNull Long id);

}

