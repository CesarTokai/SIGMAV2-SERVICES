package tokai.com.mx.SIGMAV2.shared.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence.JpaPersonalInformationRepository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;
    private final JpaUserRepository userRepository;
    private final JpaPersonalInformationRepository personalRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public AuditAspect(AuditService auditService,
                       JpaUserRepository userRepository,
                       JpaPersonalInformationRepository personalRepo) {
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.personalRepo = personalRepo;
    }

    @Around("@annotation(auditable) || @within(auditable)")
    public Object aroundAuditable(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        AuditEntry entry = new AuditEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setCreatedAt(LocalDateTime.now());

        // Action / resource
        String action = (auditable != null && !auditable.action().isEmpty()) ? auditable.action() : pjp.getSignature().toShortString();
        entry.setAction(action);
        if (auditable != null && !auditable.resource().isEmpty()) entry.setResourceType(auditable.resource());

        // HTTP request info if available
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest request = sra.getRequest();
            entry.setClientIp(request.getRemoteAddr());
            entry.setUserAgent(request.getHeader("User-Agent"));
        }

        // Resolve principal (email) from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            String principal = auth.getName();
            entry.setPrincipal(principal);

            // Try to resolve userId and personal info to get full name
            try {
                Optional<BeanUser> bu = userRepository.findByEmail(principal);
                if (bu.isPresent()) {
                    BeanUser beanUser = bu.get();
                    Long userId = beanUser.getId();
                    Optional<BeanPersonalInformation> pi = personalRepo.findByUser_UserId(userId);
                    if (pi.isPresent()) {
                        BeanPersonalInformation info = pi.get();
                        StringBuilder full = new StringBuilder();
                        if (info.getName() != null) full.append(info.getName());
                        if (info.getFirstLastName() != null) full.append(" ").append(info.getFirstLastName());
                        if (info.getSecondLastName() != null) full.append(" ").append(info.getSecondLastName());
                        entry.setPrincipalName(full.toString().trim());
                    }
                }
            } catch (Exception ignore) {
                // No propagar errores de auditoría
            }
        }

        try {
            Object result = pjp.proceed();
            entry.setOutcome("SUCCESS");
            // Optionally store a small result snapshot
            try {
                String details = mapper.writeValueAsString(Map.of("args", pjp.getArgs()));
                entry.setDetails(details);
            } catch (Exception e) {
                // ignore
            }
            auditService.log(entry);
            return result;
        } catch (Throwable ex) {
            entry.setOutcome("FAILURE");
            try {
                entry.setDetails(mapper.writeValueAsString(Map.of("exception", ex.getClass().getSimpleName(), "message", ex.getMessage())));
            } catch (Exception e) {
                // ignore
            }
            auditService.log(entry);
            throw ex;
        }
    }
}

