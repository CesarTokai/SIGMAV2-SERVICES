package tokai.com.mx.SIGMAV2.modules.personal_information.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(
    name = "personal_information",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_personal_information_user",
        columnNames = "user_id"
    )
)
public class BeanPersonalInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_information_id")
    private Long personalInformationId;


    @Column(name ="name", nullable = false)
    private String name;

    @Column(name ="first_last_name")  // Cambiar a nullable = true
    private String firstLastName;

    @Column(name ="second_last_name")
    private String secondLastName;

    @Column(name ="phone_number")
    private String phoneNumber;

    @Lob  // Agregar esta anotación para LONGBLOB
    @Column(name ="image", columnDefinition = "LONGBLOB")
    private byte[] image;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "comments")
    private String comments;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        referencedColumnName = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_pi_user")
    )
    private BeanUser user;
}
