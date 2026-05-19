package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.ProductCatalogUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCatalogService implements ProductCatalogUseCase {

    private final ProductRepository productRepository;

    @Override
    public List<Product> listProducts() {
        return productRepository.findAll();
    }


    @Override
    public List<Product> searchProducts(String descr) {
        return productRepository.searchByDescription(descr);
    }
}

