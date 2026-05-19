package tokai.com.mx.SIGMAV2.shared.audit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    @Async("auditThreadPool")
    public void log(AuditEntry entry) {
        try {
            repository.save(entry);
        } catch (Exception e) {
            // No lanzar excepciones desde la auditoría para no afectar el flujo principal
            // Registrar a log si hay logger disponible (opcional)
            System.err.println("Error al guardar audit entry: " + e.getMessage());
        }
    }
}

