package tokai.com.mx.SIGMAV2.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_entry")
@Data
public class AuditEntry {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "principal")
    private String principal; // e.g. email

    @Column(name = "principal_name")
    private String principalName; // nombre completo si está disponible

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "outcome", length = 50)
    private String outcome; // SUCCESS / FAILURE

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "client_ip", length = 100)
    private String clientIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON o texto

    @PrePersist
    protected void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

}
