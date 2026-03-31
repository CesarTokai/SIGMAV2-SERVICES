package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseSearchDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseWizardStepDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.ProductoBajaDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.ExportResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.ImportResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.WizardStepResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.util.PeriodParserUtil;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.input.MultiWarehouseUseCase;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.output.MultiWarehouseInventoryPort;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.output.MultiWarehouseWarehousePort;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence.MultiWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence.imports.MultiWarehouseImportLogRepository;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.io.InputStream;


@Service
@RequiredArgsConstructor
public class MultiWarehouseServiceImpl implements MultiWarehouseUseCase {

    private static final Logger log = LoggerFactory.getLogger(MultiWarehouseServiceImpl.class);

    // --- Repositorios propios del módulo ---
    private final MultiWarehouseRepository multiWarehouseRepository;
    private final MultiWarehouseImportLogRepository importLogRepository;
    private final PeriodRepository periodRepository;

    // --- Ports de salida — NO repositorios JPA externos directos ---
    private final MultiWarehouseWarehousePort warehousePort;
    private final MultiWarehouseInventoryPort inventoryPort;

    /** Tamaño de batch para saveAll masivo — evita OOM con archivos grandes. */
    private static final int BATCH_SIZE = 500;

    // =========================================================================
    // CONSULTA
    // =========================================================================

