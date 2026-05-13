package tokai.com.mx.SIGMAV2.modules.users.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "user_activity_log")
public class BeanUserActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    private Long logId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BeanUser user;

    @Column(name = "action_type", nullable = false)
    private String actionType; // LOGIN, LOGOUT, BLOCKED, UNBLOCKED, DEACTIVATED, ACTIVATED, PASSWORD_CHANGED, FAILED_LOGIN, PASSWORD_RESET_ATTEMPT

    @Column(name = "action_details")
    private String actionDetails; // Detalles adicionales (razón del bloqueo, etc)

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "previous_status")
    private String previousStatus; // Estado anterior (para cambios de status)

    @Column(name = "new_status")
    private String newStatus; // Estado nuevo
}

