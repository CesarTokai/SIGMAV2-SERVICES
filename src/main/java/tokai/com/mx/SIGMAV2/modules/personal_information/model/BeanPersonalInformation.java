package tokai.com.mx.SIGMAV2.modules.personal_information.model;

import jakarta.annotation.Generated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import jakarta.persistence.*;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "personal_information")
public class BeanPersonalInformation {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
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

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private BeanUser user;
    
}



