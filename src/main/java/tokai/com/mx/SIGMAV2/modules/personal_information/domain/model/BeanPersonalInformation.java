package tokai.com.mx.SIGMAV2.modules.personal_information.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import jakarta.persistence.*;

import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.UserEntity;

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

    @Column(name ="first_last_name", nullable = false)
    private String firstLastName;

    @Column(name ="second_last_name", nullable = true)
    private String secondLastName;

    @Column(name ="phone_number", nullable = true)
    private String phoneNumber;

    @Column(name ="image", nullable = true)
    private byte[] image;

    
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_personal_info_user")
    )
    private UserEntity user;
    
}



