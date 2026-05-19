package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "multiwarehouse_import_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultiWarehouseImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String period;

    @Column(nullable = false)
    private LocalDateTime importDate;

    @Column(nullable = false)
    private String status;

    @Column(length = 1000)
    private String message;

    @Column(length = 64)
    private String fileHash; // SHA-256 del archivo importado

    @Column(length = 20)
    private String stage; // Etapa de importación (opcional)
}
