package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventoryImportJobEntity;

@Component
public class InventoryImportJobMapper {

    public InventoryImportJob toDomain(InventoryImportJobEntity e) {
        if (e == null) return null;
        InventoryImportJob d = new InventoryImportJob();
        d.setId(e.getId());
        d.setFileName(e.getFileName());
        d.setUser(e.getUser());
        d.setStartedAt(e.getStartedAt());
        d.setFinishedAt(e.getFinishedAt());
        d.setTotalRecords(e.getTotalRecords() != null ? e.getTotalRecords() : 0);
        d.setStatus(e.getStatus());
        d.setInsertedRows(e.getInsertedRows());
        d.setUpdatedRows(e.getUpdatedRows());
        d.setSkippedRows(e.getSkippedRows());
        d.setTotalRows(e.getTotalRows());
        d.setErrorsJson(e.getErrorsJson());
        d.setLogFilePath(e.getLogFilePath());
        d.setIdPeriod(e.getIdPeriod());
        d.setIdWarehouse(e.getIdWarehouse());
        d.setChecksum(e.getChecksum());
        d.setCreatedBy(e.getCreatedBy());
        return d;
    }

    public InventoryImportJobEntity toEntity(InventoryImportJob d) {
        if (d == null) return null;
        InventoryImportJobEntity e = new InventoryImportJobEntity();
        e.setId(d.getId());
        e.setFileName(d.getFileName());
        e.setUser(d.getUser());
        e.setStartedAt(d.getStartedAt());
        e.setFinishedAt(d.getFinishedAt());
        e.setTotalRecords(d.getTotalRecords());
        e.setStatus(d.getStatus());
        e.setInsertedRows(d.getInsertedRows());
        e.setUpdatedRows(d.getUpdatedRows());
        e.setSkippedRows(d.getSkippedRows());
        e.setTotalRows(d.getTotalRows());
        e.setErrorsJson(d.getErrorsJson());
        e.setLogFilePath(d.getLogFilePath());
        e.setIdPeriod(d.getIdPeriod());
        e.setIdWarehouse(d.getIdWarehouse());
        e.setChecksum(d.getChecksum());
        e.setCreatedBy(d.getCreatedBy());
        return e;
    }
}
