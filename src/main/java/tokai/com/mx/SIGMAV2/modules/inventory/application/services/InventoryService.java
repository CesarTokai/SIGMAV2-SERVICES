package tokai.com.mx.SIGMAV2.modules.inventory.application.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryOperationsPort;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.InventoryPersistencePort;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.ImportJob;

import java.io.InputStream;
import java.util.*;

@Service
public class InventoryService implements InventoryOperationsPort {

    private final InventoryPersistencePort persistencePort;

    public InventoryService(InventoryPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    @Override
    public void importInventory(MultipartFile file, String type) {
        ImportJob job = new ImportJob(UUID.randomUUID().toString(), type);
        try {
            persistencePort.saveImportJob(job.getId(), job.getStatus(), "Procesamiento iniciado");

            // Parsear el archivo y almacenar los datos usando el puerto de persistencia
            Map<String, Object> data = new HashMap<>();
            data.put("type", type);
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

    @Override
    public Map<String, Object> getInventoryStatus(String type) {
        return persistencePort.getInventoryByType(type);
    }

    @Override
    public Map<String, Object> getImportJobStatus(String jobId) {
        return persistencePort.getImportJobById(jobId);
    }
}
