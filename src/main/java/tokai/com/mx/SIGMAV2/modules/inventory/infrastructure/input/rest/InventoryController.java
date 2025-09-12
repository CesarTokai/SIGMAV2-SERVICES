package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.input.rest;

import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryOperationsPort;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/sigmav2/inventory")
public class InventoryController {

    private final InventoryOperationsPort inventoryOperations;

    public InventoryController(InventoryOperationsPort inventoryOperations, SortHandlerMethodArgumentResolverCustomizer sortHandlerMethodArgumentResolverCustomizer) {
        this.inventoryOperations = inventoryOperations;
    }

    @PreAuthorize("hasRole('ADMIN')")
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
}
