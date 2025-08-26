
package tokai.com.mx.SIGMAV2.modules.request_recovery_password.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.RequestToResolveRequestDTO;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.dto.ResponsePageRequestRecoveryDTO;
import tokai.com.mx.SIGMAV2.modules.mail.infrastructure.service.MailSenderImpl;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestStatus;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.model.ERole;
import tokai.com.mx.SIGMAV2.modules.users.port.out.UserRepository;
import tokai.com.mx.SIGMAV2.security.SecurityCode;
import tokai.com.mx.SIGMAV2.security.SessionInformation;
import tokai.com.mx.SIGMAV2.shared.exception.CustomException;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository.IRequestRecoveryPassword;


@Service
public class RequestRecoveryPasswordService {
    final IRequestRecoveryPassword requestRecoveryPasswordRepository;
    final MailSenderImpl mailSenderImpl;
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final SessionInformation sessionInformation;
    final RequestRecoveryPasswordService recoveryPasswordService;

    public RequestRecoveryPasswordService(
        IRequestRecoveryPassword requestRecoveryPasswordRepository,
        MailSenderImpl mailService,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        SessionInformation sessionInformation,
        RequestRecoveryPasswordService recoveryPasswordService
    ) {
        this.requestRecoveryPasswordRepository = requestRecoveryPasswordRepository;
        this.mailSenderImpl = mailService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionInformation = sessionInformation;
        this.recoveryPasswordService = recoveryPasswordService;
    }

    @Transactional
    public Page<ResponsePageRequestRecoveryDTO> findRequest(Pageable pageable){
        String email = SessionInformation.getUserName();
        String role = SessionInformation.getRole();

        Optional<BeanUser> user = userRepository.findByEmail(email);
        if(user.isEmpty())
            throw new CustomException("Something went wrong");

        if(role.equals("ADMINISTRADOR")){
            return requestRecoveryPasswordRepository.getRequestByRole(ERole.ALMACENISTA, BeanRequestStatus.PENDING, pageable);
        }
        if(role.equals("ALMACENISTA")){
            return requestRecoveryPasswordRepository.getRequestByRole(ERole.AUXILIAR, BeanRequestStatus.PENDING, pageable);
        }
        if(role.equals("AUXILIAR")){
            return requestRecoveryPasswordRepository.getRequestByRole(ERole.AUXILIAR_DE_CONTEO, BeanRequestStatus.PENDING, pageable);
        }
        return Page.empty();
    }

    @Transactional
    public Object completeRequest(RequestToResolveRequestDTO payload){
        String email = SessionInformation.getUserName();
        String role = SessionInformation.getRole();

        Optional<BeanUser> user = userRepository.findByEmail(email);
        if(user.isEmpty())
            throw new CustomException("Something went wrong");

        if(payload.getRequestId() == null)
            throw new CustomException("Something went wrong");

        Optional<BeanRequestRecoveryPassword> request = requestRecoveryPasswordRepository.findById(payload.getRequestId());
        if(request.isEmpty())
            throw new CustomException("Something went wrong");

        if(request.get().getStatus() == BeanRequestStatus.ACCEPTED)
            throw new CustomException("The request is already resolved");

        if(request.get().getStatus() == BeanRequestStatus.REJECTED)
            throw new CustomException("The request was rejected by the user");

        // Validación de rol
        if(!role.equals("ADMINISTRADOR")){
            if(!user.get().getRole().equals(request.get().getUser().getRole()))
                throw new CustomException("Something went wrong");
        }else{
            if(!request.get().getUser().getRole().equals(ERole.ALMACENISTA))
                throw new CustomException("Something went wrong");
        }

        BeanUser userToUpdate = request.get().getUser();
        if (!userToUpdate.isVerified() || !userToUpdate.isStatus()) {
            throw new CustomException("The account is either not verified or blocked");
        }
        String newPass = SecurityCode.generateAlphanumeric();
        String encodedPass = passwordEncoder.encode(newPass);
        userToUpdate.setPasswordHash(encodedPass);
        userRepository.save(userToUpdate);
        request.get().setStatus(BeanRequestStatus.ACCEPTED);
        requestRecoveryPasswordRepository.save(request.get());
        mailSenderImpl.send(userToUpdate.getEmail(), "Password recovery", "Your new password is: " + newPass);
        return true;
    }

    @Transactional
    public Object rejectRequest(RequestToResolveRequestDTO payload) {
        String email = SessionInformation.getUserName();
        String role = SessionInformation.getRole();

        Optional<BeanUser> user = userRepository.findByEmail(email);
        if(user.isEmpty())
            throw new CustomException("Something went wrong");

        if(payload.getRequestId() == null)
            throw new CustomException("Something went wrong");

        Optional<BeanRequestRecoveryPassword> request = requestRecoveryPasswordRepository.findById(payload.getRequestId());
        if(request.isEmpty())
            throw new CustomException("Something went wrong");

        if(request.get().getStatus() == BeanRequestStatus.ACCEPTED)
            throw new CustomException("The request is already resolved");

        if(request.get().getStatus() == BeanRequestStatus.REJECTED)
            throw new CustomException("The request was rejected by the user");

        if(!role.equals("ADMINISTRADOR")){
            if(!user.get().getRole().equals(request.get().getUser().getRole()))
                throw new CustomException("Something went wrong");
        }else{
            if(!request.get().getUser().getRole().equals(ERole.ALMACENISTA))
                throw new CustomException("Something went wrong");
        }

        request.get().setStatus(BeanRequestStatus.REJECTED);
        requestRecoveryPasswordRepository.save(request.get());
        mailSenderImpl.send(request.get().getUser().getEmail(), "Password recovery", "Your request was rejected");
        return true;
    }

    @Transactional
    public boolean createRequest(String email) {
        Optional<BeanUser> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("El usuario no existe.");
        }
        BeanUser existingUser = user.get();
        int pendingRequests = requestRecoveryPasswordRepository.countAllByUserAndStatus(existingUser, BeanRequestStatus.PENDING);
        if (pendingRequests > 0) {
            throw new IllegalStateException("Ya existe una solicitud pendiente para este usuario.");
        }
        BeanRequestRecoveryPassword request = new BeanRequestRecoveryPassword();
        request.setUser(existingUser);
        request.setStatus(BeanRequestStatus.PENDING);
        request.setDate(LocalDate.now());
        requestRecoveryPasswordRepository.save(request);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean verifyUser(String email) {
        Optional<BeanUser> user = userRepository.findByEmail(email);
        return user.isPresent();
    }
}