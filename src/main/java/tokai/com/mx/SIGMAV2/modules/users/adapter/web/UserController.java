package tokai.com.mx.SIGMAV2.modules.users.adapter.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tokai.com.mx.SIGMAV2.modules.users.application.service.UserServiceImpl;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserRequest;
import tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto.UserResponse;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody UserRequest request) {
        log.info("Recibida solicitud de registro para email: {}", request.getEmail());
        
        BeanUser user = userService.register(request);
        
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isStatus(),
                user.isVerified(),
                user.getAttempts(),
                user.getLastTryAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario registrado exitosamente. Revisa tu correo para verificar tu cuenta.");
        response.put("data", userResponse);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> existsByEmail(@RequestParam String email) {
        log.info("Verificando existencia de usuario: {}", email);
        
        boolean exists = userService.existsByEmail(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("email", email);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{email}")
    public ResponseEntity<Map<String, Object>> getByEmail(@PathVariable String email) {
        log.info("Buscando usuario: {}", email);
        
        Optional<BeanUser> userOpt = userService.findByUsername(email);
        
        if (userOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Usuario no encontrado");
            response.put("email", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        BeanUser user = userOpt.get();
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isStatus(),
                user.isVerified(),
                user.getAttempts(),
                user.getLastTryAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userResponse);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<Map<String, Object>> deleteByEmail(@PathVariable String email) {
        log.info("Eliminando usuario: {}", email);
        
        userService.deleteByUsername(email);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario eliminado exitosamente");
        response.put("email", email);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestParam String email, @RequestParam String code) {
        log.info("Verificando usuario: {}", email);
        
        Optional<BeanUser> userOpt = userService.verifyByUsernameAndCode(email, code);
        
        BeanUser user = userOpt.get(); // El servicio ya maneja el caso de Optional vacío con excepción
        
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isStatus(),
                user.isVerified(),
                user.getAttempts(),
                user.getLastTryAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Usuario verificado exitosamente. Tu cuenta ya está activa.");
        response.put("data", userResponse);
        
        return ResponseEntity.ok(response);
    }
}