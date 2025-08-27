package tokai.com.mx.SIGMAV2.modules.users.infrastructure.mail;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.users.domain.port.output.MailSender;
import tokai.com.mx.SIGMAV2.modules.mail.infrastructure.service.MailSenderImpl;

/**
 * Adaptador que implementa el puerto de salida MailSender
 * Delega al servicio de infraestructura de mail
 */
@Component
public class MailSenderAdapter implements MailSender {
    
    private final MailSenderImpl mailSenderImpl;

    public MailSenderAdapter(MailSenderImpl mailSenderImpl) {
        this.mailSenderImpl = mailSenderImpl;
    }

    @Override
    public void send(String to, String subject, String body) {
        mailSenderImpl.send(to, subject, body);
    }
}
