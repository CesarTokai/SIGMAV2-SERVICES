package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.PrintRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelGenerationBatch;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;

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
            throw new RuntimeException("Existen marbetes GENERADOS sin imprimir para este producto/almacén/periodo.");
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
            throw new RuntimeException("No existe una solicitud para el producto/almacén/periodo.");
        }
        LabelRequest req = opt.get();
        int remaining = req.getRequestedLabels() - req.getFoliosGenerados();
        if (remaining <= 0) {
            throw new RuntimeException("No hay folios solicitados para generar.");
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

    @Override
    @Transactional
    public LabelPrint printLabels(PrintRequestDTO dto, Long userId) {
        // Aquí podrían ir validaciones RBAC (permiso sobre almacén) y verificación de catálogos cargados
        return persistence.printLabelsRange(dto.getPeriodId(), dto.getWarehouseId(), dto.getStartFolio(), dto.getEndFolio(), userId);
    }

    @Override
    @Transactional
    public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
        // Roles permitidos para C1: ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO
        if (userRole == null) {
            throw new RuntimeException("Role de usuario requerido para registrar C1");
        }
        String roleUpper = userRole.toUpperCase();
        boolean allowed = roleUpper.equals("ADMINISTRADOR") || roleUpper.equals("ALMACENISTA") || roleUpper.equals("AUXILIAR") || roleUpper.equals("AUXILIAR_DE_CONTEO");
        if (!allowed) {
            throw new RuntimeException("No tiene permiso para registrar C1");
        }

        // Verificar que el marbete exista y esté IMPRESO
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new RuntimeException("El folio no existe");
        }
        Label label = optLabel.get();
        if (label.getEstado() == Label.State.CANCELADO) {
            throw new RuntimeException("No se puede registrar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new RuntimeException("No se puede registrar conteo: el marbete no está IMPRESO.");
        }

        // Registrar evento de conteo número 1
        LabelCountEvent.Role roleEnum;
        try { roleEnum = LabelCountEvent.Role.valueOf(roleUpper); } catch (Exception ex) { roleEnum = LabelCountEvent.Role.AUXILIAR; }

        return persistence.saveCountEvent(dto.getFolio(), userId, 1, dto.getCountedValue(), roleEnum, false);
    }

    @Override
    @Transactional
    public LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole) {
        // Roles permitidos para C2: AUXILIAR_DE_CONTEO (según configuración)
        if (userRole == null) {
            throw new RuntimeException("Role de usuario requerido para registrar C2");
        }
        String roleUpper = userRole.toUpperCase();
        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            throw new RuntimeException("No tiene permiso para registrar C2");
        }

        // Verificar que el marbete exista y esté IMPRESO
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new RuntimeException("El folio no existe");
        }
        Label label = optLabel.get();
        if (label.getEstado() == Label.State.CANCELADO) {
            throw new RuntimeException("No se puede registrar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new RuntimeException("No se puede registrar conteo: el marbete no está IMPRESO.");
        }

        LabelCountEvent.Role roleEnum;
        try { roleEnum = LabelCountEvent.Role.valueOf(roleUpper); } catch (Exception ex) { roleEnum = LabelCountEvent.Role.AUXILIAR_DE_CONTEO; }

        // Registrar evento de conteo número 2 y marcar is_final = true
        return persistence.saveCountEvent(dto.getFolio(), userId, 2, dto.getCountedValue(), roleEnum, true);
    }
}
