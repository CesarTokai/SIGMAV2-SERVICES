package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.Data;

@Data
public class MultiWarehouseWizardStepDTO {
    private int stepNumber;
    private String period;
    private String fileName;
    private boolean confirmBajas;
    // Puedes agregar más campos según el paso del wizard
}

