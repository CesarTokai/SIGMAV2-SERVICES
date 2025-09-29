package tokai.com.mx.SIGMAV2.modules.inventory.domain.service;



import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.ProductCatalogUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;

public class ProductCatalogService implements ProductCatalogUseCase {

    private final ProductRepository productRepository;

    public ProductCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> listProducts() {
        return productRepository.findAll();
    }



    @Override
    public List<Product> searchProducts(String descr) {
        return productRepository.searchByDescription(descr);
    }
}

