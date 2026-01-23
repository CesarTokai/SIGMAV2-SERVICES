package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class InventoryImportResultDTO {
    private int totalRows;
    private int inserted;
    private int updated;
    private int deactivated;
    private List<String> errors;
    private String logFileUrl;

    public InventoryImportResultDTO() {
    }

    public InventoryImportResultDTO(int totalRows, int inserted, int updated, int deactivated, List<String> errors, String logFileUrl) {
        this.totalRows = totalRows;
        this.inserted = inserted;
        this.updated = updated;
        this.deactivated = deactivated;
        this.errors = errors;
        this.logFileUrl = logFileUrl;
    }


    @Override
    public String toString() {
        return "InventoryImportResultDTO{" +
                "totalRows=" + totalRows +
                ", inserted=" + inserted +
                ", updated=" + updated +
                ", deactivated=" + deactivated +
                ", errors=" + errors +
                ", logFileUrl='" + logFileUrl + '\'' +
                '}';
    }
}
