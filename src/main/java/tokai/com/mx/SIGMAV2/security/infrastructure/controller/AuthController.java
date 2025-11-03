package tokai.com.mx.SIGMAV2.security.infrastructure.controller;


import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.validation.Valid;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep1DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep2DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.ResponseAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.jwt.JwtUtils;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.JwtBlacklistService;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.UserDetailsServicePer;
  import tokai.com.mx.SIGMAV2.shared.audit.AuditEntry;
import tokai.com.mx.SIGMAV2.shared.audit.AuditService;
import tokai.com.mx.SIGMAV2.shared.response.ApiResponse;
import tokai.com.mx.SIGMAV2.shared.response.ResponseHelper;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/sigmav2/auth")
@PreAuthorize("permitAll()")
public class AuthController {
    private final UserDetailsServicePer userDetailsServicePer;
    private final JwtBlacklistService jwtBlacklistService;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;
    private final JpaUserRepository jpaUserRepository;

    public AuthController(UserDetailsServicePer userDetailsServicePer, JwtBlacklistService jwtBlacklistService, JwtUtils jwtUtils, AuditService auditService, JpaUserRepository jpaUserRepository) {
        this.userDetailsServicePer = userDetailsServicePer;
        this.jwtBlacklistService = jwtBlacklistService;
        this.jwtUtils = jwtUtils;
        this.auditService = auditService;
        this.jpaUserRepository = jpaUserRepository;
    }

    @GetMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> getMapping (){
        return ResponseHelper.success("hola mundo", "Endpoint de administrador accedido correctamente");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<ResponseAuthDTO>> login(@RequestBody @Valid RequestAuthDTO authRequest){
        try {
            ResponseAuthDTO authResponse = this.userDetailsServicePer.login(authRequest);
            return ResponseHelper.success(authResponse, "Inicio de sesión exitoso");
        } catch (Exception e) {
            return ResponseHelper.unauthorized("LOGIN_FAILED", "Error de autenticación", e.getMessage());
        }
    }

    @PostMapping("/findUserToResetPassword")
    public ResponseEntity<ApiResponse<Object>> findUserToResetPassword(@RequestBody @Valid RequestResetPasswordStep1DTO payload){
        try {
            Object result = this.userDetailsServicePer.findUserToResetPassword(payload);
            return ResponseHelper.success(result, "Usuario encontrado para recuperación de contraseña");
        } catch (Exception e) {
            return ResponseHelper.badRequest("USER_NOT_FOUND", "Error al buscar usuario", e.getMessage());
        }
    }

    @PostMapping("/compareCodeToResetPassword")
    public ResponseEntity<ApiResponse<Object>> compareCodeToResetPassword(@RequestBody @Valid RequestResetPasswordStep2DTO payload){
        try {
            Object result = this.userDetailsServicePer.compareCodeToResetPassword(payload);
            return ResponseHelper.success(result, "Código de verificación validado correctamente");
        } catch (Exception e) {
            return ResponseHelper.badRequest("INVALID_CODE", "Código de verificación inválido", e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token != null && !token.isEmpty()) {
            try {
                DecodedJWT decoded = jwtUtils.validateToken(token);
                long exp = decoded.getExpiresAt().toInstant().getEpochSecond();
                jwtBlacklistService.blacklist(token, exp);

                String subject = decoded.getSubject();
                // marcar usuario como inactivo si existe
                try {
                    if (subject != null) {
                        jpaUserRepository.findByEmail(subject).ifPresent(u -> {
                            u.setStatus(false);
                            jpaUserRepository.save(u);
                        });
                    }
                } catch (Exception ignore) {
                    // no propagar
                }

                // Registrar en bitácora
                AuditEntry entry = new AuditEntry();
                entry.setAction("LOGOUT");
                entry.setResourceType("Auth");
                entry.setDetails("Logout JWT");
                entry.setPrincipal(subject);
                entry.setOutcome("SUCCESS");
                auditService.log(entry);
            } catch (Exception e) {
                // Si el token es inválido, igual respondemos éxito pero logueamos el error
                AuditEntry entry = new AuditEntry();
                entry.setAction("LOGOUT_FAIL");
                entry.setResourceType("Auth");
                entry.setDetails("Logout JWT inválido");
                entry.setPrincipal(null);
                entry.setOutcome("FAILURE");
                auditService.log(entry);
            }
        }
        return ResponseHelper.success(null, "Sesión cerrada correctamente");
    }

}