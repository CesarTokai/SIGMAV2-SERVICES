package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;


import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.LabelManagementUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.LabelRepository;

import java.time.LocalDateTime;
import java.util.List;

public class LabelManagementService implements LabelManagementUseCase {

    private final LabelRepository labelRepository;

    public LabelManagementService(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @Override
    public Label createLabel(Label label) {
        // Validar que no exista un marbete igual para el mismo producto, almacén y periodo
        boolean exists = labelRepository.existsByProductWarehousePeriod(
                label.getProductId(), label.getWarehouseId(), label.getPeriodId());
        if (exists) {
            throw new IllegalArgumentException("Ya existe un marbete para este producto, almacén y periodo");
        }
        label.setCreatedAt(LocalDateTime.now());
        return labelRepository.save(label);
    }

    @Override
    public List<Label> listLabelsByPeriod(Long periodId) {
        return labelRepository.findByPeriod(periodId);
    }
}
