package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.ProductInfoPort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductInfoAdapter implements ProductInfoPort {

    private final JpaProductRepository jpaProductRepository;

    @Override
    public Optional<ProductInfo> findById(Long id) {
        return jpaProductRepository.findById(id)
                .map(e -> new ProductInfo(e.getIdProduct(), e.getCveArt(), e.getDescr(), e.getUniMed()));
    }

    @Override
    public List<ProductInfo> findAllById(Collection<Long> ids) {
        return jpaProductRepository.findAllById(ids).stream()
                .map(e -> new ProductInfo(e.getIdProduct(), e.getCveArt(), e.getDescr(), e.getUniMed()))
                .collect(Collectors.toList());
    }
}
