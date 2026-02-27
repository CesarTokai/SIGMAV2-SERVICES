package tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para información personal.
 * Puede incluir campos enriquecidos de User (email, role, status)
 * porque es una responsabilidad del adaptador web ensamblar la vista completa.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private String comments;

    // Campos enriquecidos desde el módulo de Users (solo se populan cuando corresponde)
    private String email;
    private String role;
    private boolean status;
}


