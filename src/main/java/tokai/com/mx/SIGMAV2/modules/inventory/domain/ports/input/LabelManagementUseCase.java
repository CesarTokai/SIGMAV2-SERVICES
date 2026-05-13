package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Label;

import java.util.List;

public interface LabelManagementUseCase {
    Label createLabel(Label label);
    List<Label> listLabelsByPeriod(Long periodId);
}