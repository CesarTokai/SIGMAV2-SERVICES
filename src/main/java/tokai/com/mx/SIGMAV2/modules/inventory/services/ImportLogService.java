package tokai.com.mx.SIGMAV2.modules.inventory.services;

import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.ImportLogEntryDto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ImportLogService {
    
    private static final String LOGS_DIRECTORY = "logs/inventory/";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    public String generateLogFile(Long periodId, String filename, List<ImportLogEntryDto> logEntries) throws IOException {
        // Crear directorio si no existe
        File logsDir = new File(LOGS_DIRECTORY);
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        // Generar nombre de archivo de log
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logFileName = String.format("import_log_period_%d_%s_%s.csv", 
            periodId, 
            sanitizeFilename(filename), 
            timestamp);
        
        File logFile = new File(logsDir, logFileName);
        
        // Escribir log CSV
        try (FileWriter writer = new FileWriter(logFile)) {
            // Escribir encabezado
            writer.write(ImportLogEntryDto.getCsvHeader() + "\n");
            
            // Escribir entradas
            for (ImportLogEntryDto entry : logEntries) {
                writer.write(entry.toCsvLine() + "\n");
            }
        }
        
        return logFile.getAbsolutePath();
    }
    
    public String generateSummaryLog(Long periodId, String filename, 
                                   int totalRows, int inserted, int updated, int skipped, 
                                   List<String> errors) throws IOException {
        // Crear directorio si no existe
        File logsDir = new File(LOGS_DIRECTORY);
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        // Generar nombre de archivo de resumen
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String summaryFileName = String.format("import_summary_period_%d_%s_%s.txt", 
            periodId, 
            sanitizeFilename(filename), 
            timestamp);
        
        File summaryFile = new File(logsDir, summaryFileName);
        
        // Escribir resumen
        try (FileWriter writer = new FileWriter(summaryFile)) {
            writer.write("RESUMEN DE IMPORTACIÓN DE INVENTARIO\n");
            writer.write("=====================================\n\n");
            writer.write("Periodo ID: " + periodId + "\n");
            writer.write("Archivo: " + filename + "\n");
            writer.write("Fecha/Hora: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "\n\n");
            
            writer.write("ESTADÍSTICAS:\n");
            writer.write("Total de filas procesadas: " + totalRows + "\n");
            writer.write("Registros insertados: " + inserted + "\n");
            writer.write("Registros actualizados: " + updated + "\n");
            writer.write("Registros omitidos: " + skipped + "\n\n");
            
            if (errors != null && !errors.isEmpty()) {
                writer.write("ERRORES ENCONTRADOS:\n");
                for (String error : errors) {
                    writer.write("- " + error + "\n");
                }
            } else {
                writer.write("No se encontraron errores.\n");
            }
        }
        
        return summaryFile.getAbsolutePath();
    }
    
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        
        // Remover extensión y caracteres especiales
        String name = filename.replaceAll("\\.[^.]+$", ""); // Remover extensión
        name = name.replaceAll("[^a-zA-Z0-9_-]", "_"); // Reemplazar caracteres especiales
        
        return name.length() > 50 ? name.substring(0, 50) : name;
    }
}