package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;


import java.util.List;

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

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getInserted() {
        return inserted;
    }

    public void setInserted(int inserted) {
        this.inserted = inserted;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getDeactivated() {
        return deactivated;
    }

    public void setDeactivated(int deactivated) {
        this.deactivated = deactivated;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getLogFileUrl() {
        return logFileUrl;
    }

    public void setLogFileUrl(String logFileUrl) {
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
