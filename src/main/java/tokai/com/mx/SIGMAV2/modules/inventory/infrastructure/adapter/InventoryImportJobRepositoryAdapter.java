package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.adapter;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryImportJob;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryImportJobRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryImportJobRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventoryImportJobEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper.InventoryImportJobMapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("inventoryImportJobRepositoryAdapter")
public class InventoryImportJobRepositoryAdapter implements InventoryImportJobRepository {

    private final JpaInventoryImportJobRepository jpa;
    private final InventoryImportJobMapper mapper;

    public InventoryImportJobRepositoryAdapter(JpaInventoryImportJobRepository jpa, InventoryImportJobMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public InventoryImportJob save(InventoryImportJob job) {
        InventoryImportJobEntity e = mapper.toEntity(job);
        InventoryImportJobEntity saved = jpa.save(e);
        return mapper.toDomain(saved);
    }

    @Override
    public List<InventoryImportJob> findAll() {
        return jpa.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InventoryImportJob> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }
}
