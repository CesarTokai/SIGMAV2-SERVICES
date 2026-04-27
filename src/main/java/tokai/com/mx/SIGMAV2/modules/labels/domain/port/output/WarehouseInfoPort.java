package tokai.com.mx.SIGMAV2.modules.labels.domain.port.output;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WarehouseInfoPort {

    record WarehouseInfo(Long id, String warehouseKey, String nameWarehouse) {}

    Optional<WarehouseInfo> findById(Long id);

    List<WarehouseInfo> findAllById(Collection<Long> ids);
}
