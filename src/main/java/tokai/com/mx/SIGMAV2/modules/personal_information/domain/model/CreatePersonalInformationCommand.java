package tokai.com.mx.SIGMAV2.modules.personal_information.domain.model;

import lombok.Getter;

/**
 * Command object de dominio para crear o actualizar información personal.
 * Desacopla el puerto de dominio de los DTOs del adaptador web.
 */
@Getter
public class CreatePersonalInformationCommand {

    private final String name;
    private final String firstLastName;
    private final String secondLastName;
    private final String phoneNumber;

    public CreatePersonalInformationCommand(String name, String firstLastName,
                                            String secondLastName, String phoneNumber) {
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
    }
}

