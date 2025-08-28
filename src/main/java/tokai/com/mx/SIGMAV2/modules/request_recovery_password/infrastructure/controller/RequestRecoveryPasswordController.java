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

import jakarta.validation.Valid;
import java.util.Map;
import java.util.HashMap;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.application.service.RequestRecoveryPasswordService;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.RequestToResolveRequestDTO;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.VerifyEmailDTO;


@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/sigmav2/request-recovery-password")
public class RequestRecoveryPasswordController {
    final RequestRecoveryPasswordService requestRecoveryPasswordService;

    public RequestRecoveryPasswordController(RequestRecoveryPasswordService requestRecoveryPasswordService) {
        this.requestRecoveryPasswordService = requestRecoveryPasswordService;
    }

    @GetMapping("/getPage")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR')")
    public ResponseEntity<?> findRequest(Pageable pageable){
        return new ResponseEntity<>(
                this.requestRecoveryPasswordService.findRequest(pageable),
                HttpStatus.OK
        );
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
        boolean success = requestRecoveryPasswordService.createRequest(payload.email());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        
        if (success) {
            response.put("message", "Solicitud de recuperación creada exitosamente");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("message", "Error al crear la solicitud de recuperación");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


}
