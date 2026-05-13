package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.adapter;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.ProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper.ProductMapper;
import java.util.List;
import java.util.Optional;

@Component
public class ProductRepositoryAdapter implements ProductRepository {
    private final JpaProductRepository jpaRepository;
    private final ProductMapper mapper;

    public ProductRepositoryAdapter(JpaProductRepository jpaRepository, ProductMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Product save(Product product) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(product)));
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<Product> findByCveArt(String cveArt) {
        return jpaRepository.findByCveArt(cveArt).map(mapper::toDomain);
    }

    @Override
    public List<Product> searchByDescription(String description) {
        return jpaRepository.searchByDescription(description).stream().map(mapper::toDomain).toList();
    }
}
