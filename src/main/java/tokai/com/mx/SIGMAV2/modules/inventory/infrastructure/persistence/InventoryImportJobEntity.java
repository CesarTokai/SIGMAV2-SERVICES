package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory_import_jobs")
public class InventoryImportJobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "username")
    private String user;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "status")
    private String status;

    @Column(name = "inserted_rows")
    private Integer insertedRows;

    @Column(name = "updated_rows")
    private Integer updatedRows;

    @Column(name = "skipped_rows")
    private Integer skippedRows;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "errors_json", columnDefinition = "LONGTEXT")
    private String errorsJson;

    @Column(name = "log_file_path")
    private String logFilePath;

    @Column(name = "id_period")
    private Long idPeriod;

    @Column(name = "id_warehouse")
    private Long idWarehouse;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "created_by")
    private String createdBy;


}
