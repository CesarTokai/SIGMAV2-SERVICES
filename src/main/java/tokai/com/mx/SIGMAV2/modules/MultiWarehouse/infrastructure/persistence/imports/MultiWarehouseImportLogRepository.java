package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence.imports;

import org.springframework.data.repository.CrudRepository;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;

import java.util.Optional;

public interface MultiWarehouseImportLogRepository extends CrudRepository<MultiWarehouseImportLog, Long> {
    Optional<MultiWarehouseImportLog> findByPeriodAndStageAndFileHash(String period, String stage, String fileHash);
    // Puedes agregar métodos personalizados si es necesario
}

// Nota: Debes crear la entidad MultiWarehouseImportLog en domain/model si vas a usar este repositorio.
