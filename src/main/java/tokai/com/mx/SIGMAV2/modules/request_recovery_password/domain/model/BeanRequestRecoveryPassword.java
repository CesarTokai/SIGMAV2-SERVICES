package tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "request_recovery_password")
public class BeanRequestRecoveryPassword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id", updatable = false, nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    private BeanRequestStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BeanUser user;

    
    private LocalDate date;
}
