package tokai.com.mx.SIGMAV2.security;


import org.springframework.security.core.context.SecurityContextHolder;


public class SessionInformation {
    public static String getUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public static String getRole() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .findFirst()
                .map(authority -> {
                    String fullAuthority = authority.getAuthority();
                    String[] parts = fullAuthority.split("_");
                    return parts.length > 1 ? parts[1] : fullAuthority;
                })
                .orElse("UNKNOWN");
    }

}
