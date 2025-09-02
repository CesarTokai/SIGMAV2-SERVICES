package tokai.com.mx.SIGMAV2.security.infrastructure.controller;


import jakarta.validation.Valid;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep1DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep2DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.ResponseAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.UserDetailsServicePer;
import tokai.com.mx.SIGMAV2.shared.response.ApiResponse;
import tokai.com.mx.SIGMAV2.shared.response.ResponseHelper;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/sigmav2/auth")
@PreAuthorize("permitAll()")
public class AuthController {
    private final UserDetailsServicePer userDetailsServicePer;

    public AuthController(UserDetailsServicePer userDetailsServicePer) {
        this.userDetailsServicePer = userDetailsServicePer;
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

}