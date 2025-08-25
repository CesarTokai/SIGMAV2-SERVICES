package tokai.com.mx.SIGMAV2.modules.mail.infrastructure.service;


import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import tokai.com.mx.SIGMAV2.modules.mail.domain.port.output.MailSender;
import tokai.com.mx.SIGMAV2.shared.exception.CustomException;

@Service
public class MailSenderImpl implements MailSender {

    private final JavaMailSender javaMailSender;

    public MailSenderImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (to == null || to.isEmpty()) {
            throw new CustomException("Invalid email address.");
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            javaMailSender.send(message);

        } catch (Exception e) {
            throw new CustomException("Error sending email: " + e.getMessage());
        }
    }
}
