package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InventoryImportJob {

    public enum ImportStatus { SUCCESS, WARNING, ERROR }

    private Long id;
    private String fileName;
    private String user;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int totalRecords;
    private ImportStatus status;
    private Integer insertedRows;
    private Integer updatedRows;
    private Integer skippedRows;
    private Integer totalRows;
    private String errorsJson;
    private String logFilePath;
    private Long idPeriod;
    private Long idWarehouse;
    private String checksum;
    private String createdBy;

}
