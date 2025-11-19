package tokai.com.mx.SIGMAV2.modules.personal_information.domain.model;

import lombok.Getter;
import lombok.Setter;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.UserEntity;

import java.time.LocalDateTime;

/**
 * Entidad de dominio pura - Información Personal del Usuario
 * Sin dependencias de frameworks externos
 */
@Getter
@Setter
public class PersonalInformation {
    private Long id;
    private Long userId;
    private String name;
    private String firstLastName;
    private String secondLastName;
    private String phoneNumber;
    private byte[] image;
    private String comments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor por defecto
    public PersonalInformation() {}

    // Constructor completo ordenado
    public PersonalInformation(Long id, Long userId, String name, String firstLastName,
                             String secondLastName, String phoneNumber, byte[] image,
                             String comments, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
        this.image = image;
        this.comments = comments;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Métodos de dominio
    public void updateInformation(String name, String firstLastName, String secondLastName, String phoneNumber) {
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
        this.comments = comments;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateImage(byte[] image) {
        this.image = image;
        this.updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (name != null) fullName.append(name);
        if (firstLastName != null) fullName.append(" ").append(firstLastName);
        if (secondLastName != null) fullName.append(" ").append(secondLastName);
        return fullName.toString().trim();
    }

    public boolean hasCompleteBasicInfo() {
        return name != null && !name.trim().isEmpty() && 
               firstLastName != null && !firstLastName.trim().isEmpty();
    }


    public void setUser(UserEntity user) {

    }
}
