package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import java.time.LocalDateTime;

public class ImportJob {
    private String id;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private String type;

    public ImportJob(String id, String type) {
        this.id = id;
        this.type = type;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public void markAsCompleted(String message) {
        this.status = "COMPLETED";
        this.message = message;
    }

    public void markAsFailed(String error) {
        this.status = "FAILED";
        this.message = error;
    }

    // Getters
    public String getId() { return id; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getType() { return type; }
}
