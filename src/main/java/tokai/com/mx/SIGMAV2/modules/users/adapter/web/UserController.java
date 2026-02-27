package tokai.com.mx.SIGMAV2.modules.users.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.RegisterUserCommand;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.input.UserService;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.ResendVerificationCodeRequest;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserDomainResponse;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.VerifyUserRequest;
import tokai.com.mx.SIGMAV2.modules.users.domain.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody UserRequest request) {
        log.info("Recibida solicitud de registro para email: {}", request.getEmail());

        RegisterUserCommand command = new RegisterUserCommand(
                request.getEmail(), request.getPassword(), request.getRole(),
                request.getName(), request.getFirstLastName(), request.getSecondLastName(),
                request.getPhoneNumber(), request.getComments(),
                request.getStatus(), request.getPreVerified()
        );

        User user = userService.register(command);
        UserDomainResponse userResponse = mapToUserDomainResponse(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario registrado exitosamente. Revisa tu correo para verificar tu cuenta.");
        response.put("data", userResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> existsByEmail(@RequestParam String email) {
        log.info("Verificando existencia de usuario: {}", email);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", userService.existsByEmail(email));
        response.put("email", email);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta un usuario por email — requiere autenticación para evitar
     * enumeración de usuarios desde el exterior.
     */
    @GetMapping("/{email}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> getByEmail(@PathVariable String email) {
        log.info("Buscando usuario: {}", email);

        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Usuario no encontrado");
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", mapToUserDomainResponse(userOpt.get()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{email}/connected")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
    public ResponseEntity<Map<String, Object>> isConnected(@PathVariable String email) {
        log.info("Consultando estado de conexión para usuario: {}", email);

        Optional<User> userOpt = userService.findByEmail(email);
        Map<String, Object> response = new HashMap<>();
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Usuario no encontrado");
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = userOpt.get();
        response.put("success", true);
        response.put("email", user.getEmail());
        response.put("connected", user.isStatus());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{email}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Map<String, Object>> deleteByEmail(@PathVariable String email) {
        log.info("Eliminando usuario: {}", email);
        userService.deleteByEmail(email);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario eliminado exitosamente");
        response.put("email", email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody VerifyUserRequest request) {
        log.info("Verificando usuario: {}", request.getEmail());

        Optional<User> userOpt = userService.verify(request.getEmail(), request.getCode());
        User user = userOpt.orElseThrow(() -> new RuntimeException("Usuario no encontrado al verificar"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario verificado exitosamente. Tu cuenta ya está activa.");
        response.put("data", mapToUserDomainResponse(user));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<Map<String, Object>> resendVerificationCode(
            @RequestBody ResendVerificationCodeRequest request) {
        log.info("Solicitando reenvío de código de verificación para: {}", request.getEmail());
        try {
            userService.resendVerificationCode(request.getEmail());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Código de verificación reenviado exitosamente. Revisa tu correo electrónico.");
            response.put("email", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al reenviar código de verificación para {}: {}", request.getEmail(), e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("email", request.getEmail());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private UserDomainResponse mapToUserDomainResponse(User user) {
        return new UserDomainResponse(
                user.getId(), user.getEmail(), user.getRole().name(),
                user.isStatus(), user.isVerified(), user.getAttempts(),
                user.getLastTryAt(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}