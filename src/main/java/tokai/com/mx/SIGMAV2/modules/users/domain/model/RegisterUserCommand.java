package tokai.com.mx.SIGMAV2.modules.users.domain.model;

import lombok.Getter;

/**
 * Command object de dominio para registrar un nuevo usuario.
 * Desacopla el puerto de entrada (UserService) de los DTOs del adaptador web.
 */
@Getter
public class RegisterUserCommand {

    private final String email;
    private final String password;
    private final String role;
    private final String name;
    private final String firstLastName;
    private final String secondLastName;
    private final String phoneNumber;
    private final String comments;
    private final Boolean status;
    private final Boolean preVerified;

    public RegisterUserCommand(String email, String password, String role,
                               String name, String firstLastName, String secondLastName,
                               String phoneNumber, String comments,
                               Boolean status, Boolean preVerified) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.name = name;
        this.firstLastName = firstLastName;
        this.secondLastName = secondLastName;
        this.phoneNumber = phoneNumber;
        this.comments = comments;
        this.status = status;
        this.preVerified = preVerified;
    }
}

