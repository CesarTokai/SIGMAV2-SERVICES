package tokai.com.mx.SIGMAV2.modules.labels.domain.port.output;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductInfoPort {

    record ProductInfo(Long id, String cveArt, String descr, String uniMed) {}

    Optional<ProductInfo> findById(Long id);

    List<ProductInfo> findAllById(Collection<Long> ids);
}
