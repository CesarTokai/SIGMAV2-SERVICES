package tokai.com.mx.SIGMAV2.security.infrastructure.controller;


import jakarta.validation.Valid;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep1DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep2DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.ResponseAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.service.UserDetailsServicePer;

import org.springframework.http.HttpStatus;
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
    public String getMapping (){
        return "hola mundo";
    }

    @PostMapping("/")
    public ResponseEntity<ResponseAuthDTO> login(@RequestBody @Valid RequestAuthDTO authRequest){
        return new ResponseEntity<>(
                this.userDetailsServicePer.login(authRequest),
                HttpStatus.OK
        );
    }

    @PostMapping("/findUserToResetPassword")
    public ResponseEntity<Object> findUserToResetPassword(@RequestBody @Valid RequestResetPasswordStep1DTO payload){
        return new ResponseEntity<>(
                this.userDetailsServicePer.findUserToResetPassword(payload),
                HttpStatus.OK
        );
    }

    @PostMapping("/compareCodeToResetPassword")
    public ResponseEntity<Object> compareCodeToResetPassword(@RequestBody @Valid RequestResetPasswordStep2DTO payload){
        return new ResponseEntity<>(
                this.userDetailsServicePer.compareCodeToResetPassword(payload),
                HttpStatus.OK
        );
    }

}