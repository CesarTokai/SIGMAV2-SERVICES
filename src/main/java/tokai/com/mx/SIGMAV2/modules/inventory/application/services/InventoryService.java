package tokai.com.mx.SIGMAV2.modules.inventory.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryOperationsPort;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryPersistencePort;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.ImportJob;
import tokai.com.mx.SIGMAV2.modules.periods.application.port.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InventoryService implements InventoryOperationsPort {

    private final InventoryPersistencePort persistencePort;
    private final PeriodRepository periodRepository;

    public InventoryService(InventoryPersistencePort persistencePort, PeriodRepository periodRepository) {
        this.persistencePort = persistencePort;
        this.periodRepository = periodRepository;
    }

    @Override
    @Transactional
    public void importInventory(MultipartFile file, String type, String period, Long warehouseId) {
        // Validaciones de entrada
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("Periodo* es obligatorio");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo .xlsx/.csv es obligatorio");
        }
        LocalDate periodDate = parsePeriod(period);
        if (periodDate == null) {
            throw new IllegalArgumentException("Formato de periodo inválido. Use MM-yyyy o yyyy-MM");
        }
        periodRepository.findByDate(periodDate).ifPresent(p -> {
            if (p.getState() == Period.PeriodState.CLOSED || p.getState() == Period.PeriodState.LOCKED) {
                throw new IllegalStateException("El periodo está " + p.getState() + ", no se permite importar");
            }
        });

        ImportJob job = new ImportJob(UUID.randomUUID().toString(), type);
        try {
            persistencePort.saveImportJob(job.getId(), job.getStatus(), "Procesamiento iniciado");

            // Parsear el archivo y almacenar los datos usando el puerto de persistencia
            Map<String, Object> data = new HashMap<>();
            data.put("type", type);
            data.put("period", periodDate.toString());
            if (warehouseId != null) data.put("warehouseId", warehouseId);
            List<List<String>> rows = new ArrayList<>();

            try (InputStream inputStream = file.getInputStream()) {
                org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream);
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        rowData.add(cell.toString());
                    }
                    rows.add(rowData);
                }
                workbook.close();
            }

            data.put("rows", rows);
            data.put("rowCount", rows.size());
            // Simple headers mapping if first row seems like headers
            if (!rows.isEmpty()) {
                data.put("headers", rows.get(0));
            }
            persistencePort.saveInventoryData(data);

            // Build simple CSV log (as example): treat all rows after header as 'ALTA'
            StringBuilder sb = new StringBuilder();
            sb.append("accion,codigo,descripcion,unidad,existencia,estatus\n");
            int start = !rows.isEmpty() ? 1 : 0;
            for (int i = start; i < rows.size(); i++) {
                List<String> r = rows.get(i);
                String cve = !r.isEmpty() ? r.get(0) : "";
                String desc = r.size() > 1 ? r.get(1) : "";
                String uni = r.size() > 2 ? r.get(2) : "";
                String exist = r.size() > 3 ? r.get(3) : "";
                String status = r.size() > 4 ? r.get(4) : "";
                sb.append("ALTA").append(',')
                  .append(escapeCsv(cve)).append(',')
                  .append(escapeCsv(desc)).append(',')
                  .append(escapeCsv(uni)).append(',')
                  .append(escapeCsv(exist)).append(',')
                  .append(escapeCsv(status)).append('\n');
            }
            persistencePort.saveImportLog(job.getId(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            job.markAsCompleted("Importación completada. Filas: " + rows.size());
            persistencePort.saveImportJob(job.getId(), job.getStatus(), job.getMessage());
        } catch (Exception e) {
            job.markAsFailed(e.getMessage());
            persistencePort.saveImportJob(job.getId(), job.getStatus(), job.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\n") || s.contains("\"");
        String val = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + val + "\"" : val;
    }

    private LocalDate parsePeriod(String period) {
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

    @Override
    public Map<String, Object> getInventoryStatus(String type) {
        return persistencePort.getInventoryByType(type);
    }

    @Override
    public Map<String, Object> getImportJobStatus(String jobId) {
        return persistencePort.getImportJobById(jobId);
    }

    @Override
    public byte[] getImportLog(String jobId) {
        return persistencePort.getImportLog(jobId);
    }

    @Override
    public Map<String, Object> queryInventory(String type, String period, Long warehouseId, String search, Integer page, Integer size) {
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("Periodo* es obligatorio");
        }
        if (type == null || type.isBlank()) type = "DEFAULT";
        Map<String, Object> data = persistencePort.getInventoryByTypeAndPeriod(type, period, warehouseId);
        @SuppressWarnings("unchecked")
        List<List<String>> rows = (List<List<String>>) data.getOrDefault("rows", Collections.emptyList());

        // Filter by search across all columns
        List<List<String>> filtered = rows;
        if (search != null && !search.isBlank()) {
            String needle = search.toLowerCase(Locale.ROOT);
            filtered = new ArrayList<>();
            for (List<String> r : rows) {
                boolean match = false;
                for (String c : r) {
                    if (c != null && c.toLowerCase(Locale.ROOT).contains(needle)) { match = true; break; }
                }
                if (match) filtered.add(r);
            }
        }

        int total = filtered.size();
        int pageNum = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size <= 0 ? 10 : size;

        int from = Math.min(pageNum * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<List<String>> pageItems = filtered.subList(from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("type", type);
        result.put("warehouseId", warehouseId);
        result.put("total", total);
        result.put("page", pageNum);
        result.put("size", pageSize);
        result.put("items", pageItems);
        // include headers if provided in data
        if (data.containsKey("headers")) {
            result.put("headers", data.get("headers"));
        }
        return result;
    }

    @Override
    public byte[] exportInventory(String type, String period, Long warehouseId, String search, String format) {
        return new byte[0];
    }


    @Override
    public Map<String, Object> getInventoryMatrix(String period, Long[] warehouseIds) {
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("Periodo* es obligatorio");
        }
        return persistencePort.getInventoryMatrix(period, warehouseIds);
    }

    @Override
    @Transactional
    public void importMultiWarehouseInventory(MultipartFile file, String period) {
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("Periodo* es obligatorio");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo .xlsx/.csv es obligatorio");
        }
        LocalDate periodDate = parsePeriod(period);
        if (periodDate == null) {
            throw new IllegalArgumentException("Formato de periodo inválido. Use MM-yyyy o yyyy-MM");
        }
        periodRepository.findByDate(periodDate).ifPresent(p -> {
            if (p.getState() == Period.PeriodState.CLOSED || p.getState() == Period.PeriodState.LOCKED) {
                throw new IllegalStateException("El periodo está " + p.getState() + ", no se permite importar");
            }
        });
        try (InputStream inputStream = file.getInputStream()) {
            org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream);
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            List<List<String>> rows = new ArrayList<>();
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    rowData.add(cell.toString());
                }
                rows.add(rowData);
            }
            workbook.close();
            Map<String, Object> data = new HashMap<>();
            data.put("period", periodDate.toString());
            data.put("rows", rows);
            if (!rows.isEmpty()) {
                data.put("headers", rows.get(0));
            }
            persistencePort.saveMultiWarehouseInventoryData(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
