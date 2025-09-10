package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.input.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryOperationsPort;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryOperationsPort inventoryOperations;

    public InventoryController(InventoryOperationsPort inventoryOperations) {
        this.inventoryOperations = inventoryOperations;
    }

    @PostMapping("/import")
    public ResponseEntity<String> importInventory(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        inventoryOperations.importInventory(file, type);
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
