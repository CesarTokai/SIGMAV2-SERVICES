package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelGenerationBatch;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelOperationException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {

    private final LabelsPersistenceAdapter persistence;

    @Override
    @Transactional
    public void requestLabels(LabelRequestDTO dto, Long userId) {
        // Validación: no permitir solicitar si existen marbetes GENERADOS sin imprimir
        boolean exists = persistence.existsGeneratedUnprintedForProductWarehousePeriod(dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
        if (exists) {
            throw new LabelOperationException("Existen marbetes GENERADOS sin imprimir para este producto/almacén/periodo.");
        }

        // Crear y guardar la solicitud
        LabelRequest req = new LabelRequest();
        req.setProductId(dto.getProductId());
        req.setWarehouseId(dto.getWarehouseId());
        req.setPeriodId(dto.getPeriodId());
        req.setRequestedLabels(dto.getRequestedLabels());
        req.setFoliosGenerados(0);
        req.setCreatedBy(userId);
        req.setCreatedAt(LocalDateTime.now());

        persistence.save(req);
    }

    @Override
    @Transactional
    public void generateBatch(GenerateBatchDTO dto, Long userId) {
        // Buscar solicitud existente
        Optional<LabelRequest> opt = persistence.findByProductWarehousePeriod(dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
        if (opt.isEmpty()) {
            throw new LabelOperationException("No existe una solicitud para el producto/almacén/periodo.");
        }
        LabelRequest req = opt.get();
        int remaining = req.getRequestedLabels() - req.getFoliosGenerados();
        if (remaining <= 0) {
            throw new LabelOperationException("No hay folios solicitados para generar.");
        }
        int toGenerate = Math.min(remaining, dto.getLabelsToGenerate());

        // Allocación de rango de folios (transaccional)
        long[] range = persistence.allocateFolioRange(dto.getPeriodId(), toGenerate);
        long primer = range[0];
        long ultimo = range[1];

        // Guardar marbetes individuales
        persistence.saveLabelsBatch(req.getIdLabelRequest(), dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId(), primer, ultimo, userId);

        // Registrar lote
        LabelGenerationBatch batch = new LabelGenerationBatch();
        batch.setLabelRequestId(req.getIdLabelRequest());
        batch.setPeriodId(dto.getPeriodId());
        batch.setWarehouseId(dto.getWarehouseId());
        batch.setPrimerFolio(primer);
        batch.setUltimoFolio(ultimo);
        batch.setTotalGenerados(toGenerate);
        batch.setGeneradoPor(userId);
        batch.setGeneradoAt(LocalDateTime.now());

        persistence.saveGenerationBatch(batch);

        // Actualizar la solicitud
        req.setFoliosGenerados(req.getFoliosGenerados() + toGenerate);
        persistence.save(req);
    }
}

