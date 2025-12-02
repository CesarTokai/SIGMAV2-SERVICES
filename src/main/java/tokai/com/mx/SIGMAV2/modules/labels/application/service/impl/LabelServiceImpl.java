package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.entity.InventoryStockEntity;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.PrintRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryResponseDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.JasperLabelPrintService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelGenerationBatch;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.DuplicateCountException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.CountSequenceException;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelServiceImpl implements LabelService {

    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;
    private final JpaLabelRequestRepository labelRequestRepository;
    private final JasperLabelPrintService jasperLabelPrintService;

    @Override
    @Transactional
    public void requestLabels(LabelRequestDTO dto, Long userId, String userRole) {
        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        // REGLA DE NEGOCIO: Validar que la cantidad sea numérica entera (ya validado por DTO)
        // El DTO debe tener validación @Min(0) o similar

        // Buscar si ya existe una solicitud para este producto/almacén/periodo
        Optional<LabelRequest> existingRequest = persistence.findByProductWarehousePeriod(
            dto.getProductId(),
            dto.getWarehouseId(),
            dto.getPeriodId()
        );

        // REGLA DE NEGOCIO: Si la cantidad es 0, significa que ya no desea generar folios
        if (dto.getRequestedLabels() == 0) {
            if (existingRequest.isPresent()) {
                LabelRequest req = existingRequest.get();

                // Solo permitir eliminar/cancelar si NO se han generado folios aún
                if (req.getFoliosGenerados() == 0) {
                    persistence.delete(req);
                    log.info("Solicitud cancelada (cantidad=0) para producto {} en almacén {} periodo {}",
                        dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
                } else {
                    throw new InvalidLabelStateException(
                        "No se puede cancelar la solicitud porque ya se generaron " +
                        req.getFoliosGenerados() + " folios. Debe imprimirlos primero.");
                }
            }
            // Si no existe solicitud y la cantidad es 0, no hacer nada
            return;
        }

        // REGLA DE NEGOCIO: No permitir solicitar si existen marbetes GENERADOS sin imprimir
        // Esta validación solo aplica para solicitudes NUEVAS o al INCREMENTAR la cantidad
        if (existingRequest.isPresent()) {
            LabelRequest existing = existingRequest.get();

            // Si ya se generaron folios, verificar que no haya sin imprimir
            if (existing.getFoliosGenerados() > 0) {
                boolean hasUnprinted = persistence.existsGeneratedUnprintedForProductWarehousePeriod(
                    dto.getProductId(),
                    dto.getWarehouseId(),
                    dto.getPeriodId()
                );
                if (hasUnprinted) {
                    throw new InvalidLabelStateException(
                        "Existen marbetes GENERADOS sin imprimir para este producto/almacén/periodo. " +
                        "Por favor imprima los marbetes existentes antes de solicitar más.");
                }
            }

            // REGLA DE NEGOCIO: Mientras no haya ejecutado "Generar marbetes",
            // podrá cambiar la cantidad las veces que desee
            log.info("Actualizando solicitud existente de {} a {} folios para producto {} en almacén {} periodo {}",
                existing.getRequestedLabels(), dto.getRequestedLabels(),
                dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

            existing.setRequestedLabels(dto.getRequestedLabels());
            persistence.save(existing);

        } else {
            // CREAR nueva solicitud
            log.info("Creando nueva solicitud de {} folios para producto {} en almacén {} periodo {}",
                dto.getRequestedLabels(), dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());

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

        // REGLA DE NEGOCIO CUMPLIDA: Los datos se guardan automáticamente en BD,
        // puede cambiar de módulo sin temor a perder el dato
    }

    @Override
    @Transactional
    public void generateBatch(GenerateBatchDTO dto, Long userId, String userRole) {
        // Validar acceso al almacén
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        // Buscar solicitud existente
        Optional<LabelRequest> opt = persistence.findByProductWarehousePeriod(dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
        if (opt.isEmpty()) {
            throw new LabelNotFoundException("No existe una solicitud para el producto/almacén/periodo.");
        }
        LabelRequest req = opt.get();
        int remaining = req.getRequestedLabels() - req.getFoliosGenerados();
        if (remaining <= 0) {
            throw new InvalidLabelStateException("No hay folios solicitados para generar.");
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
    public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
        log.info("Iniciando impresión de marbetes: periodId={}, warehouseId={}, startFolio={}, endFolio={}, userId={}, userRole={}",
            dto.getPeriodId(), dto.getWarehouseId(), dto.getStartFolio(), dto.getEndFolio(), userId, userRole);

        // REGLA DE NEGOCIO: Esta operación delimita el contexto según usuario y almacén asignado,
        // PERO si el usuario tiene rol "ADMINISTRADOR" o "AUXILIAR", puede cambiar de almacén
        if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
            log.info("Usuario {} tiene rol {} - puede imprimir en cualquier almacén", userId, userRole);
            // Los administradores y auxiliares pueden imprimir en cualquier almacén sin validación restrictiva
        } else {
            // Para otros roles, validar acceso estricto al almacén
            warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        }

        // REGLA DE NEGOCIO: Se podrán imprimir marbetes siempre y cuando se hayan importado datos
        // de los catálogos de inventario y multialmacén
        boolean hasInventoryData = inventoryStockRepository.existsByWarehouseIdWarehouseAndPeriodId(
            dto.getWarehouseId(), dto.getPeriodId());

        if (!hasInventoryData) {
            throw new tokai.com.mx.SIGMAV2.modules.labels.application.exception.CatalogNotLoadedException(
                "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario " +
                "y multialmacén para el periodo y almacén seleccionados. " +
                "Por favor, importe los datos antes de continuar.");
        }

        // REGLA DE NEGOCIO: Validar que el rango de folios sea válido
        if (dto.getStartFolio() > dto.getEndFolio()) {
            throw new InvalidLabelStateException(
                "El folio inicial no puede ser mayor que el folio final.");
        }

        // REGLA DE NEGOCIO: Verificar que los folios solicitados existan y estén generados
        long foliosCount = dto.getEndFolio() - dto.getStartFolio() + 1;
        log.info("Intentando imprimir {} folio(s) desde {} hasta {}",
            foliosCount, dto.getStartFolio(), dto.getEndFolio());

        // Obtener los marbetes del rango especificado
        List<Label> labelsToProcess = persistence.findByFolioRange(
            dto.getPeriodId(),
            dto.getWarehouseId(),
            dto.getStartFolio(),
            dto.getEndFolio()
        );

        if (labelsToProcess.isEmpty()) {
            throw new InvalidLabelStateException(
                "No se encontraron marbetes en el rango especificado");
        }

        // Validar que no haya marbetes CANCELADOS
        long cancelledCount = labelsToProcess.stream()
            .filter(l -> l.getEstado() == Label.State.CANCELADO)
            .count();

        if (cancelledCount > 0) {
            throw new InvalidLabelStateException(
                String.format("No se pueden imprimir los marbetes: %d folios están CANCELADOS", cancelledCount));
        }

        // REGLA DE NEGOCIO: Soportar dos escenarios de impresión:
        // 1. Impresión normal: Impresión inmediata de marbetes recién generados (GENERADOS)
        // 2. Impresión extraordinaria: Reimpresión de marbetes previamente impresos (IMPRESOS)

        try {
            // Marcar como impresos y registrar en la tabla label_print
            LabelPrint result = persistence.printLabelsRange(
                dto.getPeriodId(),
                dto.getWarehouseId(),
                dto.getStartFolio(),
                dto.getEndFolio(),
                userId
            );

            log.info("Impresión registrada exitosamente: {} folio(s) del {} al {}",
                result.getCantidadImpresa(), result.getFolioInicial(), result.getFolioFinal());

            // Generar el PDF con JasperReports
            log.info("Generando PDF con {} marbetes...", labelsToProcess.size());
            byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labelsToProcess);

            log.info("PDF generado exitosamente: {} KB", pdfBytes.length / 1024);
            return pdfBytes;

        } catch (IllegalArgumentException e) {
            log.error("Error de validación en impresión: {}", e.getMessage());
            throw new InvalidLabelStateException(e.getMessage());
        } catch (IllegalStateException e) {
            log.error("Error de estado en impresión: {}", e.getMessage());
            throw new InvalidLabelStateException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole) {
        if (userRole == null) {
            throw new PermissionDeniedException("Role de usuario requerido para registrar C1");
        }
        String roleUpper = userRole.toUpperCase();
        boolean allowed = roleUpper.equals("ADMINISTRADOR") || roleUpper.equals("ALMACENISTA") || roleUpper.equals("AUXILIAR") || roleUpper.equals("AUXILIAR_DE_CONTEO");
        if (!allowed) {
            throw new PermissionDeniedException("No tiene permiso para registrar C1");
        }

        // Verificar que el marbete exista
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException("El folio no existe");
        }
        Label label = optLabel.get();

        // Validar acceso al almacén del marbete
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete no está IMPRESO.");
        }

        // No permitir registrar C1 si ya existe C1
        if (persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new DuplicateCountException("El conteo C1 ya fue registrado para este folio.");
        }
        // No permitir registrar C1 si ya existe C2 (secuencia rota)
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new CountSequenceException("No se puede registrar C1 porque ya existe un conteo C2 para este folio.");
        }

        LabelCountEvent.Role roleEnum;
        try { roleEnum = LabelCountEvent.Role.valueOf(roleUpper); } catch (Exception ex) { roleEnum = LabelCountEvent.Role.AUXILIAR; }

        return persistence.saveCountEvent(dto.getFolio(), userId, 1, dto.getCountedValue(), roleEnum, false);
    }

    @Override
    @Transactional
    public LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole) {
        if (userRole == null) {
            throw new PermissionDeniedException("Role de usuario requerido para registrar C2");
        }
        String roleUpper = userRole.toUpperCase();
        if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
            throw new PermissionDeniedException("No tiene permiso para registrar C2");
        }

        // Verificar que el marbete exista
        Optional<Label> optLabel = persistence.findByFolio(dto.getFolio());
        if (optLabel.isEmpty()) {
            throw new LabelNotFoundException("El folio no existe");
        }
        Label label = optLabel.get();

        // Validar acceso al almacén del marbete
        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        if (label.getEstado() == Label.State.CANCELADO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete está CANCELADO.");
        }
        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException("No se puede registrar conteo: el marbete no está IMPRESO.");
        }

        // Debe existir C1 antes de C2
        if (!persistence.hasCountNumber(dto.getFolio(), 1)) {
            throw new CountSequenceException("No se puede registrar C2 porque no existe un conteo C1 previo.");
        }

        // No permitir duplicar C2
        if (persistence.hasCountNumber(dto.getFolio(), 2)) {
            throw new DuplicateCountException("El conteo C2 ya fue registrado para este folio.");
        }

        LabelCountEvent.Role roleEnum;
        try { roleEnum = LabelCountEvent.Role.valueOf(roleUpper); } catch (Exception ex) { roleEnum = LabelCountEvent.Role.AUXILIAR_DE_CONTEO; }

        return persistence.saveCountEvent(dto.getFolio(), userId, 2, dto.getCountedValue(), roleEnum, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole) {
        log.info("getLabelSummary - Inicio: periodId={}, warehouseId={}, page={}, size={}, searchText={}, sortBy={}, sortDirection={}, userId={}, userRole={}",
            dto.getPeriodId(), dto.getWarehouseId(), dto.getPage(), dto.getSize(),
            dto.getSearchText(), dto.getSortBy(), dto.getSortDirection(), userId, userRole);

        // Si no se especifica periodo, obtener el último creado
        final Long periodId;
        if (dto.getPeriodId() == null) {
            periodId = persistence.findLastCreatedPeriodId()
                .orElseThrow(() -> new RuntimeException("No hay periodos registrados"));
            log.info("Usando periodo por default (último creado): {}", periodId);
        } else {
            periodId = dto.getPeriodId();
        }

        // Si no se especifica almacén, obtener el primero
        final Long warehouseId;
        if (dto.getWarehouseId() == null) {
            warehouseId = warehouseRepository.findFirstByOrderByIdWarehouseAsc()
                .map(WarehouseEntity::getIdWarehouse)
                .orElseThrow(() -> new RuntimeException("No hay almacenes registrados"));
            log.info("Usando almacén por default (primero): {}", warehouseId);
        } else {
            warehouseId = dto.getWarehouseId();
        }

        try {
            // Validar acceso al almacén (solo si no es ADMINISTRADOR o AUXILIAR)
            log.info("Validando acceso al almacén...");
            warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
            log.info("Acceso validado correctamente");
        } catch (Exception e) {
            log.warn("Error en validateWarehouseAccess: {}", e.getMessage());
            // Si falla la validación pero es ADMINISTRADOR o AUXILIAR, permitir acceso
            if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
                log.info("Usuario es ADMINISTRADOR o AUXILIAR, permitiendo acceso");
            } else {
                log.error("Usuario sin acceso al almacén", e);
                throw e; // Re-lanzar la excepción si no tiene permisos
            }
        }

        // Obtener información del almacén
        WarehouseEntity warehouseEntity = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Almacén no encontrado: " + warehouseId));

        String claveAlmacen = warehouseEntity.getWarehouseKey();
        String nombreAlmacen = warehouseEntity.getNameWarehouse();

        log.info("Almacén encontrado: {} - {}", claveAlmacen, nombreAlmacen);

        // Obtener todas las solicitudes de marbetes para este periodo y almacén
        List<LabelRequest> allRequests = labelRequestRepository.findAll();
        Map<Long, LabelRequest> requestsByProduct = allRequests.stream()
                .filter(req -> req.getPeriodId() != null && req.getPeriodId().equals(periodId) &&
                              req.getWarehouseId() != null && req.getWarehouseId().equals(warehouseId))
                .collect(Collectors.toMap(
                    LabelRequest::getProductId,
                    req -> req,
                    (existing, replacement) -> existing
                ));

        log.info("Encontradas {} solicitudes de productos", requestsByProduct.size());

        // Obtener todos los marbetes generados para este periodo y almacén
        List<Label> labels = persistence.findByPeriodIdAndWarehouseId(periodId, warehouseId, 0, 100000);

        // Agrupar marbetes por producto y contar
        Map<Long, Long> generatedLabelsByProduct = labels.stream()
                .collect(Collectors.groupingBy(Label::getProductId, Collectors.counting()));

        log.info("Encontrados {} marbetes generados para {} productos", labels.size(), generatedLabelsByProduct.size());

        // IMPORTANTE: Obtener TODOS los productos del inventario que tienen existencias en este almacén y periodo
        // Esto asegura que se muestren todos los productos aunque no tengan solicitudes de marbetes
        List<InventoryStockEntity> allStockInWarehouse = inventoryStockRepository
                .findByWarehouseIdWarehouseAndPeriodId(warehouseId, periodId);

        log.info("Encontrados {} productos en el inventario del almacén {}", allStockInWarehouse.size(), warehouseId);

        // Construir la lista completa de productos (inventario + solicitudes + marbetes)
        Set<Long> allProductIds = new HashSet<>();

        // Agregar todos los productos del inventario del almacén
        allStockInWarehouse.stream()
                .filter(stock -> stock.getProduct() != null)
                .forEach(stock -> allProductIds.add(stock.getProduct().getIdProduct()));

        // Agregar productos con solicitudes
        allProductIds.addAll(requestsByProduct.keySet());

        // Agregar productos con marbetes generados
        allProductIds.addAll(generatedLabelsByProduct.keySet());

        log.info("Total de productos únicos a mostrar: {}", allProductIds.size());

        List<LabelSummaryResponseDTO> allResults = new ArrayList<>();

        for (Long productId : allProductIds) {
            try {
                // Obtener información del producto
                ProductEntity product = productRepository.findById(productId).orElse(null);
                if (product == null) {
                    log.warn("Producto no encontrado: {}", productId);
                    continue;
                }

                // Obtener la solicitud de marbetes para este producto
                LabelRequest request = requestsByProduct.get(productId);
                int foliosSolicitados = request != null ? request.getRequestedLabels() : 0;
                int foliosExistentes = generatedLabelsByProduct.getOrDefault(productId, 0L).intValue();

                // Obtener existencias del inventario (ahora incluyendo periodo)
                int existencias = 0;
                String estado = "SIN_STOCK";
                try {
                    InventoryStockEntity stock = inventoryStockRepository
                            .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(productId, warehouseId, periodId)
                            .orElse(null);
                    if (stock != null) {
                        existencias = stock.getExistQty() != null ? stock.getExistQty().intValue() : 0;
                        estado = stock.getStatus() != null ? stock.getStatus().name() : "A";
                    }
                } catch (Exception e) {
                    log.warn("No se pudieron obtener existencias para producto {}: {}", productId, e.getMessage());
                }

                // Construir el DTO de respuesta
                LabelSummaryResponseDTO summary = LabelSummaryResponseDTO.builder()
                        .productId(productId)
                        .claveProducto(product.getCveArt())
                        .nombreProducto(product.getDescr())
                        .claveAlmacen(claveAlmacen)
                        .nombreAlmacen(nombreAlmacen)
                        .foliosSolicitados(foliosSolicitados)
                        .foliosExistentes(foliosExistentes)
                        .estado(estado)
                        .existencias(existencias)
                        .build();

                allResults.add(summary);

            } catch (Exception e) {
                log.error("Error procesando producto {}: {}", productId, e.getMessage(), e);
            }
        }

        log.info("Construidos {} registros totales", allResults.size());

        // Aplicar filtro de búsqueda (case-insensitive según requerimientos)
        List<LabelSummaryResponseDTO> filteredResults = allResults;
        if (dto.getSearchText() != null && !dto.getSearchText().trim().isEmpty()) {
            String searchLower = dto.getSearchText().toLowerCase();
            filteredResults = allResults.stream()
                .filter(item -> {
                    // Búsqueda en: Clave de producto, Producto, Clave de almacén, Almacén, Estado, Existencias
                    return (item.getClaveProducto() != null && item.getClaveProducto().toLowerCase().contains(searchLower)) ||
                           (item.getNombreProducto() != null && item.getNombreProducto().toLowerCase().contains(searchLower)) ||
                           (item.getClaveAlmacen() != null && item.getClaveAlmacen().toLowerCase().contains(searchLower)) ||
                           (item.getNombreAlmacen() != null && item.getNombreAlmacen().toLowerCase().contains(searchLower)) ||
                           (item.getEstado() != null && item.getEstado().toLowerCase().contains(searchLower)) ||
                           String.valueOf(item.getExistencias()).contains(searchLower);
                })
                .collect(Collectors.toList());
            log.info("Búsqueda aplicada: '{}', resultados filtrados: {}", dto.getSearchText(), filteredResults.size());
        }

        // Aplicar ordenamiento personalizado
        Comparator<LabelSummaryResponseDTO> comparator = getComparator(dto.getSortBy());
        if ("DESC".equalsIgnoreCase(dto.getSortDirection())) {
            comparator = comparator.reversed();
        }
        filteredResults.sort(comparator);
        log.info("Ordenamiento aplicado: {} {}", dto.getSortBy(), dto.getSortDirection());

        // Aplicar paginación
        int totalFiltered = filteredResults.size();
        int start = dto.getPage() * dto.getSize();
        int end = Math.min(start + dto.getSize(), totalFiltered);

        if (start >= totalFiltered && totalFiltered > 0) {
            log.warn("Página {} fuera de rango (total: {}), devolviendo lista vacía", dto.getPage(), totalFiltered);
            return new ArrayList<>();
        }

        List<LabelSummaryResponseDTO> paginatedResults = start < totalFiltered ?
            filteredResults.subList(start, end) : new ArrayList<>();

        log.info("Paginación aplicada: página {}, tamaño {}, devolviendo {} registros de {} totales filtrados",
            dto.getPage(), dto.getSize(), paginatedResults.size(), totalFiltered);

        return paginatedResults;
    }

    /**
     * Método auxiliar para obtener el comparador según la columna seleccionada.
     * Columnas soportadas: foliosExistentes, claveProducto, producto/nombreProducto,
     * claveAlmacen, almacen/nombreAlmacen, estado, existencias
     */
    private Comparator<LabelSummaryResponseDTO> getComparator(String sortBy) {
        if (sortBy == null) sortBy = "claveProducto";

        return switch (sortBy.toLowerCase()) {
            case "foliosexistentes" -> Comparator.comparing(LabelSummaryResponseDTO::getFoliosExistentes);
            case "claveproducto" -> Comparator.comparing(LabelSummaryResponseDTO::getClaveProducto,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "producto", "nombreproducto" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreProducto,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "clavealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getClaveAlmacen,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "almacen", "nombrealmacen" -> Comparator.comparing(LabelSummaryResponseDTO::getNombreAlmacen,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "estado" -> Comparator.comparing(LabelSummaryResponseDTO::getEstado,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "existencias" -> Comparator.comparing(LabelSummaryResponseDTO::getExistencias);
            default ->
                // Por defecto, ordenar por clave de producto
                    Comparator.comparing(LabelSummaryResponseDTO::getClaveProducto,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

    @Override
    @Transactional
    public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
        // Validar acceso al almacén una sola vez
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
        for (ProductBatchDTO product : dto.getProducts()) {
            try {
                GenerateBatchDTO single = new GenerateBatchDTO();
                single.setProductId(product.getProductId());
                single.setWarehouseId(dto.getWarehouseId());
                single.setPeriodId(dto.getPeriodId());
                single.setLabelsToGenerate(product.getLabelsToGenerate());
                this.generateBatch(single, userId, userRole);
            } catch (Exception e) {
                log.error("Error generando marbetes para producto {}: {}", product.getProductId(), e.getMessage());
                // Aquí podrías recolectar errores individuales si quieres devolver un resumen
            }
        }
    }
}
