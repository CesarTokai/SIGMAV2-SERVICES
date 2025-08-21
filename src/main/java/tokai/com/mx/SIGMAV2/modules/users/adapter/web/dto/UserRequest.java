package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

public class UserRequest {
    private String email;
    private String password;
    private String role;

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Method to convert UserRequest to User domain model
    public tokai.com.mx.SIGMAV2.modules.users.model.BeanUser toDomain() {
        tokai.com.mx.SIGMAV2.modules.users.model.BeanUser user = new tokai.com.mx.SIGMAV2.modules.users.model.BeanUser();
        user.setEmail(this.email);
        user.setPasswordHash(this.password);
        user.setRole(tokai.com.mx.SIGMAV2.modules.users.model.ERole.valueOf(this.role));
        return user;
    }
    
}
