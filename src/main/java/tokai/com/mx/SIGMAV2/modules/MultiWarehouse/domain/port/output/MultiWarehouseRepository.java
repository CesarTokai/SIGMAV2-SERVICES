package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.output;

import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida (output port) para persistencia del módulo MultiWarehouse.
 *
 * <p>El caso de uso sólo conoce esta interfaz — no sabe si la implementación
 * usa JPA, MongoDB o una API externa. Las implementaciones concretas
 * viven en {@code infrastructure/persistence}.
 */
public interface MultiWarehouseRepository {

    // -------------------------------------------------------------------------
    // MultiWarehouseExistence
    // -------------------------------------------------------------------------

    List<MultiWarehouseExistence> findByPeriodId(Long periodId);

    List<MultiWarehouseExistence> findActiveByPeriodId(Long periodId);

    List<MultiWarehouseExistence> findInactiveByPeriodId(Long periodId);

    Optional<MultiWarehouseExistence> findByProductCodeAndWarehouseKeyAndPeriodId(
        String productCode, String warehouseKey, Long periodId);

    /**
     * Persiste todos los registros de la lista.
     * Acepta tanto inserts (id=null) como updates (id!=null).
     */
    List<MultiWarehouseExistence> saveAll(List<MultiWarehouseExistence> existences);

    // -------------------------------------------------------------------------
    // MultiWarehouseImportLog
    // -------------------------------------------------------------------------

    Optional<MultiWarehouseImportLog> findImportLogByPeriodAndStageAndFileHash(
        String period, String stage, String fileHash);

    Optional<MultiWarehouseImportLog> findImportLogById(Long id);

    MultiWarehouseImportLog saveImportLog(MultiWarehouseImportLog log);
}

