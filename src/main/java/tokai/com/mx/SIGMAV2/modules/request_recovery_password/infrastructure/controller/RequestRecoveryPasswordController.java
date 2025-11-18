package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.application.service.RequestRecoveryPasswordService;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.RequestToResolveRequestDTO;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.VerifyEmailDTO;


@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/sigmav2/auth")
public class RequestRecoveryPasswordController {
    final RequestRecoveryPasswordService requestRecoveryPasswordService;
    private static final Logger log = LoggerFactory.getLogger(RequestRecoveryPasswordController.class);

    public RequestRecoveryPasswordController(RequestRecoveryPasswordService requestRecoveryPasswordService) {
        this.requestRecoveryPasswordService = requestRecoveryPasswordService;
    }

    @GetMapping("/getPage")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR')")
    public ResponseEntity<?> findRequest(Pageable pageable){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("findRequest invoked by={}, authenticated={}, authorities={}",
                auth == null ? null : auth.getName(),
                auth != null && auth.isAuthenticated(),
                auth == null ? null : auth.getAuthorities());
        return new ResponseEntity<>(
                this.requestRecoveryPasswordService.findRequest(pageable),
                HttpStatus.OK
        );
    }

    @GetMapping("/me")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> whoAmI() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> resp = new HashMap<>();
        if (auth == null) {
            resp.put("name", null);
            resp.put("authenticated", false);
            resp.put("authorities", null);
        } else {
            resp.put("name", auth.getName());
            resp.put("authenticated", auth.isAuthenticated());
            resp.put("authorities", auth.getAuthorities());
        }
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/resolveRequest")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR')")
    public ResponseEntity<?> resolveRequest(@RequestBody @Valid RequestToResolveRequestDTO payload){
        return new ResponseEntity<>(
                this.requestRecoveryPasswordService.completeRequest(payload),
                HttpStatus.OK
        );
    }

    @PostMapping("/rejectRequest")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR')")
    public ResponseEntity<?> rejectRequest(@RequestBody @Valid RequestToResolveRequestDTO payload){
        return new ResponseEntity<>(
                this.requestRecoveryPasswordService.rejectRequest(payload),
                HttpStatus.OK
        );
    }

    @PostMapping("/verifyUser")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> verifyUser(@RequestBody @Valid VerifyEmailDTO payload) {
        boolean success = requestRecoveryPasswordService.verifyUser(payload.email());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        
        if (success) {
            response.put("message", "Usuario verificado exitosamente");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Usuario no encontrado");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/createRequest")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> createPasswordRecoveryRequest(@RequestBody @Valid VerifyEmailDTO payload) {
        log.debug("POST /api/auth/createRequest body={}", payload);
        boolean success = requestRecoveryPasswordService.createRequest(payload.email());
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Solicitud de recuperación creada exitosamente" : "Error al crear la solicitud de recuperación");
        return ResponseEntity.status(success ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/getHistory")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR')")
    public ResponseEntity<?> getRequestHistory(Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("getRequestHistory invoked by={}, authenticated={}, authorities={}",
                auth == null ? null : auth.getName(),
                auth != null && auth.isAuthenticated(),
                auth == null ? null : auth.getAuthorities());
        // Implementa este método en el servicio para devolver solicitudes aceptadas y rechazadas
        return new ResponseEntity<>(
                this.requestRecoveryPasswordService.getRequestHistory(pageable),
                HttpStatus.OK
        );
    }

}
