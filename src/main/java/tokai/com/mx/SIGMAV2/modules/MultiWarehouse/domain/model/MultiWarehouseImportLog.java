package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "multiwarehouse_import_log")
@Data
public class MultiWarehouseImportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String period;
    private LocalDateTime importDate;
    private String status; // Ejemplo: SUCCESS, ERROR
    private String message;
}

