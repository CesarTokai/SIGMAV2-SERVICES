package tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePersonalInformationRequest {
    
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String name;
    
    @Size(max = 100, message = "El primer apellido no puede exceder 100 caracteres")
    private String firstLastName;
    
    @Size(max = 100, message = "El segundo apellido no puede exceder 100 caracteres")
    private String secondLastName;
    
    @Size(max = 15, message = "El número de teléfono no puede exceder 15 caracteres")
    private String phoneNumber;

    // Constructor por defecto
    public UpdatePersonalInformationRequest() {}

    // Constructor con parámetros
    public UpdatePersonalInformationRequest(String name, String firstLastName, String secondLastName, String phoneNumber) {
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
    }

}
