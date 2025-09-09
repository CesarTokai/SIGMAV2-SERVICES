package tokai.com.mx.SIGMAV2.modules.inventory.dto;

import java.util.List;

public class ImportResultDto {
    
    private String jobId;
    private Long jobIdLong; // Para descargar logs
    private String period;
    private String mode;
    private int totalRows;
    private int inserted;
    private int updated;
    private int skipped;
    private long durationMs;
    private List<String> errors;
    private String logDownloadUrl; // URL para descargar el log
    
    // Constructors
    public ImportResultDto() {}
    
    public ImportResultDto(String jobId, String period, String mode, int totalRows, 
                          int inserted, int updated, int skipped, long durationMs) {
        this.jobId = jobId;
        this.period = period;
        this.mode = mode;
        this.totalRows = totalRows;
        this.inserted = inserted;
        this.updated = updated;
        this.skipped = skipped;
        this.durationMs = durationMs;
    }
    
    // Getters and Setters
    public String getJobId() {
        return jobId;
    }
    
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public int getTotalRows() {
        return totalRows;
    }
    
    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }
    
    public int getInserted() {
        return inserted;
    }
    
    public void setInserted(int inserted) {
        this.inserted = inserted;
    }
    
    public int getUpdated() {
        return updated;
    }
    
    public void setUpdated(int updated) {
        this.updated = updated;
    }
    
    public int getSkipped() {
        return skipped;
    }
    
    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public Long getJobIdLong() {
        return jobIdLong;
    }
    
    public void setJobIdLong(Long jobIdLong) {
        this.jobIdLong = jobIdLong;
    }
    
    public String getLogDownloadUrl() {
        return logDownloadUrl;
    }
    
    public void setLogDownloadUrl(String logDownloadUrl) {
        this.logDownloadUrl = logDownloadUrl;
    }
}