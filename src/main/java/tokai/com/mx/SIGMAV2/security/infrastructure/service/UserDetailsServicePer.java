package tokai.com.mx.SIGMAV2.security.infrastructure.service;


import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import tokai.com.mx.SIGMAV2.modules.mail.infrastructure.service.MailSenderImpl;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.domain.model.BeanRequestStatus;
import tokai.com.mx.SIGMAV2.modules.request_recovery_password.infrastructure.repository.IRequestRecoveryPassword;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.UserRepository;
import tokai.com.mx.SIGMAV2.security.infrastructure.adapter.SecurityUserAdapter;
import tokai.com.mx.SIGMAV2.security.SecurityCode;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep1DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.RequestResetPasswordStep2DTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.dto.ResponseAuthDTO;
import tokai.com.mx.SIGMAV2.security.infrastructure.jwt.JwtUtils;
import tokai.com.mx.SIGMAV2.shared.exception.CustomException;
import tokai.com.mx.SIGMAV2.shared.audit.AuditService;
import tokai.com.mx.SIGMAV2.shared.audit.AuditEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class UserDetailsServicePer implements UserDetailsService {
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final JwtUtils jwtUtils;
    final MailSenderImpl mailService;
    final IRequestRecoveryPassword recoveryPasswordRepository;
    final SecurityUserAdapter securityUserAdapter;
    final AuditService auditService;

    @Value("${spring.datasource.url:unknown}")
    private String datasourceUrl;

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServicePer.class);

    public UserDetailsServicePer(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, MailSenderImpl mailService, IRequestRecoveryPassword recoveryPasswordRepository, SecurityUserAdapter securityUserAdapter, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.mailService = mailService;
        this.recoveryPasswordRepository = recoveryPasswordRepository;
        this.securityUserAdapter = securityUserAdapter;
        this.auditService = auditService;
    }


    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) {
    tokai.com.mx.SIGMAV2.modules.users.domain.model.User domainUser = userRepository.findByEmail(email)
        .orElseThrow(() -> new CustomException("User or password incorrect"));
    
    BeanUser user = securityUserAdapter.toLegacyUser(domainUser);

    List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
    authorityList.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toString()));

    return new User(
        user.getEmail(),
        user.getPasswordHash(),
        user.isVerified(),
        true,
        true,
        user.isStatus(),
        authorityList
    );
    }


    @Transactional
    public ResponseAuthDTO login(RequestAuthDTO authRequest) {
        try {
            // load user
            UserDetails userDetails = loadUserByUsername(authRequest.getEmail());

            if (userDetails == null)
                throw new CustomException("User or password incorrect");

            tokai.com.mx.SIGMAV2.modules.users.domain.model.User domainUser2 = userRepository.findByEmail(authRequest.getEmail()).orElseThrow(() -> new CustomException("User or password incorrect"));
            BeanUser user2 = securityUserAdapter.toLegacyUser(domainUser2);

            // Check if account is not verified
            if (!user2.isVerified()) {
                if (!passwordEncoder.matches(authRequest.getPassword(), user2.getPasswordHash())) {
                    throw new CustomException("Invalid credentials. Please try again.");
                }
                throw new CustomException("Account not verified");
            }

            // if user is blocked
            if (!userDetails.isAccountNonLocked()){
                if (user2.getLastTryAt() != null && user2.getLastTryAt().plusMinutes(5).isAfter(java.time.LocalDateTime.now())) {
                    throw new CustomException("User is blocked");
                } else {
                    user2.setStatus(true);
                    tokai.com.mx.SIGMAV2.modules.users.domain.model.User updatedDomain = securityUserAdapter.toDomainUser(user2);
                    userRepository.save(updatedDomain);
                }
            }

            // validate password
            if (!passwordEncoder.matches(authRequest.getPassword(), userDetails.getPassword())) {
                user2.setAttempts(user2.getAttempts() + 1);
                if (user2.getAttempts() == 3) {
                    user2.setStatus(false);
                    user2.setLastTryAt(java.time.LocalDateTime.now());
                    user2.setAttempts(0);
                }
                user2.setLastTryAt(java.time.LocalDateTime.now());
                tokai.com.mx.SIGMAV2.modules.users.domain.model.User updatedDomain = securityUserAdapter.toDomainUser(user2);
                userRepository.save(updatedDomain);
                throw new CustomException("User or password incorrect");
            }

            // Si el usuario no está habilitado, permitir solo si tiene rol ADMINISTRADOR
            boolean hasAdminRole = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMINISTRADOR"));
            if (!userDetails.isEnabled() && !hasAdminRole) {
                throw new CustomException("account not verified");
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails.getUsername(),
                    userDetails.getPassword(),
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtUtils.createToken(authentication);

            // marcar usuario como activo y actualizar last_login_at
            user2.setStatus(true);
            user2.setLastLoginAt(java.time.LocalDateTime.now());
            user2.setLastActivityAt(java.time.LocalDateTime.now());
            tokai.com.mx.SIGMAV2.modules.users.domain.model.User updatedDomain = securityUserAdapter.toDomainUser(user2);
            userRepository.save(updatedDomain);

            // registrar auditoría de login exitoso
            try {
                AuditEntry entry = new AuditEntry();
                entry.setAction("LOGIN");
                entry.setResourceType("Auth");
                entry.setOutcome("SUCCESS");
                entry.setPrincipal(user2.getEmail());
                entry.setDetails("Login successful");
                auditService.log(entry);
            } catch (Exception ignore) {
                // no propagar errores de auditoría
            }

            ResponseAuthDTO response = new ResponseAuthDTO();
            response.setEmail(user2.getEmail());
            response.setToken(accessToken);
            response.setRole(user2.getRole().toString()); // Agregar el rol a la respuesta
            return response;
        } catch (Exception e) {
            // registrar auditoría de fallo en login
            try {
                AuditEntry entry = new AuditEntry();
                entry.setAction("LOGIN");
                entry.setResourceType("Auth");
                entry.setOutcome("FAILURE");
                entry.setPrincipal(authRequest != null ? authRequest.getEmail() : null);
                entry.setDetails(e.getMessage());
                auditService.log(entry);
            } catch (Exception ignore) {
                // ignore
            }
            throw e;
        }
    }

    @Transactional
    public Object findUserToResetPassword(RequestResetPasswordStep1DTO payload) {
        log.debug("Using datasource: {}", datasourceUrl);
        log.debug("findUserToResetPassword invoked with payload: {}", payload);
        Optional<tokai.com.mx.SIGMAV2.modules.users.domain.model.User> domainUserOpt = userRepository.findByEmail(payload.Email());

        if (domainUserOpt.isEmpty()){
            log.debug("User not found for email={}", payload.Email());
            return true;
        }

        BeanUser user = securityUserAdapter.toLegacyUser(domainUserOpt.get());

        String code = SecurityCode.generate();
        log.debug("Generated verification code for {}: {}", user.getEmail(), code);

        // First persist the code so failures in email sending won't rollback the DB change
        user.setVerificationCode(code);
        tokai.com.mx.SIGMAV2.modules.users.domain.model.User updatedDomain = securityUserAdapter.toDomainUser(user);
        log.debug("Saving updated user verification code for email={}", user.getEmail());
        tokai.com.mx.SIGMAV2.modules.users.domain.model.User saved = userRepository.save(updatedDomain);
        log.debug("Saved user verificationCode for email={}, savedId={}, verificationCode={}", user.getEmail(), saved.getId(), saved.getVerificationCode());

        Map<String, Object> debug = new HashMap<>();
        debug.put("email", user.getEmail());
        debug.put("verificationCode", code);
        debug.put("userSavedId", saved.getId());

        // Create a pending request record so the request_recovery_password table reflects the reset attempt
        try {
            BeanRequestRecoveryPassword req = new BeanRequestRecoveryPassword();
            req.setUser(user);
            req.setStatus(BeanRequestStatus.PENDING);
            req.setDate(java.time.LocalDate.now());

            BeanRequestRecoveryPassword savedReq = recoveryPasswordRepository.saveAndFlush(req);
            log.debug("Created password recovery request id={} for user={}", savedReq.getRequestId(), user.getEmail());
            debug.put("requestSaved", true);
            debug.put("requestId", savedReq.getRequestId());
        } catch (Exception e) {
            // Log but don't break the flow: we still want to send the email and not roll back the code update
            log.error("Failed to persist password recovery request for user {}: {}", user.getEmail(), e.getMessage(), e);
            debug.put("requestSaved", false);
            debug.put("requestError", e.getMessage());
        }

        // Now try to send the email, but don't let mail errors rollback the transaction
        try {
            mailService.send(user.getEmail(), "Reset password", "Your code is: " + code);
            log.debug("Mail sent to {} (masked) for password reset", user.getEmail());
            debug.put("mailSent", true);
        } catch (Exception mailEx) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), mailEx.getMessage(), mailEx);
            // Do not rethrow: persist the code regardless of mail outcome
            debug.put("mailSent", false);
            debug.put("mailError", mailEx.getMessage());
        }

        return debug;
    }

    @Transactional
    public Object compareCodeToResetPassword(RequestResetPasswordStep2DTO payload) {
        log.debug("Using datasource: {}", datasourceUrl);
        log.debug("compareCodeToResetPassword invoked with payload: {}", payload);
        Optional<tokai.com.mx.SIGMAV2.modules.users.domain.model.User> domainUserOpt = userRepository.findByEmail(payload.Email());

        if (domainUserOpt.isEmpty()){
            log.debug("User not found for email={}", payload.Email());
            throw new CustomException("Something went wrong");
        }

        BeanUser user = securityUserAdapter.toLegacyUser(domainUserOpt.get());

        if (!user.getVerificationCode().equals(payload.verificationCode())){
            log.debug("Verification code mismatch for email={}", user.getEmail());
            throw new CustomException("Verification code is incorrect");
        }

        int count = recoveryPasswordRepository.countAllByUserAndStatus(user, BeanRequestStatus.PENDING);
        log.debug("Pending password recovery requests for {} = {}", user.getEmail(), count);
        if (count > 0){
            log.debug("User {} has pending requests, aborting", user.getEmail());
            throw new CustomException("You have a pending request");
        }

        BeanRequestRecoveryPassword request = new BeanRequestRecoveryPassword();
        request.setUser(user);
        request.setStatus(BeanRequestStatus.PENDING);
        request.setDate(java.time.LocalDate.now());

        log.debug("Saving password recovery request for user={}", user.getEmail());
        BeanRequestRecoveryPassword saved = recoveryPasswordRepository.saveAndFlush(request);
        log.debug("Saved password recovery request id={} for user={}", saved.getRequestId(), user.getEmail());

        return true;
    }
}
