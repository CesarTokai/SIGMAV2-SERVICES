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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserDetailsServicePer implements UserDetailsService {
    final UserRepository userRepository;
    final PasswordEncoder passwordEncoder;
    final JwtUtils jwtUtils;
    final MailSenderImpl mailService;
    final IRequestRecoveryPassword recoveryPasswordRepository;
    final SecurityUserAdapter securityUserAdapter;

    public UserDetailsServicePer(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils, MailSenderImpl mailService, IRequestRecoveryPassword recoveryPasswordRepository, SecurityUserAdapter securityUserAdapter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.mailService = mailService;
        this.recoveryPasswordRepository = recoveryPasswordRepository;
        this.securityUserAdapter = securityUserAdapter;
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

        if(!userDetails.isEnabled() && !userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()).get(0).equals("ROLE_ADMINISTRADOR")){
            throw new CustomException("account not verified");
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails.getUsername(),
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtils.createToken(authentication);

        ResponseAuthDTO response = new ResponseAuthDTO();
        response.setEmail(user2.getEmail());
        response.setToken(accessToken);
        response.setRole(user2.getRole().toString()); // Agregar el rol a la respuesta
        return response;
    }

    @Transactional
    public Object findUserToResetPassword(RequestResetPasswordStep1DTO payload) {
        Optional<tokai.com.mx.SIGMAV2.modules.users.domain.model.User> domainUserOpt = userRepository.findByEmail(payload.Email());

        if (domainUserOpt.isEmpty())
            return true;

        BeanUser user = securityUserAdapter.toLegacyUser(domainUserOpt.get());

    String code = SecurityCode.generate();
    mailService.send(user.getEmail(), "Reset password", "Your code is: " + code);

        user.setVerificationCode(code);
        tokai.com.mx.SIGMAV2.modules.users.domain.model.User updatedDomain = securityUserAdapter.toDomainUser(user);
        userRepository.save(updatedDomain);

        return true;
    }

    @Transactional
    public Object compareCodeToResetPassword(RequestResetPasswordStep2DTO payload) {
        Optional<tokai.com.mx.SIGMAV2.modules.users.domain.model.User> domainUserOpt = userRepository.findByEmail(payload.Email());

        if (domainUserOpt.isEmpty())
            throw new CustomException("Something went wrong");

        BeanUser user = securityUserAdapter.toLegacyUser(domainUserOpt.get());

        if (!user.getVerificationCode().equals(payload.verificationCode()))
            throw new CustomException("Verification code is incorrect");

        int count = recoveryPasswordRepository.countAllByUserAndStatus(user, BeanRequestStatus.PENDING);
        if (count > 0)
            throw new CustomException("You have a pending request");

        BeanRequestRecoveryPassword request = new BeanRequestRecoveryPassword();
        request.setUser(user);
        request.setStatus(BeanRequestStatus.PENDING);
        request.setDate(java.time.LocalDate.now());

        recoveryPasswordRepository.save(request);

        return true;
    }
}
