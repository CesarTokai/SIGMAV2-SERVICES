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
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateFileResponseDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelWithCommentsReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCancelledRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.modules.users.model.BeanUser;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio especializado en la generación de reportes y el archivo TXT de existencias.
 *
 * Correcciones aplicadas:
 *  - Bug diferencias: se reportan diferencias aunque un conteo sea 0 (existencias reales = cero).
 *  - N+1 en productos y almacenes: batchLoadProducts / batchLoadWarehouses.
 *  - fuenteConteo explícito en WarehouseDetail y ProductDetail ("C2","C1","SIN_CONTEO").
 *  - Totales en ProductDetail solo suman conteos C2 (consistencia con reporte comparativo).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelReportService {

    private final WarehouseAccessService warehouseAccessService;
    private final JpaLabelRepository jpaLabelRepository;
    private final JpaLabelCancelledRepository jpaLabelCancelledRepository;
    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    private final JpaPeriodRepository jpaPeriodRepository;
    private final JpaUserRepository userRepository;

    // ═══════════════════════════════════════════════════════════════════
    // REPORTES
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<DistributionReportDTO> getDistributionReport(ReportFilterDTO filter, Long userId, String userRole) {

        // AUXILIAR_DE_CONTEO no tiene restricción de almacén
        if (filter.getWarehouseId() != null && !isAuxiliarDeConteo(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findAllLabelsByPeriodAndWarehouseForDistribution(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findAllLabelsByPeriodForDistribution(filter.getPeriodId());

        // Cargar usuarios referenciados en batch
        Set<Long> userIds = labels.stream().map(Label::getCreatedBy).collect(Collectors.toSet());
        Map<Long, String> userEmailMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getEmail()));

        Map<Long, WarehouseEntity> whMap = batchLoadWarehouses(labels);

        Map<String, List<Label>> grouped = labels.stream()
                .collect(Collectors.groupingBy(l -> l.getWarehouseId() + "_" + l.getCreatedBy()));

        List<DistributionReportDTO> result = new ArrayList<>();
        for (List<Label> group : grouped.values()) {
            if (group.isEmpty()) continue;
            Label first = group.get(0);
            WarehouseEntity wh = whMap.get(first.getWarehouseId());
            String userName = userEmailMap.getOrDefault(first.getCreatedBy(), "Usuario " + first.getCreatedBy());
            Long minF = group.stream().map(Label::getFolio).min(Long::compareTo).orElse(0L);
            Long maxF = group.stream().map(Label::getFolio).max(Long::compareTo).orElse(0L);
            result.add(new DistributionReportDTO(
                    userName,
                    wh != null ? wh.getWarehouseKey() : String.valueOf(first.getWarehouseId()),
                    wh != null ? wh.getNameWarehouse() : "Almacén " + first.getWarehouseId(),
                    minF, maxF, group.size()));
        }
        result.sort(Comparator.comparing(DistributionReportDTO::getClaveAlmacen));
        return result;
    }

    @Transactional(readOnly = true)
    public List<LabelListReportDTO> getLabelListReport(ReportFilterDTO filter, Long userId, String userRole) {
        // AUXILIAR_DE_CONTEO no tiene restricción de almacén
        if (filter.getWarehouseId() != null && !isAuxiliarDeConteo(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        // Cargar período
        var period = jpaPeriodRepository.findById(filter.getPeriodId()).orElse(null);
        String periodName = period != null ? period.getDate().toString() : filter.getPeriodId().toString();

        return labels.stream().map(label -> {
            ProductEntity   p  = prodMap.get(label.getProductId());
            WarehouseEntity w  = whMap.get(label.getWarehouseId());
            BigDecimal c1 = null, c2 = null;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 1) c1 = e.getCountedValue();
                if (e.getCountNumber() == 2) c2 = e.getCountedValue();
            }
            return new LabelListReportDTO(
                    label.getFolio(),
                    p != null ? p.getCveArt()  : "",
                    p != null ? p.getDescr()  : "",
                    p != null ? p.getUniMed() : "",
                    w != null ? w.getWarehouseKey() : "",
                    w != null ? w.getNameWarehouse() : "",
                    c1, c2,
                    label.getEstado().name(),
                    label.getEstado() == Label.State.CANCELADO,
                    filter.getPeriodId(),
                    periodName);
        }).sorted(Comparator.comparing(LabelListReportDTO::getNumeroMarbete)).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingLabelsReportDTO> getPendingLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Reporte pendientes: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        
        // AUXILIAR_DE_CONTEO no tiene restricción de almacén
        if (filter.getWarehouseId() != null && !isAuxiliarDeConteo(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // Traer TODOS los marbetes (cancelados y no cancelados)
        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        List<PendingLabelsReportDTO> result = new ArrayList<>();
        for (Label label : labels) {
            BigDecimal c1 = null, c2 = null;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 1) c1 = e.getCountedValue();
                if (e.getCountNumber() == 2) c2 = e.getCountedValue();
            }

            // Incluir:
            // 1. Todos los marbetes CANCELADOS
            // 2. Marbetes NO CANCELADOS que falten conteos (C1 o C2)
            boolean estaCancelado = label.getEstado() == Label.State.CANCELADO;
            boolean faltanConteos = c1 == null || c2 == null;

            if (estaCancelado || faltanConteos) {
                ProductEntity   p = prodMap.get(label.getProductId());
                WarehouseEntity w = whMap.get(label.getWarehouseId());
                result.add(new PendingLabelsReportDTO(
                        label.getFolio(),
                        p != null ? p.getCveArt()  : "", p != null ? p.getDescr()  : "", p != null ? p.getUniMed() : "",
                        w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                        c1, c2, label.getEstado().name(), estaCancelado));
            }
        }
        result.sort(Comparator.comparing(PendingLabelsReportDTO::getNumeroMarbete));
        return result;
    }

    /**
     * Reporte de diferencias.
     * CORRECCIÓN: se incluyen marbetes donde C1 ≠ C2 independientemente de si alguno es 0.
     * Un producto con C2=0 y C1=5 TIENE diferencia y debe aparecer en el reporte.
     */
    @Transactional(readOnly = true)
    public List<DifferencesReportDTO> getDifferencesReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Reporte diferencias: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        
        // AUXILIAR_DE_CONTEO no tiene restricción de almacén
        if (filter.getWarehouseId() != null && !isAuxiliarDeConteo(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        // ── 1. Marbetes activos (no cancelados) con diferencia ──────────
        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        List<DifferencesReportDTO> result = new ArrayList<>();
        for (Label label : labels) {
            BigDecimal c1 = null, c2 = null;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 1) c1 = e.getCountedValue();
                if (e.getCountNumber() == 2) c2 = e.getCountedValue();
            }
            // ✅ CORRECCIÓN: solo se requiere que ambos conteos existan y sean distintos.
            // Si C1=5 y C2=0 → diferencia de 5, debe aparecer.
            if (c1 != null && c2 != null && c1.compareTo(c2) != 0) {
                ProductEntity   p = prodMap.get(label.getProductId());
                WarehouseEntity w = whMap.get(label.getWarehouseId());
                result.add(new DifferencesReportDTO(
                        label.getFolio(),
                        p != null ? p.getCveArt()  : "", p != null ? p.getDescr()  : "", p != null ? p.getUniMed() : "",
                        w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                        c1, c2, c1.subtract(c2).abs(), label.getEstado().name()));
            }
        }

        // ── 2. Marbetes CANCELADOS que tenían diferencia entre C1 y C2 ──
        List<LabelCancelled> cancelledLabels = filter.getWarehouseId() != null
                ? jpaLabelCancelledRepository.findByPeriodIdAndWarehouseIdAndReactivado(
                        filter.getPeriodId(), filter.getWarehouseId(), false)
                : jpaLabelCancelledRepository.findByPeriodIdAndReactivado(filter.getPeriodId(), false);

        if (!cancelledLabels.isEmpty()) {
            Set<Long> cancelledProdIds = cancelledLabels.stream()
                    .map(LabelCancelled::getProductId).collect(Collectors.toSet());
            Set<Long> cancelledWhIds = cancelledLabels.stream()
                    .map(LabelCancelled::getWarehouseId).collect(Collectors.toSet());

            Map<Long, ProductEntity> cancelledProdMap = productRepository.findAllByIdIn(cancelledProdIds).stream()
                    .collect(Collectors.toMap(ProductEntity::getIdProduct, Function.identity()));
            Map<Long, WarehouseEntity> cancelledWhMap = warehouseRepository.findAllByIdIn(cancelledWhIds).stream()
                    .collect(Collectors.toMap(WarehouseEntity::getIdWarehouse, Function.identity()));

            for (LabelCancelled lc : cancelledLabels) {
                BigDecimal c1 = lc.getConteo1AlCancelar();
                BigDecimal c2 = lc.getConteo2AlCancelar();
                // Solo incluir si ambos conteos existen y son distintos
                if (c1 != null && c2 != null && c1.compareTo(c2) != 0) {
                    ProductEntity   p = cancelledProdMap.get(lc.getProductId());
                    WarehouseEntity w = cancelledWhMap.get(lc.getWarehouseId());
                    result.add(new DifferencesReportDTO(
                            lc.getFolio(),
                            p != null ? p.getCveArt()  : "", p != null ? p.getDescr()  : "", p != null ? p.getUniMed() : "",
                            w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                            c1, c2, c1.subtract(c2).abs(), "CANCELADO"));
                }
            }
        }

        result.sort(Comparator.comparing(DifferencesReportDTO::getNumeroMarbete));
        return result;
    }

    @Transactional(readOnly = true)
    public List<CancelledLabelsReportDTO> getCancelledLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Reporte cancelados: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        
        // AUXILIAR_DE_CONTEO no tiene restricción de almacén
        if (filter.getWarehouseId() != null && !isAuxiliarDeConteo(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<LabelCancelled> cancelledLabels = filter.getWarehouseId() != null
                ? jpaLabelCancelledRepository.findByPeriodIdAndWarehouseIdAndReactivado(filter.getPeriodId(), filter.getWarehouseId(), false)
                : jpaLabelCancelledRepository.findByPeriodIdAndReactivado(filter.getPeriodId(), false);

        // Cargar productos y almacenes en batch
        Set<Long> prodIds = cancelledLabels.stream().map(LabelCancelled::getProductId).collect(Collectors.toSet());
        Set<Long> whIds   = cancelledLabels.stream().map(LabelCancelled::getWarehouseId).collect(Collectors.toSet());
        Set<Long> uIds    = cancelledLabels.stream().map(LabelCancelled::getCanceladoBy).collect(Collectors.toSet());

        Map<Long, ProductEntity>   prodMap  = productRepository.findAllByIdIn(prodIds).stream()
                .collect(Collectors.toMap(ProductEntity::getIdProduct, Function.identity()));
        Map<Long, WarehouseEntity> whMap    = warehouseRepository.findAllByIdIn(whIds).stream()
                .collect(Collectors.toMap(WarehouseEntity::getIdWarehouse, Function.identity()));
        Map<Long, String> userEmailMap = userRepository.findAllById(uIds).stream()
                                   .collect(Collectors.toMap(u -> u.getId(), u -> u.getEmail()));

        return cancelledLabels.stream().map(c -> {
            ProductEntity   p = prodMap.get(c.getProductId());
            WarehouseEntity w = whMap.get(c.getWarehouseId());
            return new CancelledLabelsReportDTO(
                    c.getFolio(),
                    p != null ? p.getCveArt()  : "", p != null ? p.getDescr()  : "", p != null ? p.getUniMed() : "",
                    w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                    c.getConteo1AlCancelar(), c.getConteo2AlCancelar(),
                    c.getMotivoCancelacion(),
                    c.getCanceladoAt() != null ? c.getCanceladoAt().toString() : "",
                    userEmailMap.getOrDefault(c.getCanceladoBy(), "Usuario " + c.getCanceladoBy()));
        }).sorted(Comparator.comparing(CancelledLabelsReportDTO::getNumeroMarbete)).toList();
    }

    /**
     * Reporte comparativo: existencias físicas (suma de C2) vs teóricas (inventory_stock).
     * CORRECCIÓN: si el marbete no tiene C2 se registra como pendiente en log
     * pero NO se omite del agrupado — el producto aparece con fisicas=0 si no hay ningún C2.
     */
    @Transactional(readOnly = true)
    public List<ComparativeReportDTO> getComparativeReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Reporte comparativo: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        
        // AUXILIAR_DE_CONTEO no tiene restricción de almacén
        if (filter.getWarehouseId() != null && !isAuxiliarDeConteo(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        // Agrupar por producto+almacén
        Map<String, List<Label>> grouped = labels.stream()
                .collect(Collectors.groupingBy(l -> l.getProductId() + "_" + l.getWarehouseId()));

        List<ComparativeReportDTO> result = new ArrayList<>();
        for (List<Label> group : grouped.values()) {
            if (group.isEmpty()) continue;
            Label first = group.get(0);
            BigDecimal fisicas    = BigDecimal.ZERO;

            for (Label label : group) {
                BigDecimal c2 = null;
                for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                    if (e.getCountNumber() == 2) { c2 = e.getCountedValue(); break; }
                }
                if (c2 != null) fisicas = fisicas.add(c2);
            }

            BigDecimal teoricas = BigDecimal.ZERO;
            try {
                var stockOpt = inventoryStockRepository
                        .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                                first.getProductId(), first.getWarehouseId(), first.getPeriodId());
                if (stockOpt.isPresent() && stockOpt.get().getExistQty() != null) {
                    teoricas = stockOpt.get().getExistQty();
                }
            } catch (Exception e) {
                log.warn("No se pudieron obtener existencias teóricas para producto={} almacén={}: {}",
                        first.getProductId(), first.getWarehouseId(), e.getMessage());
            }

            BigDecimal diferencia  = fisicas.subtract(teoricas);
            BigDecimal porcentaje  = BigDecimal.ZERO;
            if (teoricas.compareTo(BigDecimal.ZERO) != 0) {
                porcentaje = diferencia.divide(teoricas, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));


            }

            ProductEntity   p = prodMap.get(first.getProductId());
            WarehouseEntity w = whMap.get(first.getWarehouseId());
            result.add(new ComparativeReportDTO(
                    w != null ? w.getWarehouseKey()    : String.valueOf(first.getWarehouseId()),
                    w != null ? w.getNameWarehouse()   : "Almacén " + first.getWarehouseId(),
                    p != null ? p.getCveArt()          : String.valueOf(first.getProductId()),
                    p != null ? p.getDescr()           : "",
                    p != null ? p.getUniMed()          : "",
                    fisicas, teoricas, diferencia, porcentaje));
        }
        result.sort(Comparator.comparing(ComparativeReportDTO::getClaveAlmacen)
                .thenComparing(ComparativeReportDTO::getClaveProducto));
        return result;
    }

    /**
     * Reporte almacén-detalle.
     * Incluye TODOS los marbetes: generados, impresos y cancelados.
     * Los cancelados aparecen con estado "CANCELADO" y cantidad 0.
     * CORRECCIÓN: fuenteConteo explícito — "C2", "C1" o "SIN_CONTEO".
     */
    @Transactional(readOnly = true)
    public List<WarehouseDetailReportDTO> getWarehouseDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Reporte almacén-detalle: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        
        // Se traen TODOS los marbetes (incluidos cancelados) para mostrar el inventario completo
        // Si no se especifica almacén, se filtran por los almacenes accesibles al usuario
        List<Label> labels;
        if (filter.getWarehouseId() != null) {
            // AUXILIAR_DE_CONTEO no tiene restricción de almacén
            if (!isAuxiliarDeConteo(userRole)) {
                warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
            }
            labels = jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId());
        } else {
            List<Long> accessibleWarehouses = warehouseAccessService.getAccessibleWarehouses(userId, userRole);
            if (accessibleWarehouses == null) {
                // Acceso total (ADMINISTRADOR / AUXILIAR / AUXILIAR_DE_CONTEO)
                labels = jpaLabelRepository.findByPeriodId(filter.getPeriodId());
            } else {
                labels = accessibleWarehouses.isEmpty()
                        ? List.of()
                        : jpaLabelRepository.findByPeriodIdAndWarehouseIdIn(filter.getPeriodId(), accessibleWarehouses);
            }
        }

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        return labels.stream().map(label -> {
            ProductEntity   p       = prodMap.get(label.getProductId());
            WarehouseEntity w       = whMap.get(label.getWarehouseId());
            BigDecimal      cantidad = BigDecimal.ZERO;
           String          fuente;
            boolean         esCancelado = label.getEstado() == Label.State.CANCELADO;

            if (esCancelado) {
                // Marbetes cancelados: cantidad 0, fuente indica cancelación
                fuente = "CANCELADO";
            } else {
                fuente = "SIN_CONTEO";
                for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                    if (e.getCountNumber() == 2) {
                        cantidad = e.getCountedValue();
                        fuente   = "C2";
                        break;
                    } else if (e.getCountNumber() == 1) {
                        // fallback C1 — se marca explícitamente
                        cantidad = e.getCountedValue();
                        fuente   = "C1";
                    }
                }
            }
            return new WarehouseDetailReportDTO(
                    w != null ? w.getWarehouseKey()  : String.valueOf(label.getWarehouseId()),
                    w != null ? w.getNameWarehouse() : "Almacén " + label.getWarehouseId(),
                    p != null ? p.getCveArt()        : String.valueOf(label.getProductId()),
                    p != null ? p.getDescr()         : "",
                    p != null ? p.getUniMed()        : "",
                    label.getFolio(), cantidad, label.getEstado().name(),
                    esCancelado, fuente);
        }).sorted(Comparator.comparing(WarehouseDetailReportDTO::getClaveAlmacen)
                .thenComparing(WarehouseDetailReportDTO::getClaveProducto)
                .thenComparing(WarehouseDetailReportDTO::getNumeroMarbete))
          .toList();
    }

    /**
     * Reporte producto-detalle.
     * Incluye TODOS los marbetes: generados, impresos y cancelados.
     * Los cancelados aparecen con estado "CANCELADO" y cantidad 0.
     * Ordenado por: producto → almacén → número de marbete.
     */
    @Transactional(readOnly = true)
    public List<ProductDetailReportDTO> getProductDetailReport(ReportFilterDTO filter, Long userId, String userRole) {

        // Se traen TODOS los marbetes (incluidos cancelados) para mostrar inventario completo
        // Si no se especifica almacén, se filtran por los almacenes accesibles al usuario
        List<Label> labels;
        if (filter.getWarehouseId() != null) {
            // AUXILIAR_DE_CONTEO no tiene restricción de almacén
            if (!isAuxiliarDeConteo(userRole)) {
                warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
            }
            labels = jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId());
        } else {
            List<Long> accessibleWarehouses = warehouseAccessService.getAccessibleWarehouses(userId, userRole);
            if (accessibleWarehouses == null) {
                // Acceso total (ADMINISTRADOR / AUXILIAR / AUXILIAR_DE_CONTEO)
                labels = jpaLabelRepository.findByPeriodId(filter.getPeriodId());
            } else {
                labels = accessibleWarehouses.isEmpty()
                        ? List.of()
                        : jpaLabelRepository.findByPeriodIdAndWarehouseIdIn(filter.getPeriodId(), accessibleWarehouses);
            }
        }

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        // Calcular totales por producto sumando SOLO C2 de marbetes NO cancelados
        Map<Long, BigDecimal> totalsByProduct = new HashMap<>();
        for (Label label : labels) {
            if (label.getEstado() == Label.State.CANCELADO) continue;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) {
                    totalsByProduct.merge(label.getProductId(), e.getCountedValue(), BigDecimal::add);
                    break;
                }
            }
        }

        return labels.stream().map(label -> {
            ProductEntity   p           = prodMap.get(label.getProductId());
            WarehouseEntity w           = whMap.get(label.getWarehouseId());
            BigDecimal      cantidad    = BigDecimal.ZERO;
            String          fuente      = "SIN_CONTEO";
            boolean         esCancelado = label.getEstado() == Label.State.CANCELADO;

            if (esCancelado) {
                fuente = "CANCELADO";
            } else {
                for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                    if (e.getCountNumber() == 2) {
                        cantidad = e.getCountedValue(); fuente = "C2"; break;
                    } else if (e.getCountNumber() == 1) {
                        cantidad = e.getCountedValue(); fuente = "C1";
                    }
                }
            }
            return new ProductDetailReportDTO(
                    p != null ? p.getCveArt()        : String.valueOf(label.getProductId()),
                    p != null ? p.getDescr()         : "",
                    p != null ? p.getUniMed()        : "",
                    w != null ? w.getWarehouseKey()  : String.valueOf(label.getWarehouseId()),
                    w != null ? w.getNameWarehouse() : "Almacén " + label.getWarehouseId(),
                    label.getFolio(), cantidad,
                    totalsByProduct.getOrDefault(label.getProductId(), BigDecimal.ZERO),
                    fuente, label.getEstado().name(), esCancelado);
        }).sorted(Comparator.comparing(ProductDetailReportDTO::getClaveProducto)
                .thenComparing(ProductDetailReportDTO::getClaveAlmacen)
                .thenComparing(ProductDetailReportDTO::getNumeroMarbete))
          .toList();
    }

    // ═══════════════════════════════════════════════════════════════════
    // ARCHIVO TXT
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Genera archivo TXT de existencias en memoria y lo retorna como bytes.
     */
    @Transactional(readOnly = true)
    public GenerateFileResponseDTO generateInventoryFile(Long periodId, Long userId, String userRole) {
        log.info("Generando archivo TXT de existencias para periodo {}", periodId);

        var periodEntity = jpaPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Periodo no encontrado: " + periodId));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.of("es", "ES"));
        String periodName = periodEntity.getDate().format(fmt);
        periodName = periodName.substring(0, 1).toUpperCase() + periodName.substring(1).replace(" ", "");

        List<Label> labels   = jpaLabelRepository.findNonCancelledByPeriod(periodId);
        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity> prodMap = batchLoadProducts(labels);

        // Sumar existencias por producto (C2 si existe, sino C1)
        Map<Long, ProductExistencias> productoMap = new LinkedHashMap<>();
        for (Label label : labels) {
            BigDecimal cantidad = BigDecimal.ZERO;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) { cantidad = e.getCountedValue(); break; }
                else if (e.getCountNumber() == 1) { cantidad = e.getCountedValue(); }
            }
            ProductEntity prod = prodMap.get(label.getProductId());
            productoMap.computeIfAbsent(label.getProductId(), k -> new ProductExistencias(
                    prod != null ? prod.getCveArt() : String.valueOf(k),
                    prod != null ? prod.getDescr()  : "",
                    BigDecimal.ZERO))
                    .sumarExistencias(cantidad);
        }

        List<ProductExistencias> lista = new ArrayList<>(productoMap.values());
        lista.sort(Comparator.comparing(ProductExistencias::getClaveProducto));
        String fileName = "Existencias_" + periodName + ".txt";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Calcular anchos máximos dinámicamente
            int anchoClave = lista.stream()
                    .mapToInt(p -> p.getClaveProducto() != null ? p.getClaveProducto().length() : 0)
                    .max().orElse(15);
            anchoClave = Math.max(anchoClave, "CLAVE_PRODUCTO".length()) + 2;

            int anchoDesc = lista.stream()
                    .mapToInt(p -> p.getDescripcion() != null ? p.getDescripcion().length() : 0)
                    .max().orElse(30);
            anchoDesc = Math.max(anchoDesc, "DESCRIPCION".length()) + 2;


            String formatoHeader = "%-" + anchoClave + "s%-" + anchoDesc + "s%s";
            String formatoDato   = "%-" + anchoClave + "s%-" + anchoDesc + "s%s";

            writer.write(String.format(formatoHeader, "CLAVE_PRODUCTO", "DESCRIPCION", "EXISTENCIAS"));
            writer.newLine();
            for (ProductExistencias prod : lista) {
                writer.write(String.format(formatoDato,
                        prod.getClaveProducto() != null ? prod.getClaveProducto() : "",
                        prod.getDescripcion()   != null ? prod.getDescripcion()   : "",
                        prod.getExistencias().stripTrailingZeros().toPlainString()));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar archivo TXT: " + e.getMessage(), e);
        }

        byte[] fileBytes = baos.toByteArray();
        log.info("Archivo generado en memoria: {} bytes, {} productos", fileBytes.length, lista.size());

        GenerateFileResponseDTO resp = new GenerateFileResponseDTO();
        resp.setFileName(fileName);
        resp.setTotalProductos(lista.size());
        resp.setMensaje("Archivo generado exitosamente");
        resp.setFileBytes(fileBytes);
        return resp;
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ═══════════════════════════════════════════════════════════════════

    /** Carga todos los conteos en una sola query — evita N+1. */
    private Map<Long, List<LabelCountEvent>> batchLoadCounts(List<Label> labels) {
        if (labels.isEmpty()) return new HashMap<>();
        List<Long> folios = labels.stream().map(Label::getFolio).toList();
        return jpaLabelCountEventRepository
                .findByFolioInOrderByFolioAscCountNumberAsc(folios)
                .stream()
                .collect(Collectors.groupingBy(LabelCountEvent::getFolio));
    }

    /** Carga todos los productos referenciados en una sola query — evita N+1. */
    private Map<Long, ProductEntity> batchLoadProducts(List<Label> labels) {
        if (labels.isEmpty()) return new HashMap<>();
        Set<Long> ids = labels.stream().map(Label::getProductId).collect(Collectors.toSet());
        return productRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(ProductEntity::getIdProduct, Function.identity()));
    }

    /** Carga todos los almacenes referenciados en una sola query — evita N+1. */
    private Map<Long, WarehouseEntity> batchLoadWarehouses(List<Label> labels) {
        if (labels.isEmpty()) return new HashMap<>();
        Set<Long> ids = labels.stream().map(Label::getWarehouseId).collect(Collectors.toSet());
        return warehouseRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(WarehouseEntity::getIdWarehouse, Function.identity()));
    }

    /** Valida acceso al almacén si se especificó uno concreto.
     *  AUXILIAR_DE_CONTEO tiene acceso sin restricción a todos los almacenes.
     */
    private void validateAccess(Long userId, Long warehouseId, String userRole) {
        if (warehouseId != null && !isAuxiliarDeConteo(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        }
    }

    /** Verifica si el rol es AUXILIAR_DE_CONTEO (sin restricción de almacén). */
    private boolean isAuxiliarDeConteo(String userRole) {
        return userRole != null && userRole.equalsIgnoreCase("AUXILIAR_DE_CONTEO");
    }

    /**
     * NUEVA API: Reporte de Marbetes con Comentarios
     * Retorna SOLO marbetes que tienen al menos UN comentario en C1 o C2
     * - Información del producto y almacén
     * - Conteos C1 y C2 con sus comentarios
     * - Análisis de diferencias
     * 
     * Útil para:
     * - Auditar conteos que tienen observaciones
     * - Consultar marbetes con discrepancias y notas
     * - Generar reportes de validación solo con comentarios
     */
    @Transactional(readOnly = true)
    public List<LabelWithCommentsReportDTO> getLabelListWithComments(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Iniciando reporte de marbetes con comentarios para período={}, almacén={}", 
                 filter.getPeriodId(), filter.getWarehouseId());

        // Validar acceso al almacén
        validateAccess(userId, filter.getWarehouseId(), userRole);

        // Obtener marbetes filtrados
        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findAllLabelsByPeriodAndWarehouseForDistribution(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findAllLabelsByPeriodForDistribution(filter.getPeriodId());

        if (labels.isEmpty()) {
            log.warn("No se encontraron marbetes para período={}, almacén={}", 
                     filter.getPeriodId(), filter.getWarehouseId());
            return new ArrayList<>();
        }

        // Pre-cargar datos en batch
        Map<Long, ProductEntity> productMap = batchLoadProducts(labels);
        Map<Long, WarehouseEntity> warehouseMap = batchLoadWarehouses(labels);
        
        Set<Long> folios = labels.stream().map(Label::getFolio).collect(Collectors.toSet());
        List<LabelCountEvent> countEvents = jpaLabelCountEventRepository.findByFolioInOrderByFolioAscCountNumberAsc(folios);
        Map<Long, Map<Integer, LabelCountEvent>> countsByFolioAndNumber = countEvents.stream()
                .collect(Collectors.groupingBy(
                        LabelCountEvent::getFolio,
                        Collectors.toMap(LabelCountEvent::getCountNumber, Function.identity())
                ));

        // Mapear a DTOs
        List<LabelWithCommentsReportDTO> result = new ArrayList<>();
        for (Label label : labels) {
            try {
                ProductEntity product = productMap.get(label.getProductId());
                WarehouseEntity warehouse = warehouseMap.get(label.getWarehouseId());

                if (product == null || warehouse == null) {
                    log.warn("Producto o almacén no encontrado para folio {}", label.getFolio());
                    continue;
                }

                // Obtener conteos C1 y C2
                Map<Integer, LabelCountEvent> countsByNumber = countsByFolioAndNumber.getOrDefault(label.getFolio(), new HashMap<>());
                LabelCountEvent countC1 = countsByNumber.get(1);
                LabelCountEvent countC2 = countsByNumber.get(2);

                // ✅ FILTRO: SOLO incluir si tiene COMENTARIOS
                boolean tieneComentarioC1 = countC1 != null && countC1.getComment() != null && !countC1.getComment().trim().isEmpty();
                boolean tieneComentarioC2 = countC2 != null && countC2.getComment() != null && !countC2.getComment().trim().isEmpty();
                
                if (!tieneComentarioC1 && !tieneComentarioC2) {
                    log.debug("Folio {} sin comentarios, omitiendo", label.getFolio());
                    continue; // Saltar este marbete si no tiene comentarios
                }

                // Calcular diferencia
                BigDecimal diferencia = null;
                String diferenciaPorcentaje = null;
                if (countC1 != null && countC2 != null) {
                    diferencia = countC2.getCountedValue().subtract(countC1.getCountedValue());
                    if (countC1.getCountedValue().signum() > 0) {
                        diferenciaPorcentaje = diferencia.divide(countC1.getCountedValue(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                                .setScale(2, RoundingMode.HALF_UP)
                                .toPlainString() + "%";
                    }
                }

                // Construir DTO
                LabelWithCommentsReportDTO dto = LabelWithCommentsReportDTO.builder()
                        .folio(label.getFolio())
                        .estado(label.getEstado() != null ? label.getEstado().name() : "PENDIENTE")
                        .createdAt(label.getCreatedAt())
                        .impreso(label.getImpresoAt() != null)
                        .cancelado(label.getEstado() == Label.State.CANCELADO)
                        
                        // Producto
                        .claveProducto(product.getCveArt())
                        .nombreProducto(product.getDescr())
                        .unidadMedida(product.getUniMed())
                        .descripcionProducto(product.getDescr())
                        
                        // Almacén
                        .claveAlmacen(warehouse.getWarehouseKey())
                        .nombreAlmacen(warehouse.getNameWarehouse())
                        
                        // Período
                        .periodId(label.getPeriodId())
                        
                        // Existencias teóricas
                        .existenciasTeoricas(null) // No existe en Label domain model
                        
                        // Conteo C1 con comentario
                        .conteo1Valor(countC1 != null ? countC1.getCountedValue() : null)
                        .conteo1Fecha(countC1 != null ? countC1.getCreatedAt() : null)
                        .conteo1UsuarioEmail(countC1 != null && countC1.getUserId() != null ? 
                                userRepository.findById(countC1.getUserId()).map(BeanUser::getEmail).orElse("") : null)
                        .conteo1UsuarioNombre(countC1 != null && countC1.getUserId() != null ? 
                                userRepository.findById(countC1.getUserId()).map(u -> (u.getName() != null ? u.getName() : (u.getFirstLastName() + " " + u.getSecondLastName()).trim())).orElse("") : null)
                        .conteo1Comentario(countC1 != null ? countC1.getComment() : null)
                        
                        // Conteo C2 con comentario
                        .conteo2Valor(countC2 != null ? countC2.getCountedValue() : null)
                        .conteo2Fecha(countC2 != null ? countC2.getCreatedAt() : null)
                        .conteo2UsuarioEmail(countC2 != null && countC2.getUserId() != null ? 
                                userRepository.findById(countC2.getUserId()).map(BeanUser::getEmail).orElse("") : null)
                        .conteo2UsuarioNombre(countC2 != null && countC2.getUserId() != null ? 
                                userRepository.findById(countC2.getUserId()).map(u -> (u.getName() != null ? u.getName() : (u.getFirstLastName() + " " + u.getSecondLastName()).trim())).orElse("") : null)
                        .conteo2Comentario(countC2 != null ? countC2.getComment() : null)
                        
                        // Análisis
                        .diferencia(diferencia)
                        .diferenciaPorcentaje(diferenciaPorcentaje)
                        .conteoCompleto(countC1 != null && countC2 != null)
                        .statusConteo(countC1 == null ? "SIN_CONTEO" : (countC2 == null ? "CONTEO_PARCIAL" : "CONTEO_COMPLETO"))
                        
                        .build();

                result.add(dto);

            } catch (Exception e) {
                log.error("Error procesando folio {}: {}", label.getFolio(), e.getMessage());
            }
        }

        log.info("✓ Reporte generado: {} marbetes con comentarios", result.size());
        return result;
    }

    // ── clase interna de soporte para generateInventoryFile ─────────────
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ProductExistencias {
        private String     claveProducto;
        private String     descripcion;
        private BigDecimal existencias;

        public void sumarExistencias(BigDecimal cantidad) {
            this.existencias = this.existencias.add(cantidad);
        }
    }
}

