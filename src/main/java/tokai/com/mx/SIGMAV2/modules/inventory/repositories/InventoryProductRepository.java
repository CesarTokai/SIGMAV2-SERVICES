package tokai.com.mx.SIGMAV2.modules.inventory.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.Product;

import java.util.Optional;

@Repository
public interface InventoryProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByCveArt(String cveArt);
    
    boolean existsByCveArt(String cveArt);
    
    @Query("SELECT p FROM Product p WHERE " +
           "(:q IS NULL OR UPPER(p.cveArt) LIKE UPPER(CONCAT('%', :q, '%')) OR " +
           "UPPER(p.description) LIKE UPPER(CONCAT('%', :q, '%')))")
    Page<Product> findWithFilters(@Param("q") String query, Pageable pageable);
}