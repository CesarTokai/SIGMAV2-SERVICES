package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;

@Component
public class ProductMapper {
    public Product toDomain(ProductEntity entity) {
        if (entity == null) return null;
        Product product = new Product();
        product.setId(entity.getIdProduct());
        product.setCveArt(entity.getCveArt());
        product.setDescr(entity.getDescr());
        product.setLinProd(entity.getLinProd());
        String uniMed = entity.getUniMed();
        product.setUniMed((uniMed == null || uniMed.trim().isEmpty()) ? "pz" : uniMed);
        product.setStatus(Product.Status.valueOf(entity.getStatus()));
        product.setCreatedAt(entity.getCreatedAt());
        return product;
    }
    public ProductEntity toEntity(Product product) {
        if (product == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setIdProduct(product.getId()); // ⭐ MAPEAR EL ID
        entity.setCveArt(product.getCveArt());
        entity.setDescr(product.getDescr());
        entity.setLinProd(product.getLinProd());
        String uniMed = product.getUniMed();
        entity.setUniMed((uniMed == null || uniMed.trim().isEmpty()) ? "pz" : uniMed);
        entity.setStatus(product.getStatus().name());
        entity.setCreatedAt(product.getCreatedAt());
        return entity;
    }
}
