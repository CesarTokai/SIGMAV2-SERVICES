package tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.application.service.RequestRecoveryPasswordService;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.RequestToResolveRequestDTO;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.VerifyEmailDTO;


@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/sigmav2/request-recovery-password")
@PreAuthorize("denyAll()")
public class RequestRecoveryPasswordController {
    final RequestRecoveryPasswordService requestRecoveryPasswordService;

    public RequestRecoveryPasswordController(RequestRecoveryPasswordService requestRecoveryPasswordService) {
        this.requestRecoveryPasswordService = requestRecoveryPasswordService;
    }

    @GetMapping("/getPage")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR')")
    public ResponseEntity<Page<?>> findRequest(Pageable pageable){
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
        boolean userExists = requestRecoveryPasswordService.verifyUser(payload.email());
        return userExists
                ? ResponseEntity.ok("Usuario válido.")
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe.");
    }


    @PostMapping("/createRequest")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> createPasswordRecoveryRequest(@RequestBody @Valid VerifyEmailDTO payload) {
        boolean result = requestRecoveryPasswordService.createRequest(payload.email());
        if (result) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Solicitud registrada exitosamente.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se pudo procesar la solicitud.");
    }


}
