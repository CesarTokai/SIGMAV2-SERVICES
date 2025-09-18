package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import java.util.Optional;
import java.util.List;

public interface ProductRepository {
    Optional<Product> findById(Long id);
    List<Product> findAll();
    Product save(Product product);
    void deleteById(Long id);
}