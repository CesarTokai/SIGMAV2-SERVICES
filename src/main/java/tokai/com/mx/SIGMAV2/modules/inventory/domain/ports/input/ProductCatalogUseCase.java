package tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input;

import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;

import java.util.List;

public interface ProductCatalogUseCase {
    List<Product> listProducts();

    List<Product> searchProducts(String description);

}
