package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import java.util.List;

public class InventoryImportResultDTO {
    private int totalRows;
    private int inserted;
    private int updated;
    private int deactivated;
    private List<String> errors;
    private String logFileUrl;
}