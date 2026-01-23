package tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalInformationResponse {
    private Long id;
    private Long userId;
    private String name;
    private String firstLastName;
    private String secondLastName;
    private String phoneNumber;
    private boolean hasImage;
    private String fullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String email;
    private String role;
    private boolean status;
    private String comments; // nuevo campo

    // Constructor por defecto
    public PersonalInformationResponse() {}

    // Constructor completo (compatibilidad antigua)
    public PersonalInformationResponse(Long id, Long userId, String name, String firstLastName,
                                     String secondLastName, String phoneNumber, boolean hasImage,
                                     String fullName, LocalDateTime createdAt, LocalDateTime updatedAt,
                                     String email, String role, boolean status) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
        this.hasImage = hasImage;
        this.fullName = fullName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    // Nuevo constructor que incluye comments
    public PersonalInformationResponse(Long id, Long userId, String name, String firstLastName,
                                        String secondLastName, String phoneNumber, boolean hasImage,
                                        String fullName, LocalDateTime createdAt, LocalDateTime updatedAt,
                                        String email, String role, boolean status, String comments) {
        this(id, userId, name, firstLastName, secondLastName, phoneNumber, hasImage,
                fullName, createdAt, updatedAt, email, role, status);
        this.comments = comments;
    }


}
