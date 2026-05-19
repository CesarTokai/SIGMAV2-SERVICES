package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.WarehouseInfoPort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WarehouseInfoAdapter implements WarehouseInfoPort {

    private final JpaWarehouseRepository jpaWarehouseRepository;

    @Override
    public Optional<WarehouseInfo> findById(Long id) {
        return jpaWarehouseRepository.findById(id)
                .map(e -> new WarehouseInfo(e.getIdWarehouse(), e.getWarehouseKey(), e.getNameWarehouse()));
    }

    @Override
    public List<WarehouseInfo> findAllById(Collection<Long> ids) {
        return jpaWarehouseRepository.findAllById(ids).stream()
                .map(e -> new WarehouseInfo(e.getIdWarehouse(), e.getWarehouseKey(), e.getNameWarehouse()))
                .collect(Collectors.toList());
    }
}
