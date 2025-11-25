package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence.MultiWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.infrastructure.persistence.imports.MultiWarehouseImportLogRepository;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryStockRepository;

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
public class MultiWarehouseServiceImpl implements MultiWarehouseService {
    private final MultiWarehouseRepository multiWarehouseRepository;
    private final MultiWarehouseImportLogRepository importLogRepository;
    private final PeriodRepository periodRepository;
    private final WarehouseRepository warehouseRepository;
    private final JpaProductRepository productRepository;
    private final JpaInventoryStockRepository inventoryStockRepository;

    @Override
    public Page<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search, Pageable pageable) {
        // Configurar paginación con tamaños específicos permitidos
        if (search != null && search.getPageSize() != null) {
            int size = search.getPageSize();
            // Validar tamaños permitidos según reglas de negocio: 10, 25, 50, 100
            if (size != 10 && size != 25 && size != 50 && size != 100) {
                size = 50; // Valor por defecto
            }
            int page = pageable != null ? pageable.getPageNumber() : 0;

            // Configurar ordenación personalizada
            if (search.getOrderBy() != null && !search.getOrderBy().isBlank()) {
                String orderBy = search.getOrderBy().toLowerCase();
                boolean ascending = search.getAscending() != null ? search.getAscending() : true;

                // Mapear nombres de columnas según reglas de negocio
                String sortField = mapSortField(orderBy);
                if (sortField != null) {
                    org.springframework.data.domain.Sort sort = ascending ?
                        org.springframework.data.domain.Sort.by(sortField).ascending() :
                        org.springframework.data.domain.Sort.by(sortField).descending();
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

        // Resolver periodo si se proporciona como string
        if (search != null && (search.getPeriodId() == null) && search.getPeriod() != null && !search.getPeriod().isBlank()) {
            LocalDate date = parsePeriod(search.getPeriod());
            if (date != null) {
                Optional<Period> per = periodRepository.findByDate(date);
                per.ifPresent(p -> search.setPeriodId(p.getId()));
            }
        }

        return multiWarehouseRepository.findExistences(search, pageable);
    }

    /**
     * Mapea los nombres de columnas de la interfaz a los campos de la entidad
     * según las reglas de negocio para ordenación personalizada
     */
    private String mapSortField(String orderBy) {
        switch (orderBy) {
            case "clave_producto":
            case "producto":
                return "productCode";
            case "almacen":
            case "clave_almacen":
                return "warehouseName";
            case "estado":
                return "status";
            case "existencias":
                return "stock";
            case "descripcion":
                return "productName";
            default:
                return null;
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> importFile(MultipartFile file, String period) {
        if (period == null || period.isBlank()) {
            return ResponseEntity.badRequest().body("Periodo* es obligatorio");
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Archivo multialmacen.xlsx* es obligatorio");
        }

        // Calcular hash del archivo
        String fileHash = calculateSHA256(file);
        String stage = "default"; // Puedes ajustar si manejas varias etapas
        Optional<MultiWarehouseImportLog> existingLog = importLogRepository.findByPeriodAndStageAndFileHash(period, stage, fileHash);
        if (existingLog.isPresent()) {
            // Ya se importó este archivo para este periodo y etapa
            MultiWarehouseImportLog log = new MultiWarehouseImportLog();
            log.setFileName(file.getOriginalFilename());
            log.setPeriod(period);
            log.setImportDate(LocalDateTime.now());
            log.setStatus("NO_CHANGES");
            log.setMessage("El archivo ya fue importado previamente para este periodo y etapa. No se aplicaron cambios.");
            log.setFileHash(fileHash);
            log.setStage(stage);
            importLogRepository.save(log);
            return ResponseEntity.ok(log);
        }

        // Validar estado del periodo: rechazar si CLOSED o LOCKED
        LocalDate periodDate = parsePeriod(period);
        if (periodDate == null) {
            return ResponseEntity.badRequest().body("Formato de periodo inválido. Use MM-yyyy o yyyy-MM");
        }
        Optional<Period> perOpt = periodRepository.findByDate(periodDate);
        if (perOpt.isPresent()) {
            Period.PeriodState st = perOpt.get().getState();
            if (st == Period.PeriodState.CLOSED || st == Period.PeriodState.LOCKED) {
                return ResponseEntity.status(409).body("El periodo está " + st + ", no se permite importar");
            }
        }

        // Persist import log entry
        MultiWarehouseImportLog log = new MultiWarehouseImportLog();
        log.setFileName(file.getOriginalFilename());
        log.setPeriod(period);
        log.setImportDate(LocalDateTime.now());
        log.setStatus("STARTED");
        log.setMessage("Importación iniciada");
        log.setFileHash(fileHash);
        log.setStage(stage);
        MultiWarehouseImportLog savedLog = importLogRepository.save(log);

        int processed = 0;
        int warehousesCreated = 0;
        int productsCreated = 0;
        int existingUpdated = 0;
        int markedAsInactive = 0;

        try {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            List<MultiWarehouseExistence> parsedData;
            if (filename.endsWith(".csv")) {
                parsedData = parseCsv(file);
            } else {
                parsedData = parseXlsx(file);
            }

            // Obtener el ID del periodo (si existe) o usar un valor predeterminado
            Long periodId = perOpt.map(Period::getId).orElse(null);
            if (periodId == null) {
                throw new IllegalStateException("No se pudo determinar el ID del periodo para " + period);
            }

            // REGLA DE NEGOCIO 1: Crear almacenes que no existen
            Map<String, Long> warehouseMap = createMissingWarehouses(parsedData);
            warehousesCreated = (int) warehouseMap.values().stream().filter(id -> id != null).count() -
                              (int) warehouseRepository.count();

            // REGLA DE NEGOCIO 2: Crear productos que no existen en inventario
            Map<String, Long> productMap = createMissingProducts(parsedData, periodId);
            productsCreated = (int) productMap.values().stream().filter(id -> id != null).count() -
                            (int) productRepository.count();

            // Obtener registros existentes de multiwarehouse para el periodo
            List<MultiWarehouseExistence> existingRecords = multiWarehouseRepository.findAll()
                .stream()
                .filter(e -> e.getPeriodId().equals(periodId))
                .collect(Collectors.toList());

            // Crear mapa de registros existentes para búsqueda rápida
            // Usar warehouseKey en lugar de warehouseName para la identificación correcta
            Map<String, MultiWarehouseExistence> existingMap = existingRecords.stream()
                .collect(Collectors.toMap(
                    e -> e.getProductCode() + "|" + e.getWarehouseKey(),
                    e -> e
                ));

            // REGLA DE NEGOCIO 3 y 4: Actualizar existentes o crear nuevos
            List<MultiWarehouseExistence> toSave = new java.util.ArrayList<>();
            Long maxId = multiWarehouseRepository.findMaxId().orElse(0L);

            for (MultiWarehouseExistence newData : parsedData) {
                String key = newData.getProductCode() + "|" + newData.getWarehouseKey();

                // Asignar warehouseId basado en el mapa de almacenes (usa warehouseKey)
                Long warehouseId = warehouseMap.get(newData.getWarehouseKey());
                newData.setWarehouseId(warehouseId);
                newData.setPeriodId(periodId);

                if (existingMap.containsKey(key)) {
                    // REGLA 4: Actualizar registro existente
                    MultiWarehouseExistence existing = existingMap.get(key);
                    existing.setStock(newData.getStock());
                    existing.setStatus(newData.getStatus());
                    existing.setProductName(newData.getProductName());
                    existing.setWarehouseName(newData.getWarehouseName()); // Actualizar nombre por si cambió
                    toSave.add(existing);
                    existingUpdated++;
                } else {
                    // REGLA 3: Crear nuevo registro
                    newData.setId(++maxId);
                    toSave.add(newData);
                }
            }

            // REGLA DE NEGOCIO 5: Marcar como "B" (baja) productos no presentes en Excel
            Set<String> excelKeys = parsedData.stream()
                .map(e -> e.getProductCode() + "|" + e.getWarehouseKey())
                .collect(Collectors.toSet());

            for (MultiWarehouseExistence existing : existingRecords) {
                String key = existing.getProductCode() + "|" + existing.getWarehouseKey();
                if (!excelKeys.contains(key) && !"B".equals(existing.getStatus())) {
                    existing.setStatus("B");
                    toSave.add(existing);
                    markedAsInactive++;
                }
            }

            // Guardar todos los cambios
            if (!toSave.isEmpty()) {
                multiWarehouseRepository.saveAll(toSave);
            }

            processed = toSave.size();
            savedLog.setStatus("SUCCESS");
            savedLog.setMessage(String.format(
                "Importación completada. Registros procesados: %d, " +
                "Almacenes creados: %d, Productos creados: %d, " +
                "Existentes actualizados: %d, Marcados como baja: %d",
                processed, warehousesCreated, productsCreated, existingUpdated, markedAsInactive
            ));

        } catch (Exception ex) {
            savedLog.setStatus("ERROR");
            savedLog.setMessage("Error en importación: " + ex.getMessage());
            throw new RuntimeException(ex);
        } finally {
            importLogRepository.save(savedLog);
        }
        return ResponseEntity.ok(savedLog);
    }

    @Override
    public ResponseEntity<?> processWizardStep(MultiWarehouseWizardStepDTO stepDTO) {
        if (stepDTO == null) {
            return ResponseEntity.badRequest().body("Datos del wizard requeridos");
        }
        int step = stepDTO.getStepNumber();
        switch (step) {
            case 1:
                // Validar requeridos
                if (stepDTO.getPeriod() == null || stepDTO.getPeriod().isBlank()) {
                    return ResponseEntity.badRequest().body("Periodo* es obligatorio");
                }
                if (stepDTO.getFileName() == null || stepDTO.getFileName().isBlank()) {
                    return ResponseEntity.badRequest().body("Archivo multialmacen.xlsx* es obligatorio");
                }
                return ResponseEntity.ok("Paso 1 validado");
            case 2:
            case 3:
                // En un escenario real se detectan y muestran faltantes
                // Forzamos la regla: Debe resolver faltantes antes de continuar
                return ResponseEntity.status(409).body("Debe resolver faltantes antes de continuar");
            case 4:
                // Confirmación de bajas obligatoria para marcar STATUS = B
                if (!stepDTO.isConfirmBajas()) {
                    return ResponseEntity.badRequest().body("Se exige casilla ‘Confirmo las bajas’ para continuar");
                }
                return ResponseEntity.ok("Bajas confirmadas");
            case 5:
                // Finalizar: generar resumen y log descargable (devolvemos un texto/resumen simple)
                String resumen = "Importación finalizada. Registros procesados: " + 0 + ", Bajas confirmadas: " + (stepDTO.isConfirmBajas() ? "Sí" : "No");
                return ResponseEntity.ok(resumen);
            default:
                return ResponseEntity.badRequest().body("Paso del wizard no soportado");
        }
    }

    @Override
    public ResponseEntity<?> exportExistences(MultiWarehouseSearchDTO search) {
        // Export filtered results to CSV
        Page<MultiWarehouseExistence> page = multiWarehouseRepository.findExistences(search, PageRequest.of(0, Integer.MAX_VALUE));
        List<MultiWarehouseExistence> list = page.getContent();
        String header = "Clave Producto,Producto,Clave Almacen,Almacen,Estado,Existencias";
        String rows = list.stream().map(e -> String.join(",",
                safe(e.getProductCode()),
                safe(e.getProductName()),
                safe(e.getWarehouseKey()),
                safe(e.getWarehouseName()),
                safe(e.getStatus()),
                e.getStock() == null ? "" : e.getStock().toPlainString()
        )).collect(Collectors.joining("\n"));
        String csv = header + "\n" + rows + (rows.isEmpty() ? "" : "\n");
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=multiwarehouse_export.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String safe(String v) {
        if (v == null) return "";
        // Escape commas and quotes for CSV
        if (v.contains(",") || v.contains("\"")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    @Override
    public ResponseEntity<?> getImportLog(Long id) {
        return importLogRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Helpers
    private List<MultiWarehouseExistence> parseCsv(MultipartFile file) throws Exception {
        List<MultiWarehouseExistence> list = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return list;
            String[] headers = headerLine.split(",");
            int iAlmacenKey = indexOf(headers, new String[]{"cve_alm","CVE_ALM","almacen_clave","warehouse_key"});
            int iProducto = indexOf(headers, new String[]{"producto","product","codigo","codigo_producto","product_code","cve_art","CVE_ART"});
            int iDesc = indexOf(headers, new String[]{"descripcion","descripción","description","producto_nombre","product_name","descr","DESCR"});
            int iExist = indexOf(headers, new String[]{"existencias","stock","cantidad","exist","EXIST"});
            int iEstado = indexOf(headers, new String[]{"estado","status","STATUS"});

            // Validar que se encontraron las columnas esenciales
            if (iAlmacenKey < 0 || iProducto < 0 || iExist < 0 || iEstado < 0) {
                throw new IllegalArgumentException("El archivo CSV no contiene todas las columnas requeridas. " +
                        "Columnas no encontradas: " +
                        (iAlmacenKey < 0 ? "Clave Almacén (CVE_ALM), " : "") +
                        (iProducto < 0 ? "Producto (CVE_ART), " : "") +
                        (iExist < 0 ? "Existencias (EXIST), " : "") +
                        (iEstado < 0 ? "Estado (STATUS)" : ""));
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsv(line);
                MultiWarehouseExistence e = new MultiWarehouseExistence();
                e.setWarehouseKey(get(cols, iAlmacenKey)); // CVE_ALM
                e.setProductCode(get(cols, iProducto)); // CVE_ART
                // La descripción viene del inventario, pero si se proporciona en el Excel, la usamos temporalmente
                e.setProductName(iDesc >= 0 ? get(cols, iDesc) : get(cols, iProducto));
                e.setStock(parseDecimal(get(cols, iExist))); // EXIST
                String st = get(cols, iEstado);
                e.setStatus(normalizeStatus(st)); // STATUS
                list.add(e);
            }
        }
        return list;
    }

    private List<MultiWarehouseExistence> parseXlsx(MultipartFile file) throws Exception {
        List<MultiWarehouseExistence> list = new java.util.ArrayList<>();
        try (java.io.InputStream is = file.getInputStream()) {
            org.apache.poi.ss.usermodel.Workbook wb = org.apache.poi.ss.usermodel.WorkbookFactory.create(is);
            org.apache.poi.ss.usermodel.Sheet sheet = wb.getSheetAt(0);
            java.util.Iterator<org.apache.poi.ss.usermodel.Row> it = sheet.iterator();
            if (!it.hasNext()) { wb.close(); return list; }
            org.apache.poi.ss.usermodel.Row header = it.next();
            int cols = header.getLastCellNum();
            String[] headers = new String[cols];
            for (int i=0;i<cols;i++) headers[i] = getCellString(header.getCell(i));
            int iAlmacenKey = indexOf(headers, new String[]{"cve_alm","CVE_ALM","almacen_clave","warehouse_key"});
            int iProducto = indexOf(headers, new String[]{"producto","product","codigo","codigo_producto","product_code","cve_art","CVE_ART"});
            int iDesc = indexOf(headers, new String[]{"descripcion","descripción","description","producto_nombre","product_name","descr","DESCR"});
            int iExist = indexOf(headers, new String[]{"existencias","stock","cantidad","exist","EXIST"});
            int iEstado = indexOf(headers, new String[]{"estado","status","STATUS"});

            // Validar que se encontraron las columnas esenciales
            if (iAlmacenKey < 0 || iProducto < 0 || iExist < 0 || iEstado < 0) {
                throw new IllegalArgumentException("El archivo Excel no contiene todas las columnas requeridas. " +
                        "Columnas no encontradas: " +
                        (iAlmacenKey < 0 ? "Clave Almacén (CVE_ALM), " : "") +
                        (iProducto < 0 ? "Producto (CVE_ART), " : "") +
                        (iExist < 0 ? "Existencias (EXIST), " : "") +
                        (iEstado < 0 ? "Estado (STATUS)" : ""));
            }

            while (it.hasNext()) {
                org.apache.poi.ss.usermodel.Row row = it.next();
                MultiWarehouseExistence e = new MultiWarehouseExistence();
                e.setWarehouseKey(getCellString(row.getCell(iAlmacenKey))); // CVE_ALM
                e.setProductCode(getCellString(row.getCell(iProducto))); // CVE_ART
                // La descripción viene del inventario, pero si se proporciona en el Excel, la usamos temporalmente
                e.setProductName(iDesc >= 0 ? getCellString(row.getCell(iDesc)) : getCellString(row.getCell(iProducto)));
                e.setStock(parseDecimal(getCellString(row.getCell(iExist)))); // EXIST
                e.setStatus(normalizeStatus(getCellString(row.getCell(iEstado)))); // STATUS
                list.add(e);
            }
            wb.close();
        }
        return list;
    }


    private int indexOf(String[] headers, String[] candidates) {
        if (headers == null) return -1;
        for (int i=0;i<headers.length;i++) {
            String h = headers[i] == null ? "" : headers[i].trim().toLowerCase();
            for (String c : candidates) {
                if (h.equals(c)) return i;
            }
        }
        return -1;
    }

    private String[] splitCsv(String line) {
        // Basic CSV split, supports simple quoted values
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i=0;i<line.length();i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i+1 < line.length() && line.charAt(i+1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                out.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }

    private String get(String[] arr, int idx) { return (arr != null && idx >=0 && idx < arr.length) ? arr[idx] : null; }

    private java.math.BigDecimal parseDecimal(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return new java.math.BigDecimal(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private String getCellString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return null;
        }
    }

    private String normalizeStatus(String st) {
        if (st == null) return null;
        String s = st.trim().toUpperCase();
        if (s.startsWith("B")) return "B"; // Baja
        if (s.startsWith("A")) return "A"; // Alta/Activa
        return s;
    }

    private LocalDate parsePeriod(String period) {
        // Acepta MM-yyyy o yyyy-MM
        String p = period.trim();
        try {
            if (p.matches("\\d{2}-\\d{4}")) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-yyyy");
                java.time.YearMonth ym = java.time.YearMonth.parse(p, fmt);
                return ym.atDay(1);
            }
            if (p.matches("\\d{4}-\\d{2}")) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
                java.time.YearMonth ym = java.time.YearMonth.parse(p, fmt);
                return ym.atDay(1);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * REGLA DE NEGOCIO 1: Crear almacenes que no existen
     * Si aparecen almacenes que no existen en el SIGMA, éstos serán creados automáticamente
     * y se les agregará la leyenda: "Este almacén no existía y fue creado en la importación"
     *
     * IMPORTANTE: CVE_ALM del Excel representa la clave del almacén (warehouse_key)
     */
    private Map<String, Long> createMissingWarehouses(List<MultiWarehouseExistence> parsedData) {
        Map<String, Long> warehouseMap = new HashMap<>();

        for (MultiWarehouseExistence data : parsedData) {
            String warehouseKey = data.getWarehouseKey(); // CVE_ALM del Excel
            if (warehouseKey == null || warehouseKey.trim().isEmpty()) {
                continue;
            }

            if (!warehouseMap.containsKey(warehouseKey)) {
                // Buscar almacén existente por clave
                Optional<WarehouseEntity> existing = warehouseRepository.findByWarehouseKeyAndDeletedAtIsNull(warehouseKey);

                if (existing.isPresent()) {
                    warehouseMap.put(warehouseKey, existing.get().getId());
                    // Actualizar el nombre del almacén en el objeto de datos
                    data.setWarehouseName(existing.get().getNameWarehouse());
                } else {
                    // Crear nuevo almacén - el nombre será el mismo que la clave si no se proporciona
                    String warehouseName = data.getWarehouseName() != null && !data.getWarehouseName().trim().isEmpty()
                        ? data.getWarehouseName()
                        : warehouseKey;

                    WarehouseEntity newWarehouse = new WarehouseEntity();
                    newWarehouse.setWarehouseKey(warehouseKey);
                    newWarehouse.setNameWarehouse(warehouseName);
                    newWarehouse.setObservations("Este almacén no existía y fue creado en la importación");
                    newWarehouse.setCreatedAt(LocalDateTime.now());
                    newWarehouse.setUpdatedAt(LocalDateTime.now());

                    WarehouseEntity saved = warehouseRepository.save(newWarehouse);
                    warehouseMap.put(warehouseKey, saved.getId());
                    data.setWarehouseName(warehouseName);
                }
            }
        }

        return warehouseMap;
    }

    /**
     * REGLA DE NEGOCIO 2: Crear productos que no existen en inventario
     * Si aparecen productos que no están en el inventario del periodo elegido,
     * éstos serán creados automáticamente con estado "A"
     *
     * IMPORTANTE: La descripción del producto (DESCR) viene del catálogo de inventario
     */
    private Map<String, Long> createMissingProducts(List<MultiWarehouseExistence> parsedData, Long periodId) {
        Map<String, Long> productMap = new HashMap<>();

        for (MultiWarehouseExistence data : parsedData) {
            String productCode = data.getProductCode(); // CVE_ART
            if (productCode == null || productCode.trim().isEmpty()) {
                continue;
            }

            if (!productMap.containsKey(productCode)) {
                Optional<ProductEntity> existing = productRepository.findByCveArt(productCode);

                if (existing.isPresent()) {
                    productMap.put(productCode, existing.get().getIdProduct());
                    // Actualizar la descripción del producto desde el inventario
                    data.setProductName(existing.get().getDescr());
                } else {
                    // Crear nuevo producto
                    ProductEntity newProduct = new ProductEntity();
                    newProduct.setCveArt(productCode);
                    // La descripción puede venir del Excel o usar el código como respaldo
                    String description = data.getProductName() != null && !data.getProductName().trim().isEmpty()
                        ? data.getProductName()
                        : productCode;
                    newProduct.setDescr(description);
                    newProduct.setStatus("A"); // Estado Alta según regla de negocio
                    newProduct.setCreatedAt(LocalDateTime.now());
                    newProduct.setUniMed("PZA"); // Valor por defecto para uni_med

                    ProductEntity saved = productRepository.save(newProduct);
                    productMap.put(productCode, saved.getIdProduct());
                    data.setProductName(description);
                }
            }
        }

        return productMap;
    }


    // Calcula el hash SHA-256 de un archivo MultipartFile
    private String calculateSHA256(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream dis = new DigestInputStream(is, digest);
            byte[] buffer = new byte[4096];
            while (dis.read(buffer) != -1) {}
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo calcular el hash SHA-256 del archivo", e);
        }
    }

}
