package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.input.rest;

import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryOperationsPort;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sigmav2/inventory")
public class InventoryController {

    private final InventoryOperationsPort inventoryOperations;

    public InventoryController(InventoryOperationsPort inventoryOperations, SortHandlerMethodArgumentResolverCustomizer sortHandlerMethodArgumentResolverCustomizer) {
        this.inventoryOperations = inventoryOperations;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PostMapping("/import")
    public ResponseEntity<String> importInventory(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("period") String period,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        inventoryOperations.importInventory(file, type, period, warehouseId);
        try (var inputStream = file.getInputStream()) {
            // Usando Apache POI para leer el contenido del Excel (solo la primera hoja y las primeras filas)
            org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(inputStream);
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                StringBuilder rowData = new StringBuilder();
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    rowData.append(cell.toString()).append(" | ");
                }
                System.out.println(rowData.toString());
            }
            workbook.close();
        } catch (IOException  e) {
            e.printStackTrace();
        }

        try (var inputStream = file.getInputStream()) {
            // Aquí puedes usar una librería como Apache POI para leer el contenido del Excel
            // Ejemplo simple: imprimir el número de bytes recibidos
            System.out.println("Bytes recibidos: " + inputStream.available());
            // TODO: Procesar el archivo Excel y mostrar los datos
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok("Importación iniciada correctamente");

    }

    @GetMapping("/status/{type}")
    public ResponseEntity<Map<String, Object>> getInventoryStatus(@PathVariable String type) {
        return ResponseEntity.ok(inventoryOperations.getInventoryStatus(type));
    }

    @GetMapping("/import/{jobId}")
    public ResponseEntity<Map<String, Object>> getImportStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(inventoryOperations.getImportJobStatus(jobId));
    }

    @GetMapping("/query")
    public ResponseEntity<Map<String, Object>> queryInventory(
            @RequestParam(value = "type", required = false, defaultValue = "DEFAULT") String type,
            @RequestParam("period") String period,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size)
    {
        Map<String, Object> result = inventoryOperations.queryInventory(type, period, warehouseId, search, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInventory(
            @RequestParam(value = "type", required = false, defaultValue = "DEFAULT") String type,
            @RequestParam("period") String period,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "search", required = false) String search)
            throws IOException {
        Map<String, Object> query = inventoryOperations.queryInventory(type, period, warehouseId, search, 0, Integer.MAX_VALUE);
        @SuppressWarnings("unchecked")
        List<List<String>> items = (List<List<String>>) query.get("items");
        @SuppressWarnings("unchecked")
        List<String> headers = (List<String>) query.get("headers");

        org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Inventario");
        int rowIdx = 0;
        if (headers != null && !headers.isEmpty()) {
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
        }
        if (items != null) {
            for (List<String> row : items) {
                org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowIdx++);
                for (int i = 0; i < row.size(); i++) {
                    r.createCell(i).setCellValue(row.get(i));
                }
            }
        }
        for (int i = 0; i < 10; i++) { sheet.autoSizeColumn(i); }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        String filename = "Inventario_" + URLEncoder.encode(period, StandardCharsets.UTF_8) + 
                (warehouseId != null ? ("_almacen_" + warehouseId) : "") + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(baos.toByteArray());
    }

    @GetMapping("/import/{jobId}/log")
    public ResponseEntity<byte[]> downloadImportLog(@PathVariable String jobId) {
        byte[] log = inventoryOperations.getImportLog(jobId);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_log_" + jobId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(log);
    }
}
