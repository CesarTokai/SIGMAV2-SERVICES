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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiWarehouseServiceImpl implements MultiWarehouseService {
    private final MultiWarehouseRepository multiWarehouseRepository;
    private final MultiWarehouseImportLogRepository importLogRepository;
    private final PeriodRepository periodRepository;

    @Override
    public Page<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search, Pageable pageable) {
        if (pageable == null) {
            pageable = PageRequest.of(0, 50);
        }
        return multiWarehouseRepository.findExistences(search, pageable);
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
        MultiWarehouseImportLog savedLog = importLogRepository.save(log);

        int processed = 0;
        try {
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            List<MultiWarehouseExistence> toPersist;
            if (filename.endsWith(".csv")) {
                toPersist = parseCsv(file);
            } else {
                toPersist = parseXlsx(file);
            }
            if (!toPersist.isEmpty()) {
                multiWarehouseRepository.saveAll(toPersist);
            }
            processed = toPersist.size();
            savedLog.setStatus("SUCCESS");
            savedLog.setMessage("Importación completada. Registros: " + processed);
        } catch (Exception ex) {
            savedLog.setStatus("ERROR");
            savedLog.setMessage("Error en importación: " + ex.getMessage());
            // Propagar excepción no comprobada para activar rollback transaccional
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
        String header = "Almacen,Producto,Descripcion,Existencias,Estado";
        String rows = list.stream().map(e -> String.join(",",
                safe(e.getWarehouseName()),
                safe(e.getProductCode()),
                safe(e.getProductName()),
                e.getStock() == null ? "" : e.getStock().toPlainString(),
                safe(e.getStatus())
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
            int iAlmacen = indexOf(headers, new String[]{"almacen","almacén","warehouse","almacen_nombre"});
            int iProducto = indexOf(headers, new String[]{"producto","product","codigo","codigo_producto","product_code"});
            int iDesc = indexOf(headers, new String[]{"descripcion","descripción","description","producto_nombre","product_name"});
            int iExist = indexOf(headers, new String[]{"existencias","stock","cantidad"});
            int iEstado = indexOf(headers, new String[]{"estado","status"});
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = splitCsv(line);
                MultiWarehouseExistence e = new MultiWarehouseExistence();
                e.setWarehouseName(get(cols, iAlmacen));
                e.setProductCode(get(cols, iProducto));
                e.setProductName(get(cols, iDesc));
                e.setStock(parseDecimal(get(cols, iExist)));
                String st = get(cols, iEstado);
                e.setStatus(normalizeStatus(st));
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
            int iAlmacen = indexOf(headers, new String[]{"almacen","almacén","warehouse","almacen_nombre"});
            int iProducto = indexOf(headers, new String[]{"producto","product","codigo","codigo_producto","product_code"});
            int iDesc = indexOf(headers, new String[]{"descripcion","descripción","description","producto_nombre","product_name"});
            int iExist = indexOf(headers, new String[]{"existencias","stock","cantidad"});
            int iEstado = indexOf(headers, new String[]{"estado","status"});
            while (it.hasNext()) {
                org.apache.poi.ss.usermodel.Row row = it.next();
                MultiWarehouseExistence e = new MultiWarehouseExistence();
                e.setWarehouseName(getCellString(row.getCell(iAlmacen)));
                e.setProductCode(getCellString(row.getCell(iProducto)));
                e.setProductName(getCellString(row.getCell(iDesc)));
                e.setStock(parseDecimal(getCellString(row.getCell(iExist))));
                e.setStatus(normalizeStatus(getCellString(row.getCell(iEstado))));
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

}

