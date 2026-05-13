package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.adapter;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.WarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper.WarehouseMapper;

import java.util.List;
import java.util.Optional;

@Component
public class WarehouseRepositoryAdapter implements WarehouseRepository {
    private final JpaWarehouseRepository jpaRepository;
    private final WarehouseMapper mapper;

    public WarehouseRepositoryAdapter(JpaWarehouseRepository jpaRepository, WarehouseMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Warehouse> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(warehouse)));
    }

    @Override
    public Optional<Warehouse> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}
