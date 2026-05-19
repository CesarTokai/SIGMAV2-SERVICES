package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CancelLabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCancelledStockDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.exception.LabelAlreadyCancelledException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCancelledRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Servicio especializado en cancelación y reactivación de marbetes.
 *
 * Reglas de dominio:
 * - Un marbete ya CANCELADO no puede cancelarse de nuevo.
 * - Si requestedLabels == 0 en LabelRequest, el marbete no puede cancelarse.
 * - updateCancelledStock: si existenciasActuales > 0 y no estaba reactivado, reactiva el marbete (estado → GENERADO).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelCancelService {

    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    private final JpaLabelRequestRepository labelRequestRepository;
    private final JpaLabelRepository jpaLabelRepository;
    private final JpaLabelCancelledRepository jpaLabelCancelledRepository;
    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;

    @Transactional(readOnly = true)
    public List<LabelCancelledDTO> getCancelledLabels(Long periodId, Long warehouseId, Long userId, String userRole) {
        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        return persistence.findCancelledByPeriodAndWarehouse(periodId, warehouseId, false).stream()
                .map(cancelled -> {
                    ProductEntity product = productRepository.findById(cancelled.getProductId()).orElse(null);
                    if (product == null) return null;
                    return LabelCancelledDTO.builder()
                            .idLabelCancelled(cancelled.getIdLabelCancelled()).folio(cancelled.getFolio())
                            .productId(cancelled.getProductId())
                            .claveProducto(product.getCveArt()).nombreProducto(product.getDescr())
                            .warehouseId(cancelled.getWarehouseId())
                            .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                            .periodId(cancelled.getPeriodId())
                            .existenciasAlCancelar(cancelled.getExistenciasAlCancelar())
                            .existenciasActuales(cancelled.getExistenciasActuales())
                            .motivoCancelacion(cancelled.getMotivoCancelacion())
                            .canceladoAt(cancelled.getCanceladoAt() != null ? cancelled.getCanceladoAt().toString() : null)
                            .reactivado(cancelled.getReactivado())
                            .reactivadoAt(cancelled.getReactivadoAt() != null ? cancelled.getReactivadoAt().toString() : null)
                            .notas(cancelled.getNotas()).build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional
    public LabelCancelledDTO updateCancelledStock(UpdateCancelledStockDTO dto, Long userId, String userRole) {
        LabelCancelled cancelled = persistence.findCancelledByFolioAndPeriodId(dto.getFolio(), dto.getPeriodId())
                .orElseThrow(() -> new LabelNotFoundException("Marbete cancelado no encontrado: folio " + dto.getFolio() + " periodo " + dto.getPeriodId()));

        warehouseAccessService.validateWarehouseAccess(userId, cancelled.getWarehouseId(), userRole);
        cancelled.setExistenciasActuales(dto.getExistenciasActuales());
        if (dto.getNotas() != null) cancelled.setNotas(dto.getNotas());

        // Reactivar si ahora tiene existencias
        if (dto.getExistenciasActuales() > 0 && !cancelled.getReactivado()) {
            cancelled.setReactivado(true);
            cancelled.setReactivadoAt(LocalDateTime.now());
            cancelled.setReactivadoBy(userId);
            Label label = new Label();
            label.setFolio(cancelled.getFolio());
            label.setLabelRequestId(cancelled.getLabelRequestId());
            label.setPeriodId(cancelled.getPeriodId());
            label.setWarehouseId(cancelled.getWarehouseId());
            label.setProductId(cancelled.getProductId());
            label.setEstado(Label.State.GENERADO);
            label.setCreatedBy(userId);
            label.setCreatedAt(LocalDateTime.now());
            persistence.save(label);
            log.info("Marbete folio {} reactivado", dto.getFolio());
        }
        persistence.saveCancelled(cancelled);

        ProductEntity product = productRepository.findById(cancelled.getProductId()).orElse(null);
        WarehouseEntity warehouse = warehouseRepository.findById(cancelled.getWarehouseId()).orElse(null);

        return LabelCancelledDTO.builder()
                .idLabelCancelled(cancelled.getIdLabelCancelled()).folio(cancelled.getFolio())
                .productId(cancelled.getProductId())
                .claveProducto(product != null ? product.getCveArt() : null)
                .nombreProducto(product != null ? product.getDescr() : null)
                .warehouseId(cancelled.getWarehouseId())
                .claveAlmacen(warehouse != null ? warehouse.getWarehouseKey() : null)
                .nombreAlmacen(warehouse != null ? warehouse.getNameWarehouse() : null)
                .periodId(cancelled.getPeriodId())
                .existenciasAlCancelar(cancelled.getExistenciasAlCancelar())
                .existenciasActuales(cancelled.getExistenciasActuales())
                .motivoCancelacion(cancelled.getMotivoCancelacion())
                .canceladoAt(cancelled.getCanceladoAt() != null ? cancelled.getCanceladoAt().toString() : null)
                .reactivado(cancelled.getReactivado())
                .reactivadoAt(cancelled.getReactivadoAt() != null ? cancelled.getReactivadoAt().toString() : null)
                .notas(cancelled.getNotas()).build();
    }

    @Transactional
    public void cancelLabel(CancelLabelRequestDTO dto, Long userId, String userRole) {
        log.info("Cancelando marbete folio {} periodo {} por usuario {} con rol {}", dto.getFolio(), dto.getPeriodId(), userId, userRole);
        if (dto.getFolio() == null) {
            throw new InvalidLabelStateException("El campo 'folio' es obligatorio");
        }
        Label label = jpaLabelRepository.findByFolioAndPeriodId(dto.getFolio(), dto.getPeriodId())
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + dto.getFolio() + " no encontrado en periodo " + dto.getPeriodId()));

        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
        if (label.getEstado() == Label.State.CANCELADO) throw new LabelAlreadyCancelledException(dto.getFolio());

        if (label.getLabelRequestId() != null) {
            LabelRequest labelRequest = labelRequestRepository.findById(label.getLabelRequestId()).orElse(null);
            if (labelRequest != null && (labelRequest.getRequestedLabels() == null || labelRequest.getRequestedLabels() == 0)) {
                throw new InvalidLabelStateException("No se puede cancelar: el marbete tiene 0 folios solicitados.");
            }
        }

        // Capturar conteos al momento de cancelar
        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(dto.getFolio());
        java.math.BigDecimal c1 = null, c2 = null;
        for (LabelCountEvent evt : events) {
            if (evt.getCountNumber() == 1) c1 = evt.getCountedValue();
            if (evt.getCountNumber() == 2) c2 = evt.getCountedValue();
        }

        label.setEstado(Label.State.CANCELADO);
        jpaLabelRepository.save(label);

        LabelCancelled cancelled = new LabelCancelled();
        cancelled.setFolio(dto.getFolio());
        cancelled.setLabelRequestId(label.getLabelRequestId());
        cancelled.setPeriodId(label.getPeriodId());
        cancelled.setWarehouseId(label.getWarehouseId());
        cancelled.setProductId(label.getProductId());
        cancelled.setMotivoCancelacion(dto.getMotivoCancelacion() != null ? dto.getMotivoCancelacion() : "Cancelado manualmente");
        cancelled.setCanceladoAt(LocalDateTime.now());
        cancelled.setCanceladoBy(userId);
        cancelled.setReactivado(false);
        cancelled.setConteo1AlCancelar(c1);
        cancelled.setConteo2AlCancelar(c2);
        try {
            inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId())
                    .ifPresent(stock -> {
                        int ex = stock.getExistQty() != null ? stock.getExistQty().intValue() : 0;
                        cancelled.setExistenciasAlCancelar(ex);
                        cancelled.setExistenciasActuales(ex);
                    });
        } catch (Exception e) {
            cancelled.setExistenciasAlCancelar(0);
            cancelled.setExistenciasActuales(0);
        }
        jpaLabelCancelledRepository.save(cancelled);
        log.info("Marbete {} cancelado exitosamente", dto.getFolio());
    }
}
