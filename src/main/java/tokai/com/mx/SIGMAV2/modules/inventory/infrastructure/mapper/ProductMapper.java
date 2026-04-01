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
        // ✅ FIX: Convertir Enum de entity a Enum de dominio
        if (entity.getStatus() != null) {
            product.setStatus(Product.Status.valueOf(entity.getStatus().name()));
        } else {
            product.setStatus(Product.Status.A); // Default a "A" si es null
        }
        product.setCreatedAt(entity.getCreatedAt());
        return product;
    }
    
    public ProductEntity toEntity(Product product) {
        if (product == null) return null;
        ProductEntity entity = new ProductEntity();
        entity.setIdProduct(product.getId());
        entity.setCveArt(product.getCveArt());
        entity.setDescr(product.getDescr());
        entity.setLinProd(product.getLinProd());
        String uniMed = product.getUniMed();
        entity.setUniMed((uniMed == null || uniMed.trim().isEmpty()) ? "pz" : uniMed);
        // ✅ FIX: Asignar el Enum directamente (no .name())
        // La entidad ahora es @Enumerated(EnumType.STRING)
        if (product.getStatus() != null) {
            entity.setStatus(ProductEntity.Status.valueOf(product.getStatus().name()));
        } else {
            entity.setStatus(ProductEntity.Status.A); // Default a "A"
        }
        entity.setCreatedAt(product.getCreatedAt());
        return entity;
    }
}
