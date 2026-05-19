package tokai.com.mx.SIGMAV2.modules.mail.domain.port.output;

public interface MailSender {

    void send(String to, String subject, String body);

}
