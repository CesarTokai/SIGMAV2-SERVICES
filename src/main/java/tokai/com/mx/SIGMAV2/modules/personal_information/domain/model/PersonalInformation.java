package tokai.com.mx.SIGMAV2.modules.personal_information.domain.model;

import java.time.LocalDateTime;

/**
 * Entidad de dominio pura - Información Personal del Usuario
 * Sin dependencias de frameworks externos
 */
public class PersonalInformation {
    private Long id;
    private Long userId;
    private String name;
    private String firstLastName;
    private String secondLastName;
    private String phoneNumber;
    private byte[] image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor por defecto
    public PersonalInformation() {}

    // Constructor completo
    public PersonalInformation(Long id, Long userId, String name, String firstLastName, 
                             String secondLastName, String phoneNumber, byte[] image,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
        this.image = image;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Métodos de dominio
    public void updateInformation(String name, String firstLastName, String secondLastName, String phoneNumber) {
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
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

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstLastName() {
        return firstLastName;
    }

    public void setFirstLastName(String firstLastName) {
        this.firstLastName = firstLastName;
    }

    public String getSecondLastName() {
        return secondLastName;
    }

    public void setSecondLastName(String secondLastName) {
        this.secondLastName = secondLastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