    @Override
    public List<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search) {
        // Reutiliza la lógica de paginación pero trae todos los resultados
        List<MultiWarehouseExistence> result = new ArrayList<>();
        int page = 0;
        int size = 1000; // tamaño grande para evitar demasiadas iteraciones
        Page<MultiWarehouseExistence> current;
        do {
            current = multiWarehouseRepository.findExistences(search, PageRequest.of(page++, size));
            result.addAll(current.getContent());
        } while (current.hasNext());
        return result;
    }

        @Override
    public Page<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search, Pageable pageable) {
        log.debug("findExistences: periodId={}, search={}",
                  search != null ? search.getPeriodId() : null,
                  search != null ? search.getSearch() : null);

        // Configurar paginacion — tamanios permitidos: 10, 25, 50, 100
        if (search != null && search.getPageSize() != null) {
            int size = search.getPageSize();
            if (size != 10 && size != 25 && size != 50 && size != 100) size = 50;
            int page = pageable != null ? pageable.getPageNumber() : 0;
            if (search.getOrderBy() != null && !search.getOrderBy().isBlank()) {
                String sortField = mapSortField(search.getOrderBy().toLowerCase());
                if (sortField != null) {
                    boolean asc = search.getAscending() == null || search.getAscending();
                    org.springframework.data.domain.Sort sort = asc
                        ? org.springframework.data.domain.Sort.by(sortField).ascending()
                        : org.springframework.data.domain.Sort.by(sortField).descending();
                    pageable = PageRequest.of(page, size, sort);
                } else {
                    pageable = PageRequest.of(page, size);
                }
            } else {
                pageable = PageRequest.of(page, size);
            }
        } else if (pageable == null) {
            pageable = PageRequest.of(0, 50);
        }

        // Resolver periodo desde string si no llega el ID
        if (search != null && search.getPeriodId() == null
                && search.getPeriod() != null && !search.getPeriod().isBlank()) {
            LocalDate date = PeriodParserUtil.parse(search.getPeriod());
            if (date != null) {
                periodRepository.findByDate(date).ifPresent(p -> search.setPeriodId(p.getId()));
            }
        }
        return multiWarehouseRepository.findExistences(search, pageable);
    }

    private static final Map<String, String> SORT_FIELD_MAP = Map.of(
        "clave_producto", "productCode",   "producto",      "productCode",
        "descripcion",    "productName",   "almacen",       "warehouseName",
        "clave_almacen",  "warehouseName", "estado",        "status",
        "existencias",    "stock"
    );

    private String mapSortField(String orderBy) { return SORT_FIELD_MAP.get(orderBy); }

    @Override
    public Optional<BigDecimal> getStock(String productCode, String warehouseKey, Long periodId) {
        return multiWarehouseRepository
            .findByProductCodeAndWarehouseKeyAndPeriodId(productCode, warehouseKey, periodId)
            .map(MultiWarehouseExistence::getStock);
    }

    @Override
    public Optional<MultiWarehouseImportLog> getImportLog(Long id) {
        return importLogRepository.findById(id);
    }

    @Override
    public List<MultiWarehouseExistence> getProductosDadosDeBaja(Long periodId) {
        Objects.requireNonNull(periodId, "periodId es obligatorio");
        return multiWarehouseRepository.findInactiveByPeriodId(periodId);
    }

    // =========================================================================
    // IMPORTACION
    // =========================================================================

    @Override
    @Transactional
    public ImportResult importFile(MultipartFile file, String period) {
        log.info("=== IMPORT MULTIWAREHOUSE INICIADO === period='{}', file='{}'",
                 period, file != null ? file.getOriginalFilename() : "null");

        // Validaciones de entrada
        if (period == null || period.isBlank())
            throw new IllegalArgumentException("Periodo* es obligatorio");
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Archivo multialmacen.xlsx* es obligatorio");

        // Idempotencia: detectar archivo duplicado por SHA-256
        String fileHash = calculateSHA256(file);
        log.info("SHA-256 del archivo: {}", fileHash);
        String stage = "default";
        Optional<MultiWarehouseImportLog> existingLog =
            importLogRepository.findByPeriodAndStageAndFileHash(period, stage, fileHash);
        if (existingLog.isPresent()) {
            log.warn("Archivo DUPLICADO detectado. Log existente id={}", existingLog.get().getId());
            return ImportResult.builder()
                .status(ImportResult.Status.DUPLICATE)
                .importLog(existingLog.get())
                .warningMessage("El archivo ya fue importado previamente para este periodo. No se aplicaron cambios.")
                .productosDadosDeBaja(Collections.emptyList())
                .build();
        }

        // Validar formato y estado del periodo
        LocalDate periodDate = PeriodParserUtil.parse(period);
        log.info("Periodo parseado: '{}' -> {}", period, periodDate);
        if (periodDate == null)
            throw new IllegalArgumentException("Formato de periodo invalido. Use MM-yyyy o yyyy-MM. Recibido: '" + period + "'");

        Optional<Period> perOpt = periodRepository.findByDate(periodDate);
        log.info("Periodo en BD: {} (encontrado={})", periodDate, perOpt.isPresent());
        if (perOpt.isPresent()) {
            log.info("Periodo ID={}, State={}", perOpt.get().getId(), perOpt.get().getState());
            Period.PeriodState st = perOpt.get().getState();
            if (st == Period.PeriodState.CLOSED)
                throw new IllegalStateException("PERIOD_CLOSED:" + st);
            if (st == Period.PeriodState.LOCKED)
                throw new IllegalStateException("PERIOD_LOCKED:" + st);
        }

        // Guardar log de inicio en TX INDEPENDIENTE para que persista aunque la importacion falle
        MultiWarehouseImportLog savedLog = saveImportLog(
            file.getOriginalFilename(), period, fileHash, stage, "STARTED", "Importacion iniciada");

        int processed = 0, warehousesCreated = 0, productsCreated = 0,
            existingUpdated = 0, markedAsInactive = 0;
        List<ProductoBajaDTO> productosDadosDeBaja = new ArrayList<>();

        try {
            String fname = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            List<MultiWarehouseExistence> parsedData = fname.endsWith(".csv") ? parseCsv(file) : parseXlsx(file);
            log.info("Archivo parseado: {} registros desde '{}'", parsedData.size(), fname);

            Long periodId = perOpt.map(Period::getId).orElse(null);
            log.info("PeriodId para importacion: {}", periodId);
            if (periodId == null) {
                // El periodo no existe — el usuario debe crearlo primero desde Periodos
                throw new IllegalArgumentException(
                    "El periodo '" + period + "' (fecha: " + periodDate + ") no existe en el sistema. " +
                    "Por favor crea el periodo primero desde la pantalla de Periodos antes de importar.");
            }

            // RN-MWH-002 — crear almacenes faltantes via port
            long wAntes = warehousePort.countActive();
            Map<String, Long> warehouseMap = createMissingWarehouses(parsedData);
            warehousesCreated = (int)(warehousePort.countActive() - wAntes);
            log.info("Almacenes resueltos: {}, nuevos: {}", warehouseMap.size(), warehousesCreated);

            // RN-MWH-003 — crear productos faltantes via port
            long pAntes = inventoryPort.countProducts();
            Map<String, Long> productMap = createMissingProducts(parsedData);
            productsCreated = (int)(inventoryPort.countProducts() - pAntes);
            log.info("Productos resueltos: {}, nuevos: {}", productMap.size(), productsCreated);

            // Cargar existentes del periodo para comparacion
            List<MultiWarehouseExistence> existingRecords = multiWarehouseRepository.findByPeriodId(periodId);
            Map<String, MultiWarehouseExistence> existingMap = existingRecords.stream()
                .collect(Collectors.toMap(e -> e.getProductCode() + "|" + e.getWarehouseKey(), e -> e));

            // RN-MWH-004 / RN-MWH-005 — insertar/actualizar en batches
            List<MultiWarehouseExistence> toSave = new ArrayList<>();
            for (MultiWarehouseExistence nd : parsedData) {
                String key = nd.getProductCode() + "|" + nd.getWarehouseKey();
                Long wid = warehouseMap.get(nd.getWarehouseKey());
                Long pid = productMap.get(nd.getProductCode());
                nd.setWarehouseId(wid);
                nd.setPeriodId(periodId);

                if (existingMap.containsKey(key)) {
                    MultiWarehouseExistence ex = existingMap.get(key);
                    ex.setStock(nd.getStock());
                    ex.setStatus(nd.getStatus());
                    ex.setProductName(nd.getProductName());
                    ex.setWarehouseName(nd.getWarehouseName());
                    toSave.add(ex);
                    existingUpdated++;
                } else {
                    nd.setId(null);
                    toSave.add(nd);
                }

                // Sincronizar con inventory_stock via port
                inventoryPort.upsertStock(pid, wid, periodId, nd.getStock(), nd.getStatus());

                if (toSave.size() % BATCH_SIZE == 0) {
                    multiWarehouseRepository.saveAll(toSave);
                    toSave.clear();
                }
            }

            // RN-MWH-006 — marcar como BAJA los que ya no vienen en el Excel
            Set<String> excelKeys = parsedData.stream()
                .map(e -> e.getProductCode() + "|" + e.getWarehouseKey())
                .collect(Collectors.toSet());
            for (MultiWarehouseExistence ex : existingRecords) {
                String key = ex.getProductCode() + "|" + ex.getWarehouseKey();
                if (!excelKeys.contains(key) && !"B".equals(ex.getStatus())) {
                    ex.setStatus("B");
                    toSave.add(ex);
                    markedAsInactive++;
                    productosDadosDeBaja.add(ProductoBajaDTO.builder()
                        .claveProducto(ex.getProductCode()).nombreProducto(ex.getProductName())
                        .claveAlmacen(ex.getWarehouseKey()).nombreAlmacen(ex.getWarehouseName())
                        .existenciasAnteriores(ex.getStock()).build());
                    if (toSave.size() % BATCH_SIZE == 0) {
                        multiWarehouseRepository.saveAll(toSave);
                        toSave.clear();
                    }
                }
            }
            if (!toSave.isEmpty()) {
                log.info("Guardando batch final: {} registros", toSave.size());
                multiWarehouseRepository.saveAll(toSave);
            }

            processed = parsedData.size();
            log.info("=== IMPORT COMPLETADO === procesados={}, actualizados={}, nuevos={}, bajas={}",
                     processed, existingUpdated, (processed - existingUpdated - markedAsInactive), markedAsInactive);
            boolean hasWarnings = markedAsInactive > 0;
            ImportResult.Status finalStatus = hasWarnings
                ? ImportResult.Status.SUCCESS_WITH_WARNINGS
                : ImportResult.Status.SUCCESS;

            String logMsg = String.format(
                "Importacion completada. Procesados: %d, Almacenes: %d, Productos: %d, Actualizados: %d, Bajas: %d",
                processed, warehousesCreated, productsCreated, existingUpdated, markedAsInactive);
            updateImportLog(savedLog.getId(), finalStatus.name(), logMsg);

            MultiWarehouseImportLog logFinal = importLogRepository.findById(savedLog.getId()).orElse(savedLog);
            return ImportResult.builder()
                .status(finalStatus)
                .importLog(logFinal)
                .warningMessage(hasWarnings
                    ? String.format("\u26a0\ufe0f %d producto(s) marcados como BAJA. Revisa 'Productos dados de baja'.", markedAsInactive)
                    : null)
                .productosDadosDeBaja(productosDadosDeBaja)
                .totalProcesados(processed)
                .totalActualizados(existingUpdated)
                .totalCreados(processed - existingUpdated - markedAsInactive)
                .totalAlmacenesCreados(warehousesCreated)
                .totalProductosCreados(productsCreated)
                .totalDadosDeBaja(markedAsInactive)
                .build();

        } catch (Exception ex) {
            updateImportLog(savedLog.getId(), "ERROR", "Error: " + ex.getMessage());
            throw new RuntimeException("Error en importacion de MultiAlmacen: " + ex.getMessage(), ex);
        }
    }

    // =========================================================================
    // AUDITORIA — REQUIRES_NEW para sobrevivir rollbacks
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MultiWarehouseImportLog saveImportLog(String fileName, String period, String fileHash,
                                                  String stage, String status, String message) {
        MultiWarehouseImportLog e = new MultiWarehouseImportLog();
        e.setFileName(fileName); e.setPeriod(period); e.setImportDate(LocalDateTime.now());
        e.setStatus(status); e.setMessage(message); e.setFileHash(fileHash); e.setStage(stage);
        return importLogRepository.save(e);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateImportLog(Long logId, String status, String message) {
        importLogRepository.findById(logId).ifPresent(e -> {
            e.setStatus(status); e.setMessage(message); importLogRepository.save(e);
        });
    }

    // =========================================================================
    // EXPORTACION — paginacion en batches para evitar OOM
    // =========================================================================

    @Override
    public ExportResult exportExistences(MultiWarehouseSearchDTO search) {
        List<MultiWarehouseExistence> list = new ArrayList<>();
        int pn = 0;
        Page<MultiWarehouseExistence> page;
        do {
            page = multiWarehouseRepository.findExistences(search, PageRequest.of(pn++, BATCH_SIZE));
            list.addAll(page.getContent());
        } while (page.hasNext());

        // Ordenar: numerico si la clave lo es, alfabetico si no
        list.sort((a, b) -> {
            try { return Long.compare(Long.parseLong(a.getWarehouseKey()), Long.parseLong(b.getWarehouseKey())); }
            catch (NumberFormatException e) { return a.getWarehouseKey().compareTo(b.getWarehouseKey()); }
        });

        String header = "Clave Producto,Producto,Clave Almacen,Almacen,Estado,Existencias";
        String rows = list.stream().map(e -> String.join(",",
            safe(e.getProductCode()), safe(e.getProductName()), safe(e.getWarehouseKey()),
            safe(e.getWarehouseName()), safe(e.getStatus()),
            e.getStock() == null ? "" : e.getStock().toPlainString()
        )).collect(Collectors.joining("\n"));
        byte[] bytes = (header + "\n" + rows + (rows.isEmpty() ? "" : "\n"))
            .getBytes(StandardCharsets.UTF_8);

        return ExportResult.builder()
            .csvBytes(bytes)
            .fileName("multiwarehouse_export.csv")
            .contentType("text/csv; charset=UTF-8")
            .totalRows(list.size())
            .build();
    }

    private String safe(String v) {
        if (v == null) return "";
        return (v.contains(",") || v.contains("\"")) ? '"' + v.replace("\"", "\"\"") + '"' : v;
    }

    // =========================================================================
    // WIZARD
    // =========================================================================

    @Override
    public WizardStepResult processWizardStep(MultiWarehouseWizardStepDTO stepDTO) {
        if (stepDTO == null)
            return WizardStepResult.builder().stepNumber(0).valid(false)
                .errorCode(WizardStepResult.ErrorCode.INVALID_INPUT)
                .message("Datos del wizard requeridos").build();

        switch (stepDTO.getStepNumber()) {
            case 1: return wizardStep1(stepDTO);
            case 2: return wizardStep2(stepDTO);
            case 3: return wizardStep3(stepDTO);
            case 4: return wizardStep4(stepDTO);
            case 5: return wizardStep5(stepDTO);
            default:
                return WizardStepResult.builder().stepNumber(stepDTO.getStepNumber()).valid(false)
                    .errorCode(WizardStepResult.ErrorCode.STEP_NOT_SUPPORTED)
                    .message("Paso no soportado: " + stepDTO.getStepNumber()).build();
        }
    }

    private WizardStepResult wizardStep1(MultiWarehouseWizardStepDTO dto) {
        if (dto.getPeriod() == null || dto.getPeriod().isBlank())
            return invalidInput(1, "Periodo* es obligatorio");
        if (dto.getFileName() == null || dto.getFileName().isBlank())
            return invalidInput(1, "Archivo multialmacen.xlsx* es obligatorio");
        LocalDate pd = PeriodParserUtil.parse(dto.getPeriod());
        if (pd == null) return invalidInput(1, "Formato de periodo invalido");

        Optional<Period> p = periodRepository.findByDate(pd);
        if (p.isPresent()) {
            if (p.get().getState() == Period.PeriodState.CLOSED)
                return WizardStepResult.builder().stepNumber(1).valid(false)
                    .errorCode(WizardStepResult.ErrorCode.PERIOD_CLOSED)
                    .message("El periodo esta CLOSED, no se permite importar").build();
            if (p.get().getState() == Period.PeriodState.LOCKED)
                return WizardStepResult.builder().stepNumber(1).valid(false)
                    .errorCode(WizardStepResult.ErrorCode.PERIOD_LOCKED)
                    .message("El periodo esta LOCKED, no se permite importar").build();
        }
        return WizardStepResult.builder().stepNumber(1).valid(true).message("Paso 1 validado")
            .data(Map.of("periodExists", p.isPresent())).build();
    }

    private WizardStepResult wizardStep2(MultiWarehouseWizardStepDTO dto) {
        if (dto.getPeriod() == null || dto.getPeriod().isBlank())
            return invalidInput(2, "Periodo es obligatorio para el paso 2");
        LocalDate pd = PeriodParserUtil.parse(dto.getPeriod());
        if (pd == null) return invalidInput(2, "Formato de periodo invalido");

        Optional<Period> p = periodRepository.findByDate(pd);
        if (p.isEmpty())
            return WizardStepResult.builder().stepNumber(2).valid(true)
                .message("Periodo nuevo, se creara al importar")
                .data(Map.of("almacenesActivos", Collections.emptyList(), "totalAlmacenesActivos", 0)).build();

        List<MultiWarehouseExistence> activos = multiWarehouseRepository.findActiveByPeriodId(p.get().getId());
        Set<String> claves = activos.stream().map(MultiWarehouseExistence::getWarehouseKey).collect(Collectors.toSet());
        return WizardStepResult.builder().stepNumber(2).valid(true)
            .message("Revision de almacenes completada")
            .data(Map.of("totalAlmacenesActivos", claves.size(), "almacenesActivos", claves)).build();
    }

    private WizardStepResult wizardStep3(MultiWarehouseWizardStepDTO dto) {
        if (dto.getPeriod() == null || dto.getPeriod().isBlank())
            return invalidInput(3, "Periodo es obligatorio para el paso 3");
        LocalDate pd = PeriodParserUtil.parse(dto.getPeriod());
        if (pd == null) return invalidInput(3, "Formato de periodo invalido");

        Optional<Period> p = periodRepository.findByDate(pd);
        if (p.isEmpty())
            return WizardStepResult.builder().stepNumber(3).valid(true)
                .message("Periodo nuevo, no hay productos dados de baja")
                .data(Map.of("productosDadosDeBaja", Collections.emptyList(), "totalDadosDeBaja", 0)).build();

        List<MultiWarehouseExistence> bajas = multiWarehouseRepository.findInactiveByPeriodId(p.get().getId());
        List<Map<String, Object>> bajasDTO = bajas.stream().map(e -> Map.<String, Object>of(
            "claveProducto",  e.getProductCode()   != null ? e.getProductCode()   : "",
            "nombreProducto", e.getProductName()   != null ? e.getProductName()   : "",
            "claveAlmacen",   e.getWarehouseKey()  != null ? e.getWarehouseKey()  : "",
            "nombreAlmacen",  e.getWarehouseName() != null ? e.getWarehouseName() : "")).toList();
        return WizardStepResult.builder().stepNumber(3).valid(true)
            .message(bajas.isEmpty() ? "No hay productos dados de baja" : bajas.size() + " producto(s) dados de baja")
            .data(Map.of("totalDadosDeBaja", bajas.size(), "productosDadosDeBaja", bajasDTO)).build();
    }

    private WizardStepResult wizardStep4(MultiWarehouseWizardStepDTO dto) {
        if (!dto.isConfirmBajas())
            return invalidInput(4, "Se requiere confirmar las bajas para continuar");
        return WizardStepResult.builder().stepNumber(4).valid(true)
            .message("Bajas confirmadas. Puede proceder con la importacion")
            .data(Collections.emptyMap()).build();
    }

    private WizardStepResult wizardStep5(MultiWarehouseWizardStepDTO dto) {
        if (dto.getPeriod() != null && !dto.getPeriod().isBlank()) {
            LocalDate pd = PeriodParserUtil.parse(dto.getPeriod());
            Optional<Period> p5 = pd != null ? periodRepository.findByDate(pd) : Optional.empty();
            if (p5.isPresent()) {
                List<MultiWarehouseExistence> todos = multiWarehouseRepository.findByPeriodId(p5.get().getId());
                long activos5 = todos.stream().filter(e -> !"B".equals(e.getStatus())).count();
                long bajas5   = todos.stream().filter(e ->  "B".equals(e.getStatus())).count();
                return WizardStepResult.builder().stepNumber(5).valid(true)
                    .message("Importacion lista para ejecutar")
                    .data(Map.of("totalRegistros", todos.size(), "totalActivos", activos5,
                        "totalDadosDeBaja", bajas5, "bajasConfirmadas", dto.isConfirmBajas())).build();
            }
        }
        return WizardStepResult.builder().stepNumber(5).valid(true)
            .message("Resumen generado. Listo para importar")
            .data(Map.of("bajasConfirmadas", dto.isConfirmBajas())).build();
    }

    private WizardStepResult invalidInput(int step, String message) {
        return WizardStepResult.builder().stepNumber(step).valid(false)
            .errorCode(WizardStepResult.ErrorCode.INVALID_INPUT).message(message).build();
    }

    // =========================================================================
    // RN-MWH-002: CREACION AUTOMATICA DE ALMACENES (via port)
    // =========================================================================

    private Map<String, Long> createMissingWarehouses(List<MultiWarehouseExistence> parsedData) {
        Map<String, Long>   wMap = new HashMap<>();
        Map<String, String> nMap = new HashMap<>();

        for (MultiWarehouseExistence data : parsedData) {
            String rawKey = data.getWarehouseKey();
            if (rawKey == null || rawKey.trim().isEmpty()) continue;

            // Normalizar "55.0" -> "55"
            String wk = rawKey.trim();
            if (wk.matches("\\d+\\.0")) wk = wk.substring(0, wk.indexOf('.'));
            data.setWarehouseKey(wk);
            if (wMap.containsKey(wk)) continue;

            final String finalWk = wk; // captura final para uso en lambdas
            Optional<Long> existingId = warehousePort.findIdByWarehouseKey(finalWk);
            if (existingId.isPresent()) {
                wMap.put(finalWk, existingId.get());
                warehousePort.findNameById(existingId.get()).ifPresent(name -> nMap.put(finalWk, name));
            } else {
                String wn = (data.getWarehouseName() != null && !data.getWarehouseName().trim().isEmpty())
                    ? data.getWarehouseName().trim()
                    : (finalWk.matches("\\d+") ? "Almacen " + finalWk : finalWk);

                // Validar unicidad de nombre ANTES de guardar
                if (!warehousePort.findIdsByName(wn).isEmpty()) {
                    log.warn("Nombre en conflicto: '{}' key={}. Usando clave como nombre.", wn, finalWk);
                    wn = finalWk;
                }
                String obs = "Este almacen no existia y fue creado en la importacion el "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                Long newId = warehousePort.createWarehouse(finalWk, wn, obs);
                wMap.put(finalWk, newId);
                nMap.put(finalWk, wn);
            }
        }
        // Sincronizar nombre correcto en todos los registros del Excel
        for (MultiWarehouseExistence data : parsedData) {
            String k = data.getWarehouseKey();
            if (k != null && nMap.containsKey(k)) data.setWarehouseName(nMap.get(k));
        }
        return wMap;
    }

    // =========================================================================
    // RN-MWH-003: CREACION AUTOMATICA DE PRODUCTOS (via port)
    // =========================================================================

    private Map<String, Long> createMissingProducts(List<MultiWarehouseExistence> parsedData) {
        Map<String, Long> pMap = new HashMap<>();
        for (MultiWarehouseExistence data : parsedData) {
            String pc = data.getProductCode();
            if (pc == null || pc.trim().isEmpty() || pMap.containsKey(pc)) continue;

            Optional<Long> existingId = inventoryPort.findProductIdByCveArt(pc);
            if (existingId.isPresent()) {
                pMap.put(pc, existingId.get());
                // La descripcion SIEMPRE viene del catalogo para productos existentes
                inventoryPort.findProductDescrById(existingId.get())
                    .ifPresent(data::setProductName);
            } else {
                String desc = (data.getProductName() != null && !data.getProductName().trim().isEmpty())
                    ? data.getProductName() : pc;
                Long newId = inventoryPort.createProduct(pc, desc);
                pMap.put(pc, newId);
                data.setProductName(desc);
            }
        }
        return pMap;
    }

    // =========================================================================
    // PARSERS CSV / XLSX
    // =========================================================================

    private static final String[] COL_ALMACEN  = {"cve_alm","cvealm","almacen_clave","warehouse_key"};
    private static final String[] COL_PRODUCTO = {"cve_art","cveart","producto","product","codigo","codigo_producto","product_code"};
    private static final String[] COL_DESC     = {"descr","descripcion","description","producto_nombre","product_name"};
    private static final String[] COL_EXIST    = {"exist","existencias","stock","cantidad"};
    private static final String[] COL_ESTADO   = {"status","estado"};

    private int[] parseColumnIndexes(String[] headers, String fileType) {
        int iAlm=indexOf(headers,COL_ALMACEN), iProd=indexOf(headers,COL_PRODUCTO),
            iDesc=indexOf(headers,COL_DESC), iExist=indexOf(headers,COL_EXIST), iEst=indexOf(headers,COL_ESTADO);
        if (iAlm<0||iProd<0||iExist<0||iEst<0)
            throw new IllegalArgumentException("Columnas faltantes en "+fileType+": "
                +(iAlm<0?"CVE_ALM ":"")+(iProd<0?"CVE_ART ":"")+(iExist<0?"EXIST ":"")+(iEst<0?"STATUS":""));
        return new int[]{iAlm,iProd,iDesc,iExist,iEst};
    }

    private MultiWarehouseExistence mapRowToExistence(String[] values, int[] idx) {
        MultiWarehouseExistence e = new MultiWarehouseExistence();
        e.setWarehouseKey(get(values,idx[0])); e.setProductCode(get(values,idx[1]));
        e.setProductName(idx[2]>=0 ? get(values,idx[2]) : get(values,idx[1]));
        e.setStock(parseDecimal(get(values,idx[3]))); e.setStatus(normalizeStatus(get(values,idx[4])));
        return e;
    }

    private List<MultiWarehouseExistence> parseCsv(MultipartFile file) throws Exception {
        List<MultiWarehouseExistence> list = new ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine(); if (headerLine==null) return list;
            int[] idx = parseColumnIndexes(headerLine.split(","), "CSV");
            String line; while ((line=br.readLine())!=null) {
                if (!line.trim().isEmpty()) list.add(mapRowToExistence(splitCsv(line), idx));
            }
        }
        return list;
    }

    private List<MultiWarehouseExistence> parseXlsx(MultipartFile file) throws Exception {
        List<MultiWarehouseExistence> list = new ArrayList<>();
        try (InputStream is=file.getInputStream();
             org.apache.poi.ss.usermodel.Workbook wb=org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
            java.util.Iterator<org.apache.poi.ss.usermodel.Row> it = sheet.iterator();
            if (!it.hasNext()) return list;
            org.apache.poi.ss.usermodel.Row hr = it.next(); int cols = hr.getLastCellNum();
            String[] headers = new String[cols]; for (int i=0;i<cols;i++) headers[i]=getCellString(hr.getCell(i));
            int[] idx = parseColumnIndexes(headers, "Excel");
            while (it.hasNext()) {
                org.apache.poi.ss.usermodel.Row row = it.next(); String[] values = new String[cols];
                for (int i=0;i<cols;i++) values[i]=getCellString(row.getCell(i));
                list.add(mapRowToExistence(values, idx));
            }
        }
        return list;
    }

    private int indexOf(String[] headers, String[] candidates) {
        if (headers==null) return -1;
        for (int i=0;i<headers.length;i++) {
            String h = headers[i]==null ? "" : headers[i].trim().toLowerCase();
            for (String c : candidates) if (h.equals(c)) return i;
        }
        return -1;
    }

    private String[] splitCsv(String line) {
        List<String> out=new ArrayList<>(); StringBuilder sb=new StringBuilder(); boolean inQ=false;
        for (int i=0;i<line.length();i++) { char ch=line.charAt(i);
            if (ch=='"') { if(inQ&&i+1<line.length()&&line.charAt(i+1)=='"'){sb.append('"');i++;} else inQ=!inQ; }
            else if (ch==','&&!inQ) { out.add(sb.toString()); sb.setLength(0); } else sb.append(ch); }
        out.add(sb.toString()); return out.toArray(new String[0]);
    }

    private String get(String[] arr, int idx) { return (arr!=null&&idx>=0&&idx<arr.length)?arr[idx]:null; }

    private BigDecimal parseDecimal(String s) {
        if (s==null||s.trim().isEmpty()) return null;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private String getCellString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell==null) return null;
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default:      return null;
        }
    }

    private String normalizeStatus(String st) {
        if (st==null) return null; String s=st.trim().toUpperCase();
        if (s.startsWith("B")) return "B"; if (s.startsWith("A")) return "A"; return s;
    }

    // =========================================================================
    // HASH SHA-256
    // =========================================================================

    private String calculateSHA256(MultipartFile file) {
        try (InputStream is=file.getInputStream()) {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            DigestInputStream dis=new DigestInputStream(is,digest); byte[] buf=new byte[8192];
            //noinspection StatementWithEmptyBody
            while (dis.read(buf)!=-1) {}
            StringBuilder sb=new StringBuilder(); for (byte b:digest.digest()) sb.append(String.format("%02x",b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException("No se pudo calcular SHA-256",e); }
    }

}
