package tokai.com.mx.SIGMAV2.modules.users.domain.port.output;

/**
 * Puerto de salida para envío de correos
 * Esta interfaz NO debe tener anotaciones de framework
 */
public interface MailSender {
    
    /**
     * Envía un correo electrónico
     */
    void send(String to, String subject, String body);
}
