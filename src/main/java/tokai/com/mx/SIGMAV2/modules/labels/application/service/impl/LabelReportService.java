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
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCancelledRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
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
        log.info("Reporte distribución: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        validateAccess(userId, filter.getWarehouseId(), userRole);

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findPrintedLabelsByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findPrintedLabelsByPeriod(filter.getPeriodId());

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
        log.info("Reporte listado: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        validateAccess(userId, filter.getWarehouseId(), userRole);

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
        validateAccess(userId, filter.getWarehouseId(), userRole);

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
        validateAccess(userId, filter.getWarehouseId(), userRole);

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
        validateAccess(userId, filter.getWarehouseId(), userRole);

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
        validateAccess(userId, filter.getWarehouseId(), userRole);

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
            int        pendientes = 0;

            for (Label label : group) {
                BigDecimal c2 = null;
                for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                    if (e.getCountNumber() == 2) { c2 = e.getCountedValue(); break; }
                }
                if (c2 != null) fisicas = fisicas.add(c2);
                else pendientes++;
            }

            if (pendientes > 0) {
                log.warn("Producto {} almacén {}: {} marbetes sin C2 no contabilizados en físicas",
                        first.getProductId(), first.getWarehouseId(), pendientes);
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
     * CORRECCIÓN: fuenteConteo explícito — "C2", "C1" o "SIN_CONTEO".
     * El frontend puede mostrar advertencia cuando no hay C2.
     */
    @Transactional(readOnly = true)
    public List<WarehouseDetailReportDTO> getWarehouseDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Reporte almacén-detalle: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        validateAccess(userId, filter.getWarehouseId(), userRole);

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        return labels.stream().map(label -> {
            ProductEntity   p       = prodMap.get(label.getProductId());
            WarehouseEntity w       = whMap.get(label.getWarehouseId());
            BigDecimal      cantidad = BigDecimal.ZERO;
            String          fuente  = "SIN_CONTEO";

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
            return new WarehouseDetailReportDTO(
                    w != null ? w.getWarehouseKey()  : String.valueOf(label.getWarehouseId()),
                    w != null ? w.getNameWarehouse() : "Almacén " + label.getWarehouseId(),
                    p != null ? p.getCveArt()        : String.valueOf(label.getProductId()),
                    p != null ? p.getDescr()         : "",
                    p != null ? p.getUniMed()        : "",
                    label.getFolio(), cantidad, label.getEstado().name(),
                    label.getEstado() == Label.State.CANCELADO, fuente);
        }).sorted(Comparator.comparing(WarehouseDetailReportDTO::getClaveAlmacen)
                .thenComparing(WarehouseDetailReportDTO::getClaveProducto)
                .thenComparing(WarehouseDetailReportDTO::getNumeroMarbete))
          .toList();
    }

    /**
     * Reporte producto-detalle.
     * CORRECCIÓN:
     *  - N+1 eliminado con batchLoadProducts / batchLoadWarehouses.
     *  - fuenteConteo explícito por marbete.
     *  - Total solo suma C2 (consistencia con reporte comparativo).
     */
    @Transactional(readOnly = true)
    public List<ProductDetailReportDTO> getProductDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Reporte producto-detalle: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        validateAccess(userId, filter.getWarehouseId(), userRole);

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, ProductEntity>         prodMap   = batchLoadProducts(labels);
        Map<Long, WarehouseEntity>       whMap     = batchLoadWarehouses(labels);

        // Calcular totales por producto sumando SOLO C2 (mismo criterio que comparativo)
        Map<Long, BigDecimal> totalsByProduct = new HashMap<>();
        for (Label label : labels) {
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) {
                    totalsByProduct.merge(label.getProductId(), e.getCountedValue(), BigDecimal::add);
                    break;
                }
            }
        }

        return labels.stream().map(label -> {
            ProductEntity   p        = prodMap.get(label.getProductId());
            WarehouseEntity w        = whMap.get(label.getWarehouseId());
            BigDecimal      cantidad = BigDecimal.ZERO;
            String          fuente   = "SIN_CONTEO";

            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) {
                    cantidad = e.getCountedValue(); fuente = "C2"; break;
                } else if (e.getCountNumber() == 1) {
                    cantidad = e.getCountedValue(); fuente = "C1";
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
                    fuente);
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
            writer.write("CLAVE_PRODUCTO\tDESCRIPCION\tEXISTENCIAS");
            writer.newLine();
            writer.write("========================================");
            writer.newLine();
            for (ProductExistencias prod : lista) {
                writer.write(String.format("%s\t%s\t%s",
                        prod.getClaveProducto(),
                        prod.getDescripcion(),
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

    /** Valida acceso al almacén si se especificó uno concreto. */
    private void validateAccess(Long userId, Long warehouseId, String userRole) {
        if (warehouseId != null) {
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        }
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

