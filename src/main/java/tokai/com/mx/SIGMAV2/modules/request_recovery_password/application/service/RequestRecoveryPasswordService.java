package tokai.com.mx.SIGMAV2.modules.request_recovery_password.application.service;

import lombok.extern.slf4j.Slf4j;
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
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.mapper.UserDomainMapper;
import tokai.com.mx.SIGMAV2.security.SecurityCode;
import tokai.com.mx.SIGMAV2.security.SessionInformation;
import tokai.com.mx.SIGMAV2.shared.exception.CustomException;
import tokai.com.mx.SIGMAV2.shared.exception.UserNotFoundException;
import tokai.com.mx.SIGMAV2.shared.exception.UnauthorizedAccessException;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository.IRequestRecoveryPassword;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
public class RequestRecoveryPasswordService {
    final IRequestRecoveryPassword requestRecoveryPasswordRepository;
    final MailSenderImpl mailSenderImpl;
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final UserDomainMapper userMapper;

    private static final Logger logger = LoggerFactory.getLogger(RequestRecoveryPasswordService.class);

    public RequestRecoveryPasswordService(
            IRequestRecoveryPassword requestRecoveryPasswordRepository,
            MailSenderImpl mailService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserDomainMapper userMapper
    ) {
        this.requestRecoveryPasswordRepository = requestRecoveryPasswordRepository;
        this.mailSenderImpl = mailService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public Page<ResponsePageRequestRecoveryDTO> findRequest(Pageable pageable){
        String email = SessionInformation.getUserName();
        String role = SessionInformation.getRole();

        log.info("Buscando solicitudes de recuperación para usuario: {} con rol: {}", email, role);

        // Validar usuario y rol
        validateUserAndRole(email, role);

        try {
            // Determinar solicitudes según jerarquía de roles
            switch(role.toUpperCase()) {
                case "ADMINISTRADOR":
                    return requestRecoveryPasswordRepository.getRequestByRole(
                            ERole.ALMACENISTA, BeanRequestStatus.PENDING, pageable);
                case "ALMACENISTA":
                    return requestRecoveryPasswordRepository.getRequestByRole(
                            ERole.AUXILIAR, BeanRequestStatus.PENDING, pageable);
                case "AUXILIAR":
                    return requestRecoveryPasswordRepository.getRequestByRole(
                            ERole.AUXILIAR_DE_CONTEO, BeanRequestStatus.PENDING, pageable);
                default:
                    log.warn("Rol sin permisos para ver solicitudes: {}", role);
                    throw new UnauthorizedAccessException(
                            "No tienes permisos para ver solicitudes de recuperación de contraseña");
            }
        } catch (Exception e) {
            log.error("Error al buscar solicitudes: {}", e.getMessage(), e);
            throw new CustomException("Error interno al buscar solicitudes de recuperación");
        }
    }

    @Transactional
    public String completeRequest(RequestToResolveRequestDTO payload){
        String email = SessionInformation.getUserName();
        String role = SessionInformation.getRole();

        log.info("Completando solicitud de recuperación por usuario: {}", email);

        // Validaciones de entrada
        if(payload == null) {
            throw new IllegalArgumentException("Los datos de la solicitud son obligatorios");
        }

        if(payload.getRequestId() == null) {
            throw new IllegalArgumentException("El ID de la solicitud es obligatorio");
        }

        // Validar usuario y rol
        var userDomain = validateUserAndRole(email, role);
        BeanUser user = userMapper.toEntity(userDomain);

        Optional<BeanRequestRecoveryPassword> request = requestRecoveryPasswordRepository.findById(payload.getRequestId());
        if(request.isEmpty())
            throw new CustomException("Something went wrong");

        if(request.get().getStatus() == BeanRequestStatus.ACCEPTED)
            throw new CustomException("The request is already resolved");

        if(request.get().getStatus() == BeanRequestStatus.REJECTED)
            throw new CustomException("The request was rejected by the user");

        // Validación de rol
        if(!role.equals("ADMINISTRADOR")){
            if(!user.getRole().equals(request.get().getUser().getRole()))
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
        userToUpdate.setPasswordChangedAt(java.time.LocalDateTime.now());
        userRepository.save(userMapper.toDomain(userToUpdate));
        request.get().setStatus(BeanRequestStatus.ACCEPTED);
        requestRecoveryPasswordRepository.save(request.get());
        // Devolver la contraseña generada para mostrar en el frontend
        return newPass;
    }

    @Transactional
    public Object rejectRequest(RequestToResolveRequestDTO payload) {
        String email = SessionInformation.getUserName();
        String role = SessionInformation.getRole();

        var userDomain = userRepository.findByEmail(email);
        if(userDomain.isEmpty())
            throw new CustomException("Something went wrong");
        BeanUser user = userMapper.toEntity(userDomain.get());

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
            if(!user.getRole().equals(request.get().getUser().getRole()))
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
        logger.debug("createRequest invoked for email={}", email);
        var userDomain = userRepository.findByEmail(email);
        if (userDomain.isEmpty()) {
            logger.debug("Usuario no existe: {}", email);
            throw new IllegalArgumentException("El usuario no existe.");
        }
        BeanUser existingUser = userMapper.toEntity(userDomain.get());
        int pendingRequests = requestRecoveryPasswordRepository.countAllByUserAndStatus(existingUser, BeanRequestStatus.PENDING);
        logger.debug("Pending requests for user {} = {}", email, pendingRequests);
        if (pendingRequests > 0) {
            logger.debug("Ya existe una solicitud pendiente para: {}", email);
            throw new IllegalStateException("Ya existe una solicitud pendiente para este usuario.");
        }
        BeanRequestRecoveryPassword request = new BeanRequestRecoveryPassword();
        request.setUser(existingUser);
        request.setStatus(BeanRequestStatus.PENDING);
        request.setDate(LocalDate.now());
        logger.debug("Saving request entity for user={}", email);
        BeanRequestRecoveryPassword saved = requestRecoveryPasswordRepository.save(request);
        logger.debug("Saved request id={}, user={}", saved.getRequestId(), saved.getUser() != null ? saved.getUser().getEmail() : null);
        return true;
    }

    @Transactional(readOnly = true)
    public boolean verifyUser(String email) {
        var user = userRepository.findByEmail(email);
        return user.isPresent();
    }

    @Transactional(readOnly = true)
    public Page<ResponsePageRequestRecoveryDTO> getRequestHistory(Pageable pageable) {
        String email = SessionInformation.getUserName();
        String role = SessionInformation.getRole();
        log.info("Buscando historial de solicitudes para usuario: {} con rol: {}", email, role);

        // Validar usuario y rol
        validateUserAndRole(email, role);

        try {
            switch(role.toUpperCase()) {
                case "ADMINISTRADOR":
                    // Ver historial de solicitudes de ALMACENISTA aceptadas y rechazadas
                    return requestRecoveryPasswordRepository.getRequestByRoleAndStatuses(
                            ERole.ALMACENISTA, List.of(BeanRequestStatus.ACCEPTED, BeanRequestStatus.REJECTED), pageable);
                case "ALMACENISTA":
                    return requestRecoveryPasswordRepository.getRequestByRoleAndStatuses(
                            ERole.AUXILIAR, List.of(BeanRequestStatus.ACCEPTED, BeanRequestStatus.REJECTED), pageable);
                case "AUXILIAR":
                    return requestRecoveryPasswordRepository.getRequestByRoleAndStatuses(
                            ERole.AUXILIAR_DE_CONTEO, List.of(BeanRequestStatus.ACCEPTED, BeanRequestStatus.REJECTED), pageable);
                default:
                    log.warn("Rol sin permisos para ver historial: {}", role);
                    throw new UnauthorizedAccessException(
                            "No tienes permisos para ver el historial de solicitudes de recuperación de contraseña");
            }
        } catch (Exception e) {
            log.error("Error al buscar historial de solicitudes: {}", e.getMessage(), e);
            throw new CustomException("Error interno al buscar historial de solicitudes de recuperación");
        }
    }

    /**
     * Valida que el usuario y rol existan y sean válidos
     * @param email Email del usuario en sesión
     * @param role Rol del usuario en sesión
     * @return User del dominio si las validaciones pasan
     * @throws UserNotFoundException si el usuario no existe
     * @throws UnauthorizedAccessException si el rol es inválido
     */
    private tokai.com.mx.SIGMAV2.modules.users.domain.model.User validateUserAndRole(String email, String role) {
        // Validar que el usuario existe
        var userDomain = userRepository.findByEmail(email);
        if(userDomain.isEmpty()) {
            log.error("Usuario no encontrado en sesión: {}", email);
            throw new UserNotFoundException("Usuario no encontrado o sesión inválida");
        }

        // Validar que el rol está definido
        if(role == null || role.isBlank()) {
            log.error("Rol no definido para usuario: {}", email);
            throw new UnauthorizedAccessException("Rol de usuario no definido");
        }

        return userDomain.get();
    }
}