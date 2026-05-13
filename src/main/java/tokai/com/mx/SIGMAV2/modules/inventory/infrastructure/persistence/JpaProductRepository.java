package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaProductRepository extends JpaRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByCveArt(String cveArt);

    @Query("SELECT p FROM ProductEntity p WHERE LOWER(p.descr) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<ProductEntity> searchByDescription(@Param("description") String description);

    /** Carga varios productos en una sola query — evita N+1 en reportes. */
    @Query("SELECT p FROM ProductEntity p WHERE p.idProduct IN :ids")
    List<ProductEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);
}


