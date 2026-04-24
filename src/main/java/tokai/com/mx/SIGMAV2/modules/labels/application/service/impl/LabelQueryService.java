package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.*;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio especializado en consultas read-only del módulo de marbetes.
 *
 * Reglas de acceso:
 * - AUXILIAR_DE_CONTEO bypassa validación de almacén en getLabelForCount y getLabelsForCountList.
 * - Todos los demás métodos validan acceso al almacén antes de retornar datos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelQueryService {

    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    private final JpaLabelRequestRepository labelRequestRepository;
    private final JpaUserRepository userRepository;
    private final JpaLabelRepository jpaLabelRepository;
    private final JpaLabelCountEventRepository jpaLabelCountEventRepository;
    private final JpaPeriodRepository jpaPeriodRepository;

    @Transactional(readOnly = true)
    public PendingPrintCountResponseDTO getPendingPrintCount(PendingPrintCountRequestDTO dto, Long userId, String userRole) {
        if (!warehouseAccessService.hasFullAccess(userRole)) {
            warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        }

        List<Label> pendingLabels = dto.getProductId() != null
                ? persistence.findPendingLabelsByPeriodWarehouseAndProduct(dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId())
                : persistence.findPendingLabelsByPeriodAndWarehouse(dto.getPeriodId(), dto.getWarehouseId());

        String warehouseName = null;
        String periodName = null;
        try {
            warehouseName = warehouseRepository.findById(dto.getWarehouseId())
                    .map(WarehouseEntity::getNameWarehouse).orElse(null);
        } catch (Exception e) { log.warn("No se pudo obtener nombre del almacén: {}", e.getMessage()); }
        try {
            var period = jpaPeriodRepository.findById(dto.getPeriodId());
            if (period.isPresent() && period.get().getDate() != null) {
                periodName = period.get().getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        } catch (Exception e) { log.warn("No se pudo obtener nombre del periodo: {}", e.getMessage()); }

        return new PendingPrintCountResponseDTO((long) pendingLabels.size(), dto.getPeriodId(), dto.getWarehouseId(), warehouseName, periodName);
    }

    @Transactional(readOnly = true)
    public List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole) {
        final Long periodId = dto.getPeriodId() != null ? dto.getPeriodId()
                : persistence.findLastCreatedPeriodId()
                        .orElseThrow(() -> new RuntimeException("No hay periodos registrados"));

        final Long warehouseId = dto.getWarehouseId() != null ? dto.getWarehouseId()
                : warehouseRepository.findFirstByOrderByIdWarehouseAsc()
                        .map(WarehouseEntity::getIdWarehouse)
                        .orElseThrow(() -> new RuntimeException("No hay almacenes registrados"));

        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

        WarehouseEntity warehouseEntity = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado: " + warehouseId));

        Map<Long, LabelRequest> requestsByProduct = labelRequestRepository.findAll().stream()
                .filter(r -> periodId.equals(r.getPeriodId()) && warehouseId.equals(r.getWarehouseId()))
                .collect(Collectors.toMap(LabelRequest::getProductId, r -> r, (a, b) -> a));

        List<Label> labels = jpaLabelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId);

        // Solo contar GENERADOS — no IMPRESOS ni CANCELADOS
        Map<Long, Long> generatedByProduct = labels.stream()
                .filter(l -> l.getEstado() != null && l.getEstado().name().equals("GENERADO"))
                .collect(Collectors.groupingBy(Label::getProductId, Collectors.counting()));

        List<InventoryStockEntity> allStock = inventoryStockRepository
                .findByWarehouseIdWarehouseAndPeriodId(warehouseId, periodId);

        Set<Long> allProductIds = new HashSet<>();
        allStock.stream().filter(s -> s.getProduct() != null).forEach(s -> allProductIds.add(s.getProduct().getIdProduct()));
        allProductIds.addAll(requestsByProduct.keySet());
        allProductIds.addAll(generatedByProduct.keySet());

        List<LabelSummaryResponseDTO> allResults = new ArrayList<>();
        for (Long productId : allProductIds) {
            try {
                ProductEntity product = productRepository.findById(productId).orElse(null);
                if (product == null) continue;

                LabelRequest request = requestsByProduct.get(productId);
                int foliosSolicitados = request != null ? request.getRequestedLabels() : 0;
                int foliosExistentes = generatedByProduct.getOrDefault(productId, 0L).intValue();

                int existencias = 0;
                String estado = "SIN_STOCK";
                try {
                    var stock = inventoryStockRepository
                            .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId)
                            .orElse(null);
                    if (stock != null) {
                        existencias = stock.getExistQty() != null ? stock.getExistQty().intValue() : 0;
                        estado = stock.getStatus() != null ? stock.getStatus().name() : "A";
                    }
                } catch (Exception e) {
                    log.warn("No se pudieron obtener existencias para producto {}: {}", productId, e.getMessage());
                }

                List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(productId, periodId, warehouseId);
                boolean impreso = !prints.isEmpty();
                String fechaImpresion = prints.stream().map(LabelPrint::getPrintedAt).filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo).map(LocalDateTime::toString).orElse(null);

                List<Label> productLabels = labels.stream().filter(l -> l.getProductId().equals(productId))
                        .sorted(Comparator.comparing(Label::getFolio)).toList();
                Long primerFolio = productLabels.isEmpty() ? null : productLabels.getFirst().getFolio();
                Long ultimoFolio = productLabels.isEmpty() ? null : productLabels.getLast().getFolio();
                List<Long> foliosList = productLabels.stream().map(Label::getFolio).toList();

                allResults.add(LabelSummaryResponseDTO.builder()
                        .productId(productId).claveProducto(product.getCveArt()).nombreProducto(product.getDescr())
                        .claveAlmacen(warehouseEntity.getWarehouseKey()).nombreAlmacen(warehouseEntity.getNameWarehouse())
                        .foliosSolicitados(foliosSolicitados).foliosExistentes(foliosExistentes)
                        .estado(estado).existencias(existencias).impreso(impreso).fechaImpresion(fechaImpresion)
                        .primerFolio(primerFolio).ultimoFolio(ultimoFolio).folios(foliosList)
                        .build());
            } catch (Exception e) {
                log.error("Error procesando producto {}: {}", productId, e.getMessage());
            }
        }

        List<LabelSummaryResponseDTO> filtered = allResults;
        if (dto.getSearchText() != null && !dto.getSearchText().isBlank()) {
            String searchLower = dto.getSearchText().toLowerCase();
            filtered = allResults.stream().filter(item ->
                    (item.getClaveProducto() != null && item.getClaveProducto().toLowerCase().contains(searchLower)) ||
                    (item.getNombreProducto() != null && item.getNombreProducto().toLowerCase().contains(searchLower)) ||
                    (item.getClaveAlmacen() != null && item.getClaveAlmacen().toLowerCase().contains(searchLower)) ||
                    (item.getNombreAlmacen() != null && item.getNombreAlmacen().toLowerCase().contains(searchLower)) ||
                    (item.getEstado() != null && item.getEstado().toLowerCase().contains(searchLower)) ||
                    String.valueOf(item.getExistencias()).contains(searchLower)
            ).collect(Collectors.toCollection(ArrayList::new));
        }

        Comparator<LabelSummaryResponseDTO> comparator = getSummaryComparator(dto.getSortBy());
        if ("DESC".equalsIgnoreCase(dto.getSortDirection())) comparator = comparator.reversed();
        filtered.sort(comparator);

        int total = filtered.size();
        int start = dto.getPage() * dto.getSize();
        if (start >= total && total > 0) return new ArrayList<>();
        int end = Math.min(start + dto.getSize(), total);
        return start < total ? filtered.subList(start, end) : new ArrayList<>();
    }

    private Comparator<LabelSummaryResponseDTO> getSummaryComparator(String sortBy) {
        if (sortBy == null) sortBy = "claveProducto";
        return switch (sortBy.toLowerCase()) {
            case "foliosexistentes" -> Comparator.comparing(LabelSummaryResponseDTO::getFoliosExistentes);
            case "producto", "nombreproducto" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreProducto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "clavealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getClaveAlmacen, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "almacen", "nombrealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreAlmacen, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "estado" -> Comparator.comparing(LabelSummaryResponseDTO::getEstado, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "existencias" -> Comparator.comparing(LabelSummaryResponseDTO::getExistencias);
            default -> Comparator.comparing(LabelSummaryResponseDTO::getClaveProducto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

    @Transactional(readOnly = true)
    public LabelStatusResponseDTO getLabelStatus(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        var builder = LabelStatusResponseDTO.builder().folio(folio);
        try {
            var optLabel = persistence.findByFolio(folio);
            if (optLabel.isEmpty()) {
                return builder.estado("NO_EXISTE").impreso(false).mensaje("El folio no existe").build();
            }
            var label = optLabel.get();
            Long actualPeriodId = periodId != null ? periodId : label.getPeriodId();
            Long actualWarehouseId = warehouseId != null ? warehouseId : label.getWarehouseId();
            builder.periodId(actualPeriodId).warehouseId(actualWarehouseId).productId(label.getProductId())
                    .estado(label.getEstado() != null ? label.getEstado().name() : "SIN_ESTADO");

            productRepository.findById(label.getProductId()).ifPresent(p -> {
                builder.claveProducto(p.getCveArt()); builder.nombreProducto(p.getDescr());
            });
            warehouseRepository.findById(label.getWarehouseId()).ifPresent(w -> {
                builder.claveAlmacen(w.getWarehouseKey()); builder.nombreAlmacen(w.getNameWarehouse());
            });

            var prints = persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), actualPeriodId, actualWarehouseId);
            boolean impreso = !prints.isEmpty();
            String fechaImpresion = prints.stream().map(LabelPrint::getPrintedAt).filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo).map(LocalDateTime::toString).orElse(null);
            builder.impreso(impreso).fechaImpresion(fechaImpresion);

            String mensaje = label.getEstado() == null ? "Sin estado definido."
                    : switch (label.getEstado()) {
                        case CANCELADO -> "El marbete está CANCELADO.";
                        case IMPRESO -> "Ya fue impreso. Puede reimprimir si lo necesita.";
                        case GENERADO -> "Listo para imprimir.";
                    };
            builder.mensaje(mensaje);
        } catch (Exception e) {
            builder.estado("ERROR").impreso(false).mensaje("Error al consultar: " + e.getMessage());
        }
        return builder.build();
    }

    @Transactional(readOnly = true)
    public long countLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId) {
        return persistence.countByPeriodIdAndWarehouseId(periodId, warehouseId);
    }

    @Transactional(readOnly = true)
    public List<LabelDetailDTO> getLabelsByProduct(Long productId, Long periodId, Long warehouseId, Long userId, String userRole) {
        warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        int existencias = 0;
        try {
            var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId);
            if (stockOpt.isPresent()) existencias = stockOpt.get().getExistQty() != null ? stockOpt.get().getExistQty().intValue() : 0;
        } catch (Exception e) { log.warn("No se pudieron obtener existencias: {}", e.getMessage()); }

        final int existenciasFinal = existencias;
        return persistence.findByProductPeriodWarehouse(productId, periodId, warehouseId).stream()
                .map(label -> LabelDetailDTO.builder()
                        .folio(label.getFolio()).productId(label.getProductId())
                        .claveProducto(product.getCveArt()).nombreProducto(product.getDescr())
                        .warehouseId(label.getWarehouseId())
                        .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                        .periodId(label.getPeriodId()).estado(label.getEstado().name())
                        .createdAt(label.getCreatedAt() != null ? label.getCreatedAt().toString() : null)
                        .impresoAt(label.getImpresoAt() != null ? label.getImpresoAt().toString() : null)
                        .existencias(existenciasFinal).build())
                .sorted(Comparator.comparing(LabelDetailDTO::getFolio))
                .toList();
    }

    @Transactional(readOnly = true)
    public LabelForCountDTO getLabelForCount(Long folio, Long periodId, Long warehouseId, Long userId, String userRole) {
        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        // AUXILIAR_DE_CONTEO bypassa validación de almacén
        if (!userRole.toUpperCase().equals("AUXILIAR_DE_CONTEO")) {
            warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
        }

        ProductEntity product = productRepository.findById(label.getProductId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));

        List<LabelCountEvent> events = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
        BigDecimal c1 = null, c2 = null;
        String comentarioC1 = null, usuarioC1 = null, comentarioC2 = null, usuarioC2 = null;

        for (LabelCountEvent e : events) {
            if (e.getCountNumber() == 1) {
                c1 = e.getCountedValue(); comentarioC1 = e.getComment();
                if (e.getUserId() != null)
                    usuarioC1 = userRepository.findById(e.getUserId())
                            .map(u -> u.getName() != null ? u.getName() : u.getEmail()).orElse(null);
            }
            if (e.getCountNumber() == 2) {
                c2 = e.getCountedValue(); comentarioC2 = e.getComment();
                if (e.getUserId() != null)
                    usuarioC2 = userRepository.findById(e.getUserId())
                            .map(u -> u.getName() != null ? u.getName() : u.getEmail()).orElse(null);
            }
        }
        BigDecimal diferencia = (c1 != null && c2 != null) ? c2.subtract(c1) : null;

        BigDecimal existQty = null;
        try {
            var stockOpt = inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId());
            if (stockOpt.isPresent()) existQty = stockOpt.get().getExistQty();
        } catch (Exception e) { log.warn("Error obteniendo existencias: {}", e.getMessage()); }

        boolean cancelado = label.getEstado() == Label.State.CANCELADO;
        String mensaje = cancelado ? "Este marbete está CANCELADO"
                : (c1 != null && c2 != null) ? "Ambos conteos registrados"
                : (c1 != null) ? "Primer conteo registrado, falta C2" : "Listo para primer conteo";

        return LabelForCountDTO.builder()
                .folio(label.getFolio()).periodId(label.getPeriodId()).warehouseId(label.getWarehouseId())
                .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                .claveProducto(product.getCveArt()).descripcionProducto(product.getDescr())
                .unidadMedida(product.getUniMed()).cancelado(cancelado)
                .conteo1(c1).conteo2(c2).diferencia(diferencia)
                .estado(label.getEstado() != null ? label.getEstado().name() : "SIN_ESTADO")
                .impreso(!persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), periodId, label.getWarehouseId()).isEmpty())
                .mensaje(mensaje).existQty(existQty).existQtyUnidad(product.getUniMed())
                .conteo1Comentario(comentarioC1).conteo1UsuarioNombre(usuarioC1)
                .conteo2Comentario(comentarioC2).conteo2UsuarioNombre(usuarioC2)
                .build();
    }

    @Transactional(readOnly = true)
    public List<LabelForCountDTO> getLabelsForCountList(Long periodId, Long warehouseId, Long userId, String userRole) {
        String roleUpper = userRole != null ? userRole.toUpperCase() : "";

        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            if (warehouseId == null) throw new IllegalArgumentException("El almacén es obligatorio para este rol");
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
        }

        List<Label> labels;
        Map<Long, WarehouseEntity> warehouseMap = new HashMap<>();

        if (warehouseId != null) {
            WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));
            warehouseMap.put(warehouseId, warehouse);
            labels = jpaLabelRepository.findImpresosForCountList(periodId, warehouseId);
        } else {
            labels = jpaLabelRepository.findImpresosForCountByPeriod(periodId);
            Set<Long> warehouseIds = labels.stream().map(Label::getWarehouseId).collect(Collectors.toSet());
            for (Long wId : warehouseIds) {
                warehouseRepository.findById(wId).ifPresent(w -> warehouseMap.put(wId, w));
            }
        }

        if (labels.isEmpty()) return new ArrayList<>();

        List<Long> folios = labels.stream().map(Label::getFolio).toList();
        Map<Long, List<LabelCountEvent>> countsByFolio = jpaLabelCountEventRepository
                .findByFolioInOrderByFolioAscCountNumberAsc(folios).stream()
                .collect(Collectors.groupingBy(LabelCountEvent::getFolio));

        Set<Long> productIds = labels.stream().map(Label::getProductId).collect(Collectors.toSet());
        Map<Long, BigDecimal> stockByProduct = new HashMap<>();
        if (warehouseId != null) {
            for (Long productId : productIds) {
                try {
                    inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId)
                            .ifPresent(s -> stockByProduct.put(productId, s.getExistQty()));
                } catch (Exception e) { log.debug("Sin stock para producto {}: {}", productId, e.getMessage()); }
            }
        }

        List<LabelForCountDTO> result = new ArrayList<>();
        for (Label label : labels) {
            try {
                ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
                if (product == null) continue;

                WarehouseEntity warehouse = warehouseMap.get(label.getWarehouseId());
                if (warehouse == null) {
                    warehouse = warehouseRepository.findById(label.getWarehouseId())
                            .orElseThrow(() -> new RuntimeException("Almacén no encontrado"));
                    warehouseMap.put(label.getWarehouseId(), warehouse);
                }

                List<LabelCountEvent> events = countsByFolio.getOrDefault(label.getFolio(), List.of());
                BigDecimal c1 = null, c2 = null;
                String comentarioC1 = null, usuarioC1 = null, comentarioC2 = null, usuarioC2 = null;

                for (LabelCountEvent e : events) {
                    if (e.getCountNumber() == 1) {
                        c1 = e.getCountedValue(); comentarioC1 = e.getComment();
                        if (e.getUserId() != null)
                            usuarioC1 = userRepository.findById(e.getUserId())
                                    .map(u -> u.getName() != null ? u.getName() : u.getEmail()).orElse(null);
                    }
                    if (e.getCountNumber() == 2) {
                        c2 = e.getCountedValue(); comentarioC2 = e.getComment();
                        if (e.getUserId() != null)
                            usuarioC2 = userRepository.findById(e.getUserId())
                                    .map(u -> u.getName() != null ? u.getName() : u.getEmail()).orElse(null);
                    }
                }
                BigDecimal diferencia = (c1 != null && c2 != null) ? c2.subtract(c1) : null;
                String mensaje = (c1 != null && c2 != null) ? "Completo" : (c1 != null) ? "Pendiente C2" : "Pendiente C1";

                result.add(LabelForCountDTO.builder()
                        .folio(label.getFolio()).periodId(label.getPeriodId()).warehouseId(label.getWarehouseId())
                        .claveAlmacen(warehouse.getWarehouseKey()).nombreAlmacen(warehouse.getNameWarehouse())
                        .claveProducto(product.getCveArt()).descripcionProducto(product.getDescr())
                        .unidadMedida(product.getUniMed()).cancelado(false)
                        .conteo1(c1).conteo2(c2).diferencia(diferencia)
                        .estado(label.getEstado() != null ? label.getEstado().name() : "SIN_ESTADO")
                        .impreso(true).mensaje(mensaje).existQty(stockByProduct.get(label.getProductId()))
                        .existQtyUnidad(product.getUniMed())
                        .conteo1Comentario(comentarioC1).conteo1UsuarioNombre(usuarioC1)
                        .conteo2Comentario(comentarioC2).conteo2UsuarioNombre(usuarioC2)
                        .build());
            } catch (Exception e) {
                log.error("Error procesando folio {}: {}", label.getFolio(), e.getMessage());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public LabelFullDetailDTO getLabelFullDetail(Long folio, Long userId, String userRole) {
        log.info("📋 Obteniendo información COMPLETA del marbete folio={}", folio);
        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        LabelFullDetailDTO.LabelFullDetailDTOBuilder builder = LabelFullDetailDTO.builder();
        builder.folio(folio).estado(label.getEstado() != null ? label.getEstado().name() : "DESCONOCIDO")
                .createdAt(label.getCreatedAt()).impresoAt(label.getImpresoAt());

        builder.createdByUserId(label.getCreatedBy());
        try {
            userRepository.findById(label.getCreatedBy()).ifPresent(u ->
                builder.createdByEmail(u.getEmail())
                        .createdByFullName(u.getName() + " " + u.getFirstLastName())
                        .createdByRole(u.getRole() != null ? u.getRole().name() : null));
        } catch (Exception e) { log.warn("No se pudo obtener info del usuario creador: {}", e.getMessage()); }

        try {
            productRepository.findById(label.getProductId()).ifPresent(p ->
                builder.productId(p.getIdProduct()).claveProducto(p.getCveArt())
                        .nombreProducto(p.getDescr()).unidadMedida(p.getUniMed()).descripcionProducto(p.getDescr()));
        } catch (Exception e) { log.warn("No se pudo obtener info del producto: {}", e.getMessage()); }

        try {
            warehouseRepository.findById(label.getWarehouseId()).ifPresent(w ->
                builder.warehouseId(w.getIdWarehouse()).claveAlmacen(w.getWarehouseKey()).nombreAlmacen(w.getNameWarehouse()));
        } catch (Exception e) { log.warn("No se pudo obtener info del almacén: {}", e.getMessage()); }

        builder.periodId(label.getPeriodId());
        try {
            jpaPeriodRepository.findById(label.getPeriodId()).ifPresent(p -> builder.periodDate(p.getDate()));
        } catch (Exception e) { log.warn("No se pudo obtener info del período: {}", e.getMessage()); }

        try {
            inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                    label.getProductId(), label.getWarehouseId(), label.getPeriodId())
                    .ifPresent(s -> builder.existenciasTeoricas(s.getExistQty())
                            .statusExistencias(s.getStatus() != null ? s.getStatus().name() : "ACTIVO"));
        } catch (Exception e) { log.warn("No se pudo obtener existencias: {}", e.getMessage()); }

        // Conteos
        List<LabelCountEvent> countEvents = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
        List<LabelFullDetailDTO.CountEventHistoryDTO> countHistory = new ArrayList<>();
        BigDecimal c1 = null, c2 = null;
        LocalDateTime c1Fecha = null, c2Fecha = null;
        Long c1UserId = null, c2UserId = null;
        Integer c1Intentos = 0, c2Intentos = 0;

        for (LabelCountEvent event : countEvents) {
            if (event.getCountNumber() == 1) { c1 = event.getCountedValue(); c1Fecha = event.getCreatedAt(); c1UserId = event.getUserId(); c1Intentos++; }
            else if (event.getCountNumber() == 2) { c2 = event.getCountedValue(); c2Fecha = event.getCreatedAt(); c2UserId = event.getUserId(); c2Intentos++; }
            try {
                String userEmail = "DESCONOCIDO", userName = "DESCONOCIDO";
                var userOpt = userRepository.findById(event.getUserId());
                if (userOpt.isPresent()) { userEmail = userOpt.get().getEmail(); userName = userOpt.get().getName() + " " + userOpt.get().getFirstLastName(); }
                countHistory.add(LabelFullDetailDTO.CountEventHistoryDTO.builder()
                        .countNumber(event.getCountNumber()).value(event.getCountedValue()).recordedAt(event.getCreatedAt())
                        .recordedByUserId(event.getUserId()).recordedByEmail(userEmail).recordedByNombre(userName)
                        .action(event.getUpdatedAt() != null ? "UPDATED" : "CREATED")
                        .description("Conteo C" + event.getCountNumber() + ": " + event.getCountedValue()).build());
            } catch (Exception e) { log.warn("No se pudo obtener info del usuario de conteo: {}", e.getMessage()); }
        }

        builder.conteo1Valor(c1).conteo1Fecha(c1Fecha).conteo1UsuarioId(c1UserId).conteo1Intentos(c1Intentos)
               .conteo2Valor(c2).conteo2Fecha(c2Fecha).conteo2UsuarioId(c2UserId).conteo2Intentos(c2Intentos);

        if (c1UserId != null) {
            try { userRepository.findById(c1UserId).ifPresent(u -> builder.conteo1UsuarioEmail(u.getEmail()).conteo1UsuarioNombre(u.getName() + " " + u.getFirstLastName())); }
            catch (Exception e) { log.warn("No se pudo obtener info del usuario C1: {}", e.getMessage()); }
        }
        if (c2UserId != null) {
            try { userRepository.findById(c2UserId).ifPresent(u -> builder.conteo2UsuarioEmail(u.getEmail()).conteo2UsuarioNombre(u.getName() + " " + u.getFirstLastName())); }
            catch (Exception e) { log.warn("No se pudo obtener info del usuario C2: {}", e.getMessage()); }
        }
        builder.countHistory(countHistory);

        if (c1 != null && c2 != null) {
            BigDecimal diff = c2.subtract(c1);
            builder.diferencia(diff).conteoCompleto(true).statusConteo("COMPLETO");
            if (c1.compareTo(BigDecimal.ZERO) > 0)
                builder.diferenciaPorcentaje(String.format("%.2f%%", (diff.doubleValue() / c1.doubleValue()) * 100));
        } else if (c1 != null) {
            builder.conteoCompleto(false).statusConteo("PENDIENTE C2");
        } else {
            builder.conteoCompleto(false).statusConteo("PENDIENTE C1");
        }

        // Impresiones
        List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), label.getPeriodId(), label.getWarehouseId());
        List<LabelFullDetailDTO.PrintEventDTO> printHistory = new ArrayList<>();
        Boolean impreso = !prints.isEmpty();
        LocalDateTime primeraImpresion = null; Long primeraImpresionUserId = null;
        LocalDateTime ultimaReimpresion = null; Long ultimaReimpresionUserId = null;
        Integer totalReimpresiones = 0;

        if (!prints.isEmpty()) {
            primeraImpresion = prints.get(0).getPrintedAt(); primeraImpresionUserId = prints.get(0).getPrintedBy();
            ultimaReimpresion = prints.get(prints.size()-1).getPrintedAt(); ultimaReimpresionUserId = prints.get(prints.size()-1).getPrintedBy();
            totalReimpresiones = prints.size() - 1;
            for (int i = 0; i < prints.size(); i++) {
                LabelPrint print = prints.get(i);
                try {
                    String userEmail = "DESCONOCIDO", userName = "DESCONOCIDO";
                    var userOpt = userRepository.findById(print.getPrintedBy());
                    if (userOpt.isPresent()) { userEmail = userOpt.get().getEmail(); userName = userOpt.get().getName() + " " + userOpt.get().getFirstLastName(); }
                    printHistory.add(LabelFullDetailDTO.PrintEventDTO.builder()
                            .printedAt(print.getPrintedAt()).printedByUserId(print.getPrintedBy())
                            .printedByEmail(userEmail).printedByNombre(userName).isExtraordinary(i > 0)
                            .description(i == 0 ? "Primera impresión" : "Reimpresión #" + i).build());
                } catch (Exception e) { log.warn("No se pudo obtener info de impresión: {}", e.getMessage()); }
            }
        }

        builder.impreso(impreso).primeraImpresionAt(primeraImpresion).primeraImpresionPorUserId(primeraImpresionUserId)
               .ultimaReimpresionAt(ultimaReimpresion).ultimaReimpresionPorUserId(ultimaReimpresionUserId)
               .totalReimpresiones(totalReimpresiones).printHistory(printHistory);

        if (primeraImpresionUserId != null) {
            try { userRepository.findById(primeraImpresionUserId).ifPresent(u -> builder.primeraImpresionPorEmail(u.getEmail())); }
            catch (Exception e) { log.warn("No se pudo obtener info del usuario de impresión: {}", e.getMessage()); }
        }
        if (ultimaReimpresionUserId != null) {
            try { userRepository.findById(ultimaReimpresionUserId).ifPresent(u -> builder.ultimaReimpresionPorEmail(u.getEmail())); }
            catch (Exception e) { log.warn("No se pudo obtener info del usuario de reimpresión: {}", e.getMessage()); }
        }

        // Cancelación
        Optional<LabelCancelled> cancelledOpt = persistence.findCancelledByFolio(folio);
        if (cancelledOpt.isPresent()) {
            LabelCancelled cancelled = cancelledOpt.get();
            builder.cancelado(true).canceladoAt(cancelled.getCanceladoAt()).canceladoPorUserId(cancelled.getCanceladoBy())
                   .motivoCancelacion(cancelled.getMotivoCancelacion())
                   .existenciasAlCancelar(BigDecimal.valueOf(cancelled.getExistenciasAlCancelar() != null ? cancelled.getExistenciasAlCancelar() : 0))
                   .existenciasActualesAlCancelar(BigDecimal.valueOf(cancelled.getExistenciasActuales() != null ? cancelled.getExistenciasActuales() : 0))
                   .reactivado(cancelled.getReactivado()).reactivadoAt(cancelled.getReactivadoAt())
                   .reactivadoPorUserId(cancelled.getReactivadoBy()).notas(cancelled.getNotas());
            try { userRepository.findById(cancelled.getCanceladoBy()).ifPresent(u -> builder.canceladoPorEmail(u.getEmail())); }
            catch (Exception e) { log.warn("No se pudo obtener info del usuario que canceló: {}", e.getMessage()); }
            if (cancelled.getReactivado() && cancelled.getReactivadoBy() != null) {
                try { userRepository.findById(cancelled.getReactivadoBy()).ifPresent(u -> builder.reactivadoPorEmail(u.getEmail())); }
                catch (Exception e) { log.warn("No se pudo obtener info del usuario que reactivó: {}", e.getMessage()); }
            }
        } else {
            builder.cancelado(false);
        }

        // Solicitud de folios
        if (label.getLabelRequestId() != null) {
            try {
                labelRequestRepository.findById(label.getLabelRequestId()).ifPresent(req ->
                    builder.labelRequestId(req.getIdLabelRequest()).foliosSolicitados(req.getRequestedLabels()).folioSolicitadoAt(req.getCreatedAt()));
            } catch (Exception e) { log.warn("No se pudo obtener info de solicitud de folios: {}", e.getMessage()); }
        }

        // Resumen
        List<String> warnings = new ArrayList<>();
        String resumen, proximoAccion;
        if (cancelledOpt.isPresent() && !cancelledOpt.get().getReactivado()) {
            resumen = "MARBETE CANCELADO"; warnings.add("Este marbete está en estado CANCELADO");
            proximoAccion = "Revisar razón de cancelación: " + cancelledOpt.get().getMotivoCancelacion();
        } else if (label.getEstado() == Label.State.IMPRESO && !impreso) {
            resumen = "ERROR: Estado IMPRESO pero sin registros de impresión";
            warnings.add("Inconsistencia en base de datos detectada"); proximoAccion = "Contactar administrador";
        } else if (label.getEstado() == Label.State.IMPRESO && (c1 == null || c2 == null)) {
            resumen = "IMPRESO - CONTEO PENDIENTE";
            if (c1 == null) warnings.add("Conteo C1 no registrado");
            if (c2 == null) warnings.add("Conteo C2 no registrado");
            proximoAccion = "Proceder con registros de conteo";
        } else if (label.getEstado() == Label.State.IMPRESO && c1 != null && c2 != null) {
            resumen = "CONTEO COMPLETO";
            if (c1.equals(c2)) warnings.add("Conteos C1 y C2 coinciden (sin diferencia)");
            else if (Math.abs(c2.subtract(c1).doubleValue()) > c1.doubleValue() * 0.1) warnings.add("Diferencia significativa detectada (>10%)");
            proximoAccion = "Marbete completamente procesado - disponible para reportes";
        } else {
            resumen = label.getEstado().name(); proximoAccion = "Pendiente de impresión";
        }
        builder.resumenEstado(resumen).proximoAccion(proximoAccion).warnings(warnings);

        log.info("✅ Información completa obtenida para folio {}", folio);
        return builder.build();
    }

    @Transactional(readOnly = true)
    public Page<LabelFullDetailDTO> getLabelFullDetailList(LabelListFilterDTO filter, Long userId, String userRole) {
        log.info("📊 Obteniendo lista completa de marbetes - usuario={}, periodo={}, almacen={}", userId, filter.getPeriodId(), filter.getWarehouseId());

        Long periodId = filter.getPeriodId();
        Long warehouseId = filter.getWarehouseId();

        List<Label> allLabels;
        if (periodId != null && warehouseId != null) {
            allLabels = jpaLabelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId);
        } else if (periodId != null) {
            allLabels = jpaLabelRepository.findByPeriodId(periodId);
        } else {
            allLabels = jpaLabelRepository.findAll();
        }

        List<LabelFullDetailDTO> results = new ArrayList<>();
        for (Label label : allLabels) {
            try {
                warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);
                LabelFullDetailDTO fullDetail = buildLabelFullDetailDTO(label);
                if (shouldIncludeLabel(fullDetail, filter)) results.add(fullDetail);
            } catch (Exception e) {
                log.warn("No se pudo procesar marbete folio {}: {}", label.getFolio(), e.getMessage());
            }
        }

        if (filter.getSearchText() != null && !filter.getSearchText().isBlank()) {
            String searchLower = filter.getSearchText().toLowerCase();
            results = results.stream().filter(l -> matchesSearch(l, searchLower))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        Comparator<LabelFullDetailDTO> comparator = getComparatorForLabels(filter.getSortBy());
        if ("DESC".equalsIgnoreCase(filter.getSortDirection())) comparator = comparator.reversed();
        results.sort(comparator);

        int page = filter.getPage(), size = filter.getSize();
        int start = page * size, end = Math.min(start + size, results.size());
        List<LabelFullDetailDTO> pageContent = start < results.size() ? results.subList(start, end) : new ArrayList<>();

        log.info("✅ Lista completa: {} marbetes, página {}/{}", results.size(), page + 1, (results.size() + size - 1) / size);
        return new PageImpl<>(pageContent, PageRequest.of(page, size), results.size());
    }

    @Transactional(readOnly = true)
    public List<LabelDetailForPrintDTO> getSelectedLabelsInfo(List<Long> folios, Long periodId, Long warehouseId, Long userId, String userRole) {
        log.info("📋 Consultando {} marbetes seleccionados - usuario={}, warehouseId={}", folios.size(), userId, warehouseId);
        if (folios == null || folios.isEmpty()) throw new IllegalArgumentException("Debe proporcionar al menos un folio");
        if (folios.size() > 500) throw new IllegalArgumentException("Máximo 500 marbetes por consulta");
        if (warehouseId != null) warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

        List<LabelDetailForPrintDTO> results = new ArrayList<>();
        for (int i = 0; i < folios.size(); i++) {
            Long folio = folios.get(i);
            try {
                Label label = jpaLabelRepository.findById(folio)
                        .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado: " + folio));
                if (!label.getPeriodId().equals(periodId)) { log.warn("Folio {} no pertenece al período {}", folio, periodId); continue; }
                if (warehouseId != null && !label.getWarehouseId().equals(warehouseId)) { log.warn("Folio {} no pertenece al almacén {}", folio, warehouseId); continue; }

                Long effectiveWarehouseId = warehouseId != null ? warehouseId : label.getWarehouseId();
                LabelDetailForPrintDTO.LabelDetailForPrintDTOBuilder builder = LabelDetailForPrintDTO.builder()
                        .folio(folio).estado(label.getEstado() != null ? label.getEstado().name() : null)
                        .periodId(periodId).warehouseId(effectiveWarehouseId);

                try { productRepository.findById(label.getProductId()).ifPresent(p ->
                        builder.productId(p.getIdProduct()).claveProducto(p.getCveArt()).nombreProducto(p.getDescr()).unidadMedida(p.getUniMed())); }
                catch (Exception e) { log.debug("No se pudo obtener producto para folio {}", folio); }

                try { warehouseRepository.findById(effectiveWarehouseId).ifPresent(w ->
                        builder.claveAlmacen(w.getWarehouseKey()).nombreAlmacen(w.getNameWarehouse())); }
                catch (Exception e) { log.debug("No se pudo obtener almacén para folio {}", folio); }

                try { jpaPeriodRepository.findById(periodId).ifPresent(p -> builder.periodDate(p.getDate())); }
                catch (Exception e) { log.debug("No se pudo obtener período para folio {}", folio); }

                try { inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(label.getProductId(), effectiveWarehouseId, periodId)
                        .ifPresent(s -> builder.existenciasTeoricas(s.getExistQty())); }
                catch (Exception e) { log.debug("No se pudo obtener existencias para folio {}", folio); }

                List<LabelCountEvent> countEvents = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(folio);
                BigDecimal c1 = null, c2 = null;
                for (LabelCountEvent event : countEvents) {
                    if (event.getCountNumber() == 1) c1 = event.getCountedValue();
                    if (event.getCountNumber() == 2) c2 = event.getCountedValue();
                }
                builder.conteo1Valor(c1).conteo2Valor(c2);
                if (c1 != null && c2 != null) builder.diferencia(c2.subtract(c1)).statusConteo("COMPLETO");
                else if (c1 != null) builder.statusConteo("PENDIENTE C2");
                else if (c2 != null) builder.statusConteo("PENDIENTE C1");
                else builder.statusConteo("PENDIENTE");

                try { userRepository.findById(label.getCreatedBy()).ifPresent(u ->
                        builder.createdByEmail(u.getEmail()).createdByFullName(u.getName() + " " + u.getFirstLastName())); }
                catch (Exception e) { log.debug("No se pudo obtener usuario creador para folio {}", folio); }

                builder.resumenEstado(label.getEstado() != null ? label.getEstado().name() : null)
                       .mensaje(String.format("Marbete %d de %d", i + 1, folios.size()));
                results.add(builder.build());
            } catch (Exception e) { log.warn("Error procesando folio {}: {}", folio, e.getMessage()); }
        }
        log.info("✅ Se consultaron {} marbetes", results.size());
        return results;
    }

    private LabelFullDetailDTO buildLabelFullDetailDTO(Label label) {
        LabelFullDetailDTO.LabelFullDetailDTOBuilder builder = LabelFullDetailDTO.builder()
                .folio(label.getFolio()).estado(label.getEstado() != null ? label.getEstado().name() : "DESCONOCIDO")
                .createdAt(label.getCreatedAt()).impresoAt(label.getImpresoAt()).createdByUserId(label.getCreatedBy());

        try { userRepository.findById(label.getCreatedBy()).ifPresent(u ->
                builder.createdByEmail(u.getEmail()).createdByFullName(u.getName() + " " + u.getFirstLastName())
                        .createdByRole(u.getRole() != null ? u.getRole().name() : null)); }
        catch (Exception e) { log.debug("No se pudo obtener info usuario creador folio {}", label.getFolio()); }

        try { productRepository.findById(label.getProductId()).ifPresent(p ->
                builder.productId(p.getIdProduct()).claveProducto(p.getCveArt()).nombreProducto(p.getDescr())
                        .unidadMedida(p.getUniMed()).descripcionProducto(p.getDescr())); }
        catch (Exception e) { log.debug("No se pudo obtener info producto folio {}", label.getFolio()); }

        try { warehouseRepository.findById(label.getWarehouseId()).ifPresent(w ->
                builder.warehouseId(w.getIdWarehouse()).claveAlmacen(w.getWarehouseKey()).nombreAlmacen(w.getNameWarehouse())); }
        catch (Exception e) { log.debug("No se pudo obtener info almacén folio {}", label.getFolio()); }

        builder.periodId(label.getPeriodId());
        try { jpaPeriodRepository.findById(label.getPeriodId()).ifPresent(p -> builder.periodDate(p.getDate())); }
        catch (Exception e) { log.debug("No se pudo obtener info período folio {}", label.getFolio()); }

        try { inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
                label.getProductId(), label.getWarehouseId(), label.getPeriodId())
                .ifPresent(s -> builder.existenciasTeoricas(s.getExistQty())); }
        catch (Exception e) { log.debug("No se pudo obtener existencias folio {}", label.getFolio()); }

        List<LabelCountEvent> countEvents = jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(label.getFolio());
        BigDecimal c1 = null, c2 = null;
        for (LabelCountEvent event : countEvents) {
            if (event.getCountNumber() == 1) c1 = event.getCountedValue();
            if (event.getCountNumber() == 2) c2 = event.getCountedValue();
        }
        builder.conteo1Valor(c1).conteo2Valor(c2);
        if (c1 != null && c2 != null) builder.diferencia(c2.subtract(c1)).statusConteo("COMPLETO");
        else if (c1 != null) builder.statusConteo("PENDIENTE C2");
        else if (c2 != null) builder.statusConteo("PENDIENTE C1");
        else builder.statusConteo("PENDIENTE");

        List<LabelPrint> prints = persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), label.getPeriodId(), label.getWarehouseId());
        List<LabelFullDetailDTO.PrintEventDTO> printHistory = new ArrayList<>();
        Boolean impreso = !prints.isEmpty();
        LocalDateTime primeraImpresion = null; Long primeraImpresionUserId = null;
        LocalDateTime ultimaReimpresion = null; Long ultimaReimpresionUserId = null;
        int totalReimpresiones = 0;

        if (!prints.isEmpty()) {
            primeraImpresion = prints.getFirst().getPrintedAt(); primeraImpresionUserId = prints.getFirst().getPrintedBy();
            ultimaReimpresion = prints.getLast().getPrintedAt(); ultimaReimpresionUserId = prints.getLast().getPrintedBy();
            totalReimpresiones = prints.size() - 1;
            for (int i = 0; i < prints.size(); i++) {
                LabelPrint print = prints.get(i);
                try {
                    String userEmail = "DESCONOCIDO", userName = "DESCONOCIDO";
                    var userOpt = userRepository.findById(print.getPrintedBy());
                    if (userOpt.isPresent()) { userEmail = userOpt.get().getEmail(); userName = userOpt.get().getName() + " " + userOpt.get().getFirstLastName(); }
                    printHistory.add(LabelFullDetailDTO.PrintEventDTO.builder()
                            .printedAt(print.getPrintedAt()).printedByUserId(print.getPrintedBy())
                            .printedByEmail(userEmail).printedByNombre(userName).isExtraordinary(i > 0)
                            .description(i == 0 ? "Primera impresión" : "Reimpresión #" + i).build());
                } catch (Exception e) { log.debug("No se pudo obtener info impresion folio {}", label.getFolio()); }
            }
        }

        builder.impreso(impreso).primeraImpresionAt(primeraImpresion).primeraImpresionPorUserId(primeraImpresionUserId)
               .ultimaReimpresionAt(ultimaReimpresion).ultimaReimpresionPorUserId(ultimaReimpresionUserId)
               .totalReimpresiones(totalReimpresiones > 0 ? totalReimpresiones : null)
               .printHistory(printHistory.isEmpty() ? null : printHistory);

        if (primeraImpresionUserId != null) {
            try { userRepository.findById(primeraImpresionUserId).ifPresent(u -> builder.primeraImpresionPorEmail(u.getEmail())); }
            catch (Exception e) { log.debug("No se pudo obtener usuario impresion folio {}", label.getFolio()); }
        }
        if (ultimaReimpresionUserId != null) {
            try { userRepository.findById(ultimaReimpresionUserId).ifPresent(u -> builder.ultimaReimpresionPorEmail(u.getEmail())); }
            catch (Exception e) { log.debug("No se pudo obtener usuario reimpresion folio {}", label.getFolio()); }
        }

        Optional<LabelCancelled> cancelledOpt = persistence.findCancelledByFolio(label.getFolio());
        if (cancelledOpt.isPresent()) {
            LabelCancelled cancelled = cancelledOpt.get();
            builder.cancelado(true).canceladoAt(cancelled.getCanceladoAt()).canceladoPorUserId(cancelled.getCanceladoBy())
                   .motivoCancelacion(cancelled.getMotivoCancelacion())
                   .existenciasAlCancelar(BigDecimal.valueOf(cancelled.getExistenciasAlCancelar() != null ? cancelled.getExistenciasAlCancelar() : 0))
                   .existenciasActualesAlCancelar(BigDecimal.valueOf(cancelled.getExistenciasActuales() != null ? cancelled.getExistenciasActuales() : 0))
                   .reactivado(cancelled.getReactivado()).reactivadoAt(cancelled.getReactivadoAt())
                   .reactivadoPorUserId(cancelled.getReactivadoBy()).notas(cancelled.getNotas());
            try { userRepository.findById(cancelled.getCanceladoBy()).ifPresent(u -> builder.canceladoPorEmail(u.getEmail())); }
            catch (Exception e) { log.debug("No se pudo obtener usuario cancelacion folio {}", label.getFolio()); }
            if (cancelled.getReactivado() && cancelled.getReactivadoBy() != null) {
                try { userRepository.findById(cancelled.getReactivadoBy()).ifPresent(u -> builder.reactivadoPorEmail(u.getEmail())); }
                catch (Exception e) { log.debug("No se pudo obtener usuario reactivacion folio {}", label.getFolio()); }
            }
        } else {
            builder.cancelado(false);
        }

        if (label.getLabelRequestId() != null) {
            try { labelRequestRepository.findById(label.getLabelRequestId()).ifPresent(req ->
                    builder.labelRequestId(req.getIdLabelRequest()).foliosSolicitados(req.getRequestedLabels()).folioSolicitadoAt(req.getCreatedAt())); }
            catch (Exception e) { log.debug("No se pudo obtener solicitud folios folio {}", label.getFolio()); }
        }

        builder.resumenEstado(label.getEstado() != null ? label.getEstado().name() : null);
        return builder.build();
    }

    private boolean shouldIncludeLabel(LabelFullDetailDTO label, LabelListFilterDTO filter) {
        if (filter.getEstado() != null && !filter.getEstado().equals(label.getEstado())) return false;
        if (filter.getImpreso() != null && filter.getImpreso() != label.getImpreso()) return false;
        if (filter.getConteoCompleto() != null && filter.getConteoCompleto() != label.getConteoCompleto()) return false;
        if (filter.getCancelado() != null && filter.getCancelado() != label.getCancelado()) return false;
        if (filter.getProductId() != null && !filter.getProductId().equals(label.getProductId())) return false;
        return true;
    }

    private boolean matchesSearch(LabelFullDetailDTO label, String searchLower) {
        if (label.getFolio() != null && String.valueOf(label.getFolio()).contains(searchLower)) return true;
        if (label.getClaveProducto() != null && label.getClaveProducto().toLowerCase().contains(searchLower)) return true;
        if (label.getNombreProducto() != null && label.getNombreProducto().toLowerCase().contains(searchLower)) return true;
        if (label.getClaveAlmacen() != null && label.getClaveAlmacen().toLowerCase().contains(searchLower)) return true;
        if (label.getNombreAlmacen() != null && label.getNombreAlmacen().toLowerCase().contains(searchLower)) return true;
        return false;
    }

    private Comparator<LabelFullDetailDTO> getComparatorForLabels(String sortBy) {
        if (sortBy == null) sortBy = "folio";
        return switch (sortBy.toLowerCase()) {
            case "createdat" -> Comparator.comparing(LabelFullDetailDTO::getCreatedAt);
            case "estado" -> Comparator.comparing(LabelFullDetailDTO::getEstado);
            case "producto", "nombreproducto" -> Comparator.comparing(LabelFullDetailDTO::getNombreProducto, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "almacen", "nombrealmacen" -> Comparator.comparing(LabelFullDetailDTO::getNombreAlmacen, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(LabelFullDetailDTO::getFolio);
        };
    }
}
