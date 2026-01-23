package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {
    private String email;
    private String password;
    private String role;
    private String name;
    private String firstLastName;
    private String secondLastName;
    private String phoneNumber;
    private String comments;
    private Boolean status;
    private Boolean preVerified;

}
