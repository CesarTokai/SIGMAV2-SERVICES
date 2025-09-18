package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryImportJob {
    private Long id;
    private String fileName;
    private String user;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int totalRecords;
    private String status; // SUCCESS, ERROR, etc.

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
}
