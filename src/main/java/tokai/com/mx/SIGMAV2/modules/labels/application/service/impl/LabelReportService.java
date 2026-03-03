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
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.CancelledLabelsReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ComparativeReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DifferencesReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.DistributionReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.LabelListReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.PendingLabelsReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ProductDetailReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.WarehouseDetailReportDTO;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio especializado en la generación de reportes y el archivo TXT de existencias.
 * Extraído de LabelServiceImpl para cumplir con el Principio de Responsabilidad Única.
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

    @Transactional(readOnly = true)
    public List<DistributionReportDTO> getDistributionReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de distribución: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findPrintedLabelsByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findPrintedLabelsByPeriod(filter.getPeriodId());

        Map<String, List<Label>> grouped = labels.stream()
                .collect(Collectors.groupingBy(l -> l.getWarehouseId() + "_" + l.getCreatedBy()));

        List<DistributionReportDTO> result = new ArrayList<>();
        for (List<Label> group : grouped.values()) {
            if (group.isEmpty()) continue;
            Label first = group.get(0);
            WarehouseEntity wh = warehouseRepository.findById(first.getWarehouseId()).orElse(null);
            var user = userRepository.findById(first.getCreatedBy()).orElse(null);
            String userName = user != null ? user.getEmail() : "Usuario " + first.getCreatedBy();
            Long minF = group.stream().map(Label::getFolio).min(Long::compareTo).orElse(0L);
            Long maxF = group.stream().map(Label::getFolio).max(Long::compareTo).orElse(0L);
            result.add(new DistributionReportDTO(
                    userName,
                    wh != null ? wh.getWarehouseKey() : String.valueOf(first.getWarehouseId()),
                    wh != null ? wh.getNameWarehouse() : "Almacén " + first.getWarehouseId(),
                    minF, maxF, group.size()));
        }
        return result.stream().sorted(Comparator.comparing(DistributionReportDTO::getClaveAlmacen)).toList();
    }

    @Transactional(readOnly = true)
    public List<LabelListReportDTO> getLabelListReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de listado: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findByPeriodIdAndWarehouseId(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findByPeriodId(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);

        return labels.stream().map(label -> {
            ProductEntity p = productRepository.findById(label.getProductId()).orElse(null);
            WarehouseEntity w = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
            BigDecimal c1 = null, c2 = null;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 1) c1 = e.getCountedValue();
                if (e.getCountNumber() == 2) c2 = e.getCountedValue();
            }
            return new LabelListReportDTO(
                    label.getFolio(),
                    p != null ? p.getCveArt() : "", p != null ? p.getDescr() : "", p != null ? p.getUniMed() : "",
                    w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                    c1, c2, label.getEstado().name(), label.getEstado() == Label.State.CANCELADO);
        }).sorted(Comparator.comparing(LabelListReportDTO::getNumeroMarbete)).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingLabelsReportDTO> getPendingLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de pendientes: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        List<PendingLabelsReportDTO> result = new ArrayList<>();

        for (Label label : labels) {
            BigDecimal c1 = null, c2 = null;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 1) c1 = e.getCountedValue();
                if (e.getCountNumber() == 2) c2 = e.getCountedValue();
            }
            if (c1 == null || c2 == null) {
                ProductEntity p = productRepository.findById(label.getProductId()).orElse(null);
                WarehouseEntity w = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
                result.add(new PendingLabelsReportDTO(
                        label.getFolio(),
                        p != null ? p.getCveArt() : "", p != null ? p.getDescr() : "", p != null ? p.getUniMed() : "",
                        w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                        c1, c2, label.getEstado().name()));
            }
        }
        return result.stream().sorted(Comparator.comparing(PendingLabelsReportDTO::getNumeroMarbete)).toList();
    }

    @Transactional(readOnly = true)
    public List<DifferencesReportDTO> getDifferencesReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de diferencias: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        List<DifferencesReportDTO> result = new ArrayList<>();

        for (Label label : labels) {
            BigDecimal c1 = null, c2 = null;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 1) c1 = e.getCountedValue();
                if (e.getCountNumber() == 2) c2 = e.getCountedValue();
            }
            if (c1 != null && c2 != null
                    && c1.compareTo(BigDecimal.ZERO) > 0
                    && c2.compareTo(BigDecimal.ZERO) > 0
                    && c1.compareTo(c2) != 0) {
                ProductEntity p = productRepository.findById(label.getProductId()).orElse(null);
                WarehouseEntity w = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
                result.add(new DifferencesReportDTO(
                        label.getFolio(),
                        p != null ? p.getCveArt() : "", p != null ? p.getDescr() : "", p != null ? p.getUniMed() : "",
                        w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                        c1, c2, c1.subtract(c2).abs(), label.getEstado().name()));
            }
        }
        return result.stream().sorted(Comparator.comparing(DifferencesReportDTO::getNumeroMarbete)).toList();
    }

    @Transactional(readOnly = true)
    public List<CancelledLabelsReportDTO> getCancelledLabelsReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte de cancelados: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<LabelCancelled> cancelledLabels = filter.getWarehouseId() != null
                ? jpaLabelCancelledRepository.findByPeriodIdAndWarehouseIdAndReactivado(filter.getPeriodId(), filter.getWarehouseId(), false)
                : jpaLabelCancelledRepository.findByPeriodIdAndReactivado(filter.getPeriodId(), false);

        return cancelledLabels.stream().map(c -> {
            ProductEntity p = productRepository.findById(c.getProductId()).orElse(null);
            WarehouseEntity w = warehouseRepository.findById(c.getWarehouseId()).orElse(null);
            var user = userRepository.findById(c.getCanceladoBy()).orElse(null);
            return new CancelledLabelsReportDTO(
                    c.getFolio(),
                    p != null ? p.getCveArt() : "", p != null ? p.getDescr() : "", p != null ? p.getUniMed() : "",
                    w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                    c.getConteo1AlCancelar(), c.getConteo2AlCancelar(),
                    c.getMotivoCancelacion(), c.getCanceladoAt(),
                    user != null ? user.getEmail() : "Usuario " + c.getCanceladoBy());
        }).sorted(Comparator.comparing(CancelledLabelsReportDTO::getNumeroMarbete)).toList();
    }

    @Transactional(readOnly = true)
    public List<ComparativeReportDTO> getComparativeReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte comparativo: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<String, List<Label>> grouped = labels.stream()
                .collect(Collectors.groupingBy(l -> l.getProductId() + "_" + l.getWarehouseId()));

        List<ComparativeReportDTO> result = new ArrayList<>();
        for (List<Label> group : grouped.values()) {
            if (group.isEmpty()) continue;
            Label first = group.get(0);
            BigDecimal fisicas = BigDecimal.ZERO;
            int pendientes = 0;

            for (Label label : group) {
                BigDecimal c2 = null;
                for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                    if (e.getCountNumber() == 2) { c2 = e.getCountedValue(); break; }
                }
                if (c2 != null) fisicas = fisicas.add(c2);
                else pendientes++;
            }

            if (pendientes > 0) {
                log.warn("Producto {} almacén {}: {} marbetes pendientes de C2 no contabilizados",
                        first.getProductId(), first.getWarehouseId(), pendientes);
            }

            BigDecimal teoricas = BigDecimal.ZERO;
            try {
                var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                        first.getProductId(), first.getWarehouseId(), first.getPeriodId());
                if (stockOpt.isPresent() && stockOpt.get().getExistQty() != null) {
                    teoricas = stockOpt.get().getExistQty();
                }
            } catch (Exception e) {
                log.warn("No se pudieron obtener existencias teóricas: {}", e.getMessage());
            }

            BigDecimal diferencia = fisicas.subtract(teoricas);
            BigDecimal porcentaje = BigDecimal.ZERO;
            if (teoricas.compareTo(BigDecimal.ZERO) != 0) {
                porcentaje = diferencia.divide(teoricas, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
            }

            ProductEntity p = productRepository.findById(first.getProductId()).orElse(null);
            WarehouseEntity w = warehouseRepository.findById(first.getWarehouseId()).orElse(null);
            result.add(new ComparativeReportDTO(
                    w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                    p != null ? p.getCveArt() : "", p != null ? p.getDescr() : "", p != null ? p.getUniMed() : "",
                    fisicas, teoricas, diferencia, porcentaje));
        }
        return result.stream()
                .sorted(Comparator.comparing(ComparativeReportDTO::getClaveAlmacen)
                        .thenComparing(ComparativeReportDTO::getClaveProducto))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseDetailReportDTO> getWarehouseDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte almacén-detalle: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);

        return labels.stream().map(label -> {
            ProductEntity p = productRepository.findById(label.getProductId()).orElse(null);
            WarehouseEntity w = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
            BigDecimal cantidad = BigDecimal.ZERO;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) { cantidad = e.getCountedValue(); break; }
                else if (e.getCountNumber() == 1) { cantidad = e.getCountedValue(); }
            }
            return new WarehouseDetailReportDTO(
                    w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                    p != null ? p.getCveArt() : "", p != null ? p.getDescr() : "", p != null ? p.getUniMed() : "",
                    label.getFolio(), cantidad, label.getEstado().name(),
                    label.getEstado() == Label.State.CANCELADO);
        }).sorted(Comparator.comparing(WarehouseDetailReportDTO::getClaveAlmacen)
                .thenComparing(WarehouseDetailReportDTO::getClaveProducto)
                .thenComparing(WarehouseDetailReportDTO::getNumeroMarbete))
          .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDetailReportDTO> getProductDetailReport(ReportFilterDTO filter, Long userId, String userRole) {
        log.info("Generando reporte producto-detalle: periodo={}, almacén={}", filter.getPeriodId(), filter.getWarehouseId());
        if (filter.getWarehouseId() != null) {
            warehouseAccessService.validateWarehouseAccess(userId, filter.getWarehouseId(), userRole);
        }

        List<Label> labels = filter.getWarehouseId() != null
                ? jpaLabelRepository.findNonCancelledByPeriodAndWarehouse(filter.getPeriodId(), filter.getWarehouseId())
                : jpaLabelRepository.findNonCancelledByPeriod(filter.getPeriodId());

        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);
        Map<Long, BigDecimal> totalsByProduct = new HashMap<>();

        for (Label label : labels) {
            BigDecimal qty = BigDecimal.ZERO;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) { qty = e.getCountedValue(); break; }
                else if (e.getCountNumber() == 1) { qty = e.getCountedValue(); }
            }
            totalsByProduct.merge(label.getProductId(), qty, BigDecimal::add);
        }

        return labels.stream().map(label -> {
            ProductEntity p = productRepository.findById(label.getProductId()).orElse(null);
            WarehouseEntity w = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
            BigDecimal existencias = BigDecimal.ZERO;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) { existencias = e.getCountedValue(); break; }
                else if (e.getCountNumber() == 1) { existencias = e.getCountedValue(); }
            }
            return new ProductDetailReportDTO(
                    p != null ? p.getCveArt() : "", p != null ? p.getDescr() : "", p != null ? p.getUniMed() : "",
                    w != null ? w.getWarehouseKey() : "", w != null ? w.getNameWarehouse() : "",
                    label.getFolio(), existencias, totalsByProduct.get(label.getProductId()));
        }).sorted(Comparator.comparing(ProductDetailReportDTO::getClaveProducto)
                .thenComparing(ProductDetailReportDTO::getClaveAlmacen)
                .thenComparing(ProductDetailReportDTO::getNumeroMarbete))
          .toList();
    }

    /**
     * Genera archivo TXT de existencias y lo retorna como bytes (sin escribir a disco del servidor).
     * Resuelve el problema O5: antes escribía a C:\Sistemas\... en el servidor.
     */
    @Transactional(readOnly = true)
    public GenerateFileResponseDTO generateInventoryFile(Long periodId, Long userId, String userRole) {
        log.info("Generando archivo TXT de existencias para periodo {}", periodId);

        var periodEntity = jpaPeriodRepository.findById(periodId)
                .orElseThrow(() -> new RuntimeException("Periodo no encontrado: " + periodId));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.of("es", "ES"));
        String periodName = periodEntity.getDate().format(fmt);
        periodName = periodName.substring(0, 1).toUpperCase() + periodName.substring(1).replace(" ", "");

        List<Label> labels = jpaLabelRepository.findNonCancelledByPeriod(periodId);
        Map<Long, List<LabelCountEvent>> countMap = batchLoadCounts(labels);

        // Agrupar y sumar existencias por producto
        Map<Long, ProductExistencias> productoMap = new LinkedHashMap<>();
        for (Label label : labels) {
            BigDecimal cantidad = BigDecimal.ZERO;
            for (LabelCountEvent e : countMap.getOrDefault(label.getFolio(), List.of())) {
                if (e.getCountNumber() == 2) { cantidad = e.getCountedValue(); break; }
                else if (e.getCountNumber() == 1) { cantidad = e.getCountedValue(); }
            }
            productoMap.computeIfAbsent(label.getProductId(), k -> {
                ProductEntity prod = productRepository.findById(k).orElse(null);
                return new ProductExistencias(
                        prod != null ? prod.getCveArt() : "",
                        prod != null ? prod.getDescr() : "",
                        BigDecimal.ZERO);
            }).sumarExistencias(cantidad);
        }

        List<ProductExistencias> lista = new ArrayList<>(productoMap.values());
        lista.sort(Comparator.comparing(ProductExistencias::getClaveProducto));

        String fileName = "Existencias_" + periodName + ".txt";

        // Construir contenido como bytes para retornar al cliente
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

    // ── helpers privados ────────────────────────────────────────────────────

    /**
     * Carga todos los conteos de una lista de marbetes en una sola query (evita N+1).
     */
    private Map<Long, List<LabelCountEvent>> batchLoadCounts(List<Label> labels) {
        if (labels.isEmpty()) return new HashMap<>();
        List<Long> folios = labels.stream().map(Label::getFolio).toList();
        return jpaLabelCountEventRepository
                .findByFolioInOrderByFolioAscCountNumberAsc(folios)
                .stream()
                .collect(Collectors.groupingBy(LabelCountEvent::getFolio));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ProductExistencias {
        private String claveProducto;
        private String descripcion;
        private BigDecimal existencias;

        public void sumarExistencias(BigDecimal cantidad) {
            this.existencias = this.existencias.add(cantidad);
        }
    }
}

