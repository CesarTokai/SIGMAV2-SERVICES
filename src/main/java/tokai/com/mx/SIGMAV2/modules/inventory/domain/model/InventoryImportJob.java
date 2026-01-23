package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import java.time.LocalDateTime;

public class InventoryImportJob {
    private Long id;
    private String fileName;
    private String user;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int totalRecords;
    private String status; // SUCCESS, ERROR, etc.
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getInsertedRows() { return insertedRows; }
    public void setInsertedRows(Integer insertedRows) { this.insertedRows = insertedRows; }

    public Integer getUpdatedRows() { return updatedRows; }
    public void setUpdatedRows(Integer updatedRows) { this.updatedRows = updatedRows; }

    public Integer getSkippedRows() { return skippedRows; }
    public void setSkippedRows(Integer skippedRows) { this.skippedRows = skippedRows; }

    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }

    public String getErrorsJson() { return errorsJson; }
    public void setErrorsJson(String errorsJson) { this.errorsJson = errorsJson; }

    public String getLogFilePath() { return logFilePath; }
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }

    public Long getIdPeriod() { return idPeriod; }
    public void setIdPeriod(Long idPeriod) { this.idPeriod = idPeriod; }

    public Long getIdWarehouse() { return idWarehouse; }
    public void setIdWarehouse(Long idWarehouse) { this.idWarehouse = idWarehouse; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
