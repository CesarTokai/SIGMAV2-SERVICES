package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.LabelQueryPort;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;

/**
 * Adapter que implementa LabelQueryPort consultando directamente las tablas de labels.
 * Vive en infrastructure del módulo inventory para no contaminar el dominio.
 */
@Component
@RequiredArgsConstructor
public class LabelQueryAdapter implements LabelQueryPort {

    private final JpaLabelRepository labelRepository;
    private final JpaLabelCountEventRepository countEventRepository;

    @Override
    public boolean hasActiveLabelsForProduct(Long productId, Long periodId) {
        var labels = labelRepository.findByPeriodId(periodId);
        return labels.stream()
                .anyMatch(l -> l.getProductId().equals(productId)
                        && l.getEstado() != Label.State.CANCELADO);
    }

    @Override
    public boolean hasCountEventsForPeriod(Long periodId) {
        // Consultar si hay labels con conteos en este periodo
        var labels = labelRepository.findByPeriodId(periodId);
        if (labels.isEmpty()) return false;
        var folios = labels.stream().map(Label::getFolio).toList();
        // Verificar si algún folio tiene conteos registrados
        var events = countEventRepository.findByFolioInOrderByFolioAscCountNumberAsc(folios);
        return !events.isEmpty();
    }

    @Override
    public long countActiveLabelsForPeriod(Long periodId) {
        var labels = labelRepository.findNonCancelledByPeriod(periodId);
        return labels.size();
    }
}


