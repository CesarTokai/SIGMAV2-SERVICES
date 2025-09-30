package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_import_jobs")
public class InventoryImportJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "username")
    private String user;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "status")
    private String status;

    @Column(name = "inserted_rows")
    private Integer insertedRows;

    @Column(name = "updated_rows")
    private Integer updatedRows;

    @Column(name = "skipped_rows")
    private Integer skippedRows;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "errors_json")
    private String errorsJson;

    @Column(name = "log_file_path")
    private String logFilePath;

    @Column(name = "id_period")
    private Long idPeriod;

    @Column(name = "id_warehouse")
    private Long idWarehouse;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "created_by")
    private String createdBy;

    // Getters and setters
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

    public Integer getTotalRecords() { return totalRecords; }
    public void setTotalRecords(Integer totalRecords) { this.totalRecords = totalRecords; }

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
