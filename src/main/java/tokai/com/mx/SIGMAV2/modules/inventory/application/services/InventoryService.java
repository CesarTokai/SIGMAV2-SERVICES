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
            persistencePort.saveInventoryData(data);

            job.markAsCompleted("Importación completada. Filas: " + rows.size());
            persistencePort.saveImportJob(job.getId(), job.getStatus(), job.getMessage());
        } catch (Exception e) {
            job.markAsFailed(e.getMessage());
            persistencePort.saveImportJob(job.getId(), job.getStatus(), job.getMessage());
            throw new RuntimeException(e);
        }
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
}
