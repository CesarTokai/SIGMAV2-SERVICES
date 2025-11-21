package tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO de respuesta para información personal
 */
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

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @JsonProperty("created_at")
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("updated_at")
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
