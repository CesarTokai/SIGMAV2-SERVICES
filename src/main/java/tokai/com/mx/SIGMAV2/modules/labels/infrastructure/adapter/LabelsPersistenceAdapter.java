package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelFolioSequence;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelGenerationBatch;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.LabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.LabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelFolioSequenceRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelGenerationBatchRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelPrintRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCancelledRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LabelsPersistenceAdapter implements LabelRepository, LabelRequestRepository {

    private final JpaLabelRepository jpaLabelRepository;
    private final JpaLabelRequestRepository jpaLabelRequestRepository;
    private final JpaLabelFolioSequenceRepository jpaLabelFolioSequenceRepository;
    private final JpaLabelGenerationBatchRepository jpaLabelGenerationBatchRepository;
    private final JpaLabelPrintRepository jpaLabelPrintRepository;
    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;
    private final JpaPeriodRepository jpaPeriodRepository;
    private final JpaLabelCancelledRepository jpaLabelCancelledRepository;

    @Override
    public Label save(Label label) {
        return jpaLabelRepository.save(label);
    }

    public List<Label> saveAll(List<Label> labels) {
        return jpaLabelRepository.saveAll(labels);
    }

    @Override
    public Optional<Label> findByFolio(Long folio) {
        return jpaLabelRepository.findById(folio);
    }

    @Override
    public List<Label> findByPeriodIdAndWarehouseId(Long periodId, Long warehouseId, int offset, int limit) {
        if (limit <= 0) return List.of();
        int page = offset / limit;
        var pageReq = PageRequest.of(page, limit);
        return jpaLabelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId, pageReq).getContent();
    }

    @Override
    public long countByPeriodIdAndWarehouseId(Long periodId, Long warehouseId) {
        return jpaLabelRepository.countByPeriodIdAndWarehouseId(periodId, warehouseId);
    }

    @Override
    public List<Label> findGeneratedByRequestIdRange(Long requestId, Long startFolio, Long endFolio) {
        var pageReq = PageRequest.of(0, (int)(endFolio - startFolio + 1));
        return jpaLabelRepository.findByLabelRequestIdAndFolioBetween(requestId, startFolio, endFolio, pageReq).getContent();
    }

    // LabelRequestRepository methods
    @Override
    public LabelRequest save(LabelRequest request) {
        return jpaLabelRequestRepository.save(request);
    }

    @Override
    public Optional<LabelRequest> findByProductWarehousePeriod(Long productId, Long warehouseId, Long periodId) {
        return jpaLabelRequestRepository.findByProductIdAndWarehouseIdAndPeriodId(productId, warehouseId, periodId);
    }

    @Override
    public void delete(LabelRequest request) {
        jpaLabelRequestRepository.delete(request);
    }

    @Override
    public boolean existsGeneratedUnprintedForProductWarehousePeriod(Long productId, Long warehouseId, Long periodId) {
        return jpaLabelRepository.existsByProductIdAndWarehouseIdAndPeriodIdAndEstado(productId, warehouseId, periodId, Label.State.GENERADO);
    }

    // Operaciones de secuencia y generación (helpers usados por el servicio)
    @Transactional
    public synchronized long[] allocateFolioRange(Long periodId, int quantity) {
        // Bloqueo PESSIMISTIC_WRITE se aplica en el repositorio JPA via findById con @Lock
        Optional<LabelFolioSequence> opt = jpaLabelFolioSequenceRepository.findById(periodId);
        LabelFolioSequence seq;
        if (opt.isPresent()) {
            seq = opt.get();
        } else {
            seq = new LabelFolioSequence();
            seq.setPeriodId(periodId);
            seq.setUltimoFolio(0L);
        }
        long primer = seq.getUltimoFolio() + 1;
        long ultimo = seq.getUltimoFolio() + quantity;
        seq.setUltimoFolio(ultimo);
        jpaLabelFolioSequenceRepository.save(seq);
        return new long[]{primer, ultimo};
    }

    @Transactional
    public LabelGenerationBatch saveGenerationBatch(LabelGenerationBatch batch) {
        return jpaLabelGenerationBatchRepository.save(batch);
    }

    @Transactional
    public void saveLabelsBatch(Long requestId, Long periodId, Long warehouseId, Long productId, long primer, long ultimo, Long createdBy) {
        log.info("=== saveLabelsBatch INICIO ===");
        log.info("Parámetros: requestId={}, periodId={}, warehouseId={}, productId={}, primer={}, ultimo={}, createdBy={}",
            requestId, periodId, warehouseId, productId, primer, ultimo, createdBy);

        List<Label> labels = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (long f = primer; f <= ultimo; f++) {
            Label l = new Label();
            l.setFolio(f);
            l.setLabelRequestId(requestId);
            l.setPeriodId(periodId);
            l.setWarehouseId(warehouseId);
            l.setProductId(productId);
            l.setEstado(Label.State.GENERADO);
            l.setCreatedBy(createdBy);
            l.setCreatedAt(now);
            labels.add(l);
        }
        log.info("Creados {} objetos Label en memoria, procediendo a guardar en BD...", labels.size());
        jpaLabelRepository.saveAll(labels);
        log.info("Guardados {} marbetes en la base de datos exitosamente", labels.size());

        // Verificar que se guardaron
        long count = jpaLabelRepository.countByPeriodIdAndWarehouseId(periodId, warehouseId);
        log.info("Verificación: Total de marbetes en BD para periodId={}, warehouseId={}: {}", periodId, warehouseId, count);
        log.info("=== saveLabelsBatch FIN ===");
    }

    @Transactional
    public void saveLabelsBatchAsCancelled(Long requestId, Long periodId, Long warehouseId, Long productId, long primer, long ultimo, Long createdBy, Integer existencias) {
        log.info("=== saveLabelsBatchAsCancelled INICIO ===");
        log.info("Parámetros: requestId={}, periodId={}, warehouseId={}, productId={}, primer={}, ultimo={}, createdBy={}, existencias={}",
            requestId, periodId, warehouseId, productId, primer, ultimo, createdBy, existencias);

        List<tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled> cancelledLabels = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (long f = primer; f <= ultimo; f++) {
            tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled lc = new tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled();
            lc.setFolio(f);
            lc.setLabelRequestId(requestId);
            lc.setPeriodId(periodId);
            lc.setWarehouseId(warehouseId);
            lc.setProductId(productId);
            lc.setExistenciasAlCancelar(existencias != null ? existencias : 0);
            lc.setExistenciasActuales(existencias != null ? existencias : 0);
            lc.setMotivoCancelacion("Sin existencias al momento de generación");
            lc.setCanceladoAt(now);
            lc.setCanceladoBy(createdBy);
            lc.setReactivado(false);
            cancelledLabels.add(lc);
        }

        log.info("Creados {} objetos LabelCancelled en memoria, procediendo a guardar en BD...", cancelledLabels.size());
        jpaLabelCancelledRepository.saveAll(cancelledLabels);
        log.info("Guardados {} marbetes cancelados en la base de datos exitosamente", cancelledLabels.size());

        // Verificar que se guardaron
        long count = jpaLabelCancelledRepository.countByPeriodIdAndWarehouseIdAndReactivado(periodId, warehouseId, false);
        log.info("Verificación: Total de marbetes cancelados en BD para periodId={}, warehouseId={}: {}", periodId, warehouseId, count);
        log.info("=== saveLabelsBatchAsCancelled FIN ===");
    }

    // Nueva operación: impresión de rango de marbetes
    @Transactional
    public synchronized LabelPrint printLabelsRange(Long periodId, Long warehouseId, Long startFolio, Long endFolio, Long userId) {
        log.info("Iniciando printLabelsRange: periodo={}, almacén={}, folios {}-{}",
            periodId, warehouseId, startFolio, endFolio);

        // Validación básica de rango
        if (endFolio < startFolio) {
            throw new IllegalArgumentException("Rango inválido: endFolio < startFolio");
        }
        long count = endFolio - startFolio + 1;
        if (count > 500) {
            throw new IllegalArgumentException("Máximo 500 folios por lote.");
        }

        // Buscar marbetes del rango en el periodo y almacén específicos
        // MEJORA: Filtrar por periodo y almacén en la query para mayor eficiencia
        List<Label> labels = jpaLabelRepository.findByFolioBetween(startFolio, endFolio);

        // Filtrar solo los que pertenecen al periodo y almacén solicitados
        List<Label> filteredLabels = labels.stream()
            .filter(l -> l.getPeriodId().equals(periodId) && l.getWarehouseId().equals(warehouseId))
            .collect(Collectors.toList());

        // Verificar que todos los folios existan
        if (filteredLabels.size() != count) {
            // Encontrar faltantes
            java.util.Set<Long> found = filteredLabels.stream().map(Label::getFolio).collect(Collectors.toSet());
            StringBuilder sb = new StringBuilder();
            for (long f = startFolio; f <= endFolio; f++) {
                if (!found.contains(f)) {
                    if (!sb.isEmpty()) sb.append(',');
                    sb.append(f);
                }
            }
            String missing = sb.toString();
            throw new IllegalStateException(
                String.format("No es posible imprimir marbetes no generados. Folios faltantes: %s", missing));
        }

        // CORRECCIÓN ERROR #5: Validar TODOS los marbetes ANTES de modificar cualquiera
        // Esto evita inconsistencias si se encuentra un error a mitad del proceso
        List<String> errores = validateLabelsForPrinting(filteredLabels, periodId, warehouseId);

        // Si hay errores, lanzar excepción SIN HABER MODIFICADO NADA
        if (!errores.isEmpty()) {
            String mensajeError = String.join("; ", errores);
            log.error("Errores de validación en printLabelsRange: {}", mensajeError);
            throw new IllegalStateException(
                String.format("No es posible imprimir los marbetes. Errores encontrados: %s", mensajeError));
        }

        // Si llegamos aquí, TODOS los marbetes son válidos
        // Ahora sí, modificar todos de forma segura
        LocalDateTime now = LocalDateTime.now();
        log.debug("Actualizando estado de {} marbetes a IMPRESO", filteredLabels.size());

        for (Label l : filteredLabels) {
            l.setEstado(Label.State.IMPRESO);
            l.setImpresoAt(now);
        }

        // Guardar todos los labels actualizados
        jpaLabelRepository.saveAll(filteredLabels);
        log.info("Estados actualizados exitosamente para {} marbetes", filteredLabels.size());

        // Crear registro en label_prints para auditoría
        LabelPrint lp = new LabelPrint();
        lp.setPeriodId(periodId);
        lp.setWarehouseId(warehouseId);
        lp.setFolioInicial(startFolio);
        lp.setFolioFinal(endFolio);
        lp.setCantidadImpresa((int)count);
        lp.setPrintedBy(userId);
        lp.setPrintedAt(now);

        return jpaLabelPrintRepository.save(lp);
    }

    /**
     * Obtiene los marbetes de un rango específico de folios para un periodo y almacén
     */
    public List<Label> findByFolioRange(Long periodId, Long warehouseId, Long startFolio, Long endFolio) {
        List<Label> allLabels = jpaLabelRepository.findByFolioBetween(startFolio, endFolio);

        // Filtrar solo los que pertenecen al periodo y almacén especificados
        return allLabels.stream()
            .filter(l -> l.getPeriodId().equals(periodId) && l.getWarehouseId().equals(warehouseId))
            .collect(Collectors.toList());
    }

    @Transactional
    public LabelCountEvent saveCountEvent(Long folio, Long userId, Integer countNumber, java.math.BigDecimal countedValue, LabelCountEvent.Role roleAtTime, Boolean isFinal) {
        LabelCountEvent ev = new LabelCountEvent();
        ev.setFolio(folio);
        ev.setUserId(userId);
        ev.setCountNumber(countNumber);
        ev.setCountedValue(countedValue);
        ev.setRoleAtTime(roleAtTime);
        ev.setIsFinal(isFinal != null ? isFinal : false);
        ev.setCreatedAt(LocalDateTime.now());
        return jpaLabelCountEventRepository.save(ev);
    }

    // Helpers para conteos
    public boolean hasCountNumber(Long folio, Integer countNumber) {
        return jpaLabelCountEventRepository.existsByFolioAndCountNumber(folio, countNumber);
    }

    public long countEventsForFolio(Long folio) {
        return jpaLabelCountEventRepository.countByFolio(folio);
    }

    public java.util.Optional<LabelCountEvent> findLatestCountEvent(Long folio) {
        return jpaLabelCountEventRepository.findTopByFolioOrderByCreatedAtDesc(folio);
    }

    public java.util.List<LabelCountEvent> findAllCountEvents(Long folio) {
        return jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
    }

    // Método para obtener el último periodo creado (ordenado por fecha descendente)
    public Optional<Long> findLastCreatedPeriodId() {
        return jpaPeriodRepository.findLatestPeriod()
                .map(periodEntity -> periodEntity.getId());
    }

    public List<LabelPrint> findLabelPrintsByProductPeriodWarehouse(Long productId, Long periodId, Long warehouseId) {
        // Buscar todos los marbetes generados para este producto, periodo y almacén
        List<Label> labels = jpaLabelRepository.findByProductIdAndPeriodIdAndWarehouseId(productId, periodId, warehouseId);
        List<LabelPrint> prints = new ArrayList<>();
        for (Label label : labels) {
            prints.addAll(jpaLabelPrintRepository.findByPeriodIdAndWarehouseIdAndFolioInicialLessThanEqualAndFolioFinalGreaterThanEqual(
                periodId, warehouseId, label.getFolio(), label.getFolio()
            ));
        }
        return prints;
    }

    // Métodos para marbetes cancelados
    public List<LabelCancelled> findCancelledByPeriodAndWarehouse(Long periodId, Long warehouseId, Boolean reactivado) {
        return jpaLabelCancelledRepository.findByPeriodIdAndWarehouseIdAndReactivado(periodId, warehouseId, reactivado);
    }

    public Optional<LabelCancelled> findCancelledByFolio(Long folio) {
        return jpaLabelCancelledRepository.findByFolio(folio);
    }

    @Transactional
    public LabelCancelled saveCancelled(LabelCancelled cancelled) {
        return jpaLabelCancelledRepository.save(cancelled);
    }

    // Método para obtener marbetes de un producto específico
    public List<Label> findByProductPeriodWarehouse(Long productId, Long periodId, Long warehouseId) {
        return jpaLabelRepository.findByProductIdAndPeriodIdAndWarehouseId(productId, periodId, warehouseId);
    }

    /**
     * Busca un marbete específico por folio, periodo y almacén
     */
    public Optional<Label> findByFolioAndPeriodAndWarehouse(Long folio, Long periodId, Long warehouseId) {
        return jpaLabelRepository.findByFolioAndPeriodIdAndWarehouseId(folio, periodId, warehouseId);
    }

    /**
     * Busca múltiples marbetes por lista de folios (búsqueda batch)
     * CORRECCIÓN ERROR #2: Evita N+1 queries usando IN clause
     */
    public List<Label> findByFoliosInAndPeriodAndWarehouse(Collection<Long> folios, Long periodId, Long warehouseId) {
        if (folios == null || folios.isEmpty()) {
            return Collections.emptyList();
        }
        return jpaLabelRepository.findByFolioInAndPeriodIdAndWarehouseId(folios, periodId, warehouseId);
    }

    /**
     * Encuentra todos los marbetes pendientes de impresión (estado GENERADO)
     * para un periodo y almacén específicos
     */
    public List<Label> findPendingLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        return jpaLabelRepository.findByPeriodIdAndWarehouseIdAndEstado(
            periodId, warehouseId, Label.State.GENERADO);
    }

    /**
     * Encuentra todos los marbetes pendientes de impresión para un producto específico
     */
    public List<Label> findPendingLabelsByPeriodWarehouseAndProduct(Long periodId, Long warehouseId, Long productId) {
        List<Label> allPending = jpaLabelRepository.findByPeriodIdAndWarehouseIdAndEstado(
            periodId, warehouseId, Label.State.GENERADO);

        return allPending.stream()
            .filter(l -> l.getProductId().equals(productId))
            .collect(Collectors.toList());
    }

    /**
     * Valida una lista de marbetes antes de imprimirlos
     * @param labels Lista de marbetes a validar
     * @param periodId ID del periodo esperado
     * @param warehouseId ID del almacén esperado
     * @return Lista de mensajes de error. Vacía si no hay errores.
     */
    private List<String> validateLabelsForPrinting(List<Label> labels, Long periodId, Long warehouseId) {
        List<String> errores = new ArrayList<>();

        for (Label l : labels) {
            // Ya filtramos por periodo y almacén arriba, pero validamos por seguridad
            if (!l.getPeriodId().equals(periodId) || !l.getWarehouseId().equals(warehouseId)) {
                errores.add(String.format("Folio %d no pertenece al periodo/almacén seleccionado", l.getFolio()));
            }

            if (l.getEstado() == Label.State.CANCELADO) {
                errores.add(String.format("Folio %d está cancelado", l.getFolio()));
            }
        }

        return errores;
    }
}


