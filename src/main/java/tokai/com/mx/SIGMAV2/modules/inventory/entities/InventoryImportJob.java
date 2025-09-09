package tokai.com.mx.SIGMAV2.modules.inventory.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_import_jobs")
public class InventoryImportJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_period", nullable = false)
    private Period period;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_warehouse")
    private Warehouse warehouse; // NULL significa ALL warehouses
    
    @Column(name = "filename", nullable = false)
    private String filename;
    
    @Column(name = "checksum", length = 64)
    private String checksum;
    
    @Column(name = "created_by")
    private String createdBy; // user ID o email
    
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();
    
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.PENDING;
    
    @Column(name = "total_rows")
    private Integer totalRows = 0;
    
    @Column(name = "inserted_rows")
    private Integer insertedRows = 0;
    
    @Column(name = "updated_rows")
    private Integer updatedRows = 0;
    
    @Column(name = "skipped_rows")
    private Integer skippedRows = 0;
    
    @Column(name = "errors_json", columnDefinition = "TEXT")
    private String errorsJson;
    
    @Column(name = "log_file_path")
    private String logFilePath; // Ruta al archivo CSV de log
    
    public enum JobStatus {
        PENDING, RUNNING, DONE, ERROR
    }
    
    // Constructors
    public InventoryImportJob() {}
    
    public InventoryImportJob(Period period, Warehouse warehouse, String filename, String createdBy) {
        this.period = period;
        this.warehouse = warehouse;
        this.filename = filename;
        this.createdBy = createdBy;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Period getPeriod() {
        return period;
    }
    
    public void setPeriod(Period period) {
        this.period = period;
    }
    
    public Warehouse getWarehouse() {
        return warehouse;
    }
    
    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
    
    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
    
    public JobStatus getStatus() {
        return status;
    }
    
    public void setStatus(JobStatus status) {
        this.status = status;
    }
    
    public Integer getTotalRows() {
        return totalRows;
    }
    
    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }
    
    public Integer getInsertedRows() {
        return insertedRows;
    }
    
    public void setInsertedRows(Integer insertedRows) {
        this.insertedRows = insertedRows;
    }
    
    public Integer getUpdatedRows() {
        return updatedRows;
    }
    
    public void setUpdatedRows(Integer updatedRows) {
        this.updatedRows = updatedRows;
    }
    
    public Integer getSkippedRows() {
        return skippedRows;
    }
    
    public void setSkippedRows(Integer skippedRows) {
        this.skippedRows = skippedRows;
    }
    
    public String getErrorsJson() {
        return errorsJson;
    }
    
    public void setErrorsJson(String errorsJson) {
        this.errorsJson = errorsJson;
    }
    
    public String getLogFilePath() {
        return logFilePath;
    }
    
    public void setLogFilePath(String logFilePath) {
        this.logFilePath = logFilePath;
    }
}