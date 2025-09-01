package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para usuario completo con información personal
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCompleteResponse {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private String role;

    @JsonProperty("status")
    private boolean status;

    @JsonProperty("isVerified")
    private boolean verified;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    // Información Personal
    @JsonProperty("personalInformation")
    private PersonalInfoData personalInformation;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PersonalInfoData {
        
        @JsonProperty("personalInformationId")
        private Long personalInformationId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("firstLastName")
        private String firstLastName;

        @JsonProperty("secondLastName")
        private String secondLastName;

        @JsonProperty("phoneNumber")
        private String phoneNumber;

        @JsonProperty("hasImage")
        private boolean hasImage;

        @JsonProperty("fullName")
        private String fullName;

        @JsonProperty("hasCompleteInfo")
        private boolean hasCompleteInfo;

        @JsonProperty("createdAt")
        private LocalDateTime createdAt;

        @JsonProperty("updatedAt")
        private LocalDateTime updatedAt;
    }
}
