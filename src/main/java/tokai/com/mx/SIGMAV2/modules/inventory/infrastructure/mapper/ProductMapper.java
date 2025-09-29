package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;

@Component
public class ProductMapper {
    public Product toDomain(ProductEntity entity) {
        if (entity == null) return null;
        Product product = new Product();
        product.setId(entity.getId());
        product.setCveArt(entity.getCveArt());
        product.setDescr(entity.getDescr());
        product.setUniMed(entity.getUniMed());
        product.setStatus(Product.Status.valueOf(entity.getStatus()));
        product.setCreatedAt(entity.getCreatedAt());
        return product;
    }
    public ProductEntity toEntity(Product product) {
        if (product == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId());
        entity.setCveArt(product.getCveArt());
        entity.setDescr(product.getDescr());
        entity.setUniMed(product.getUniMed());
        entity.setStatus(product.getStatus().name());
        entity.setCreatedAt(product.getCreatedAt());
        return entity;
    }
}

