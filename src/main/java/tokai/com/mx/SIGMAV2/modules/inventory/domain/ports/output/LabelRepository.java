package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Label;

import java.util.List;

public interface LabelRepository {
    boolean existsByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId);
    Label save(Label label);
    List<Label> findByPeriod(Long periodId);
}