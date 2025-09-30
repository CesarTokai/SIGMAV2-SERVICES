package tokai.com.mx.SIGMAV2.modules.inventory.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.*;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.security.Principal;
import java.util.List;
import tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence.JpaPersonalInformationRepository;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.BeanPersonalInformation;

@RestController
@RequestMapping("/api/sigmav2/inventory")
public class InventoryController {
    private final InventoryQueryUseCase inventoryQueryUseCase;
    private final InventoryImportUseCase inventoryImportUseCase;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final PeriodRepository periodRepository;

    @Autowired
    private JpaPersonalInformationRepository personalInformationRepository;

    @Autowired
    public InventoryController(
            InventoryQueryUseCase inventoryQueryUseCase,
            InventoryImportUseCase inventoryImportUseCase,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            @Qualifier("inventoryPeriodRepositoryAdapter") PeriodRepository periodRepository
    ) {
        this.inventoryQueryUseCase = inventoryQueryUseCase;
        this.inventoryImportUseCase = inventoryImportUseCase;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.periodRepository = periodRepository;
    }

    // 1. Consultar snapshots por periodo y almacén
    @GetMapping("/snapshots")
    public ResponseEntity<List<InventorySnapshotDTO>> getSnapshotsByPeriodAndWarehouse(
            @RequestParam Long periodId,
            @RequestParam Long warehouseId) {
        List<InventorySnapshot> snapshots = inventoryQueryUseCase.getSnapshotsByPeriodAndWarehouse(periodId, warehouseId);
        List<InventorySnapshotDTO> dtos = snapshots.stream()
                .map(this::mapToSnapshotDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 2. Consultar stock actual
    @GetMapping("/stock")
    public ResponseEntity<InventoryStockDTO> getCurrentStock(
            @RequestParam Long productId,
            @RequestParam Long warehouseId) {
        InventoryStock stock = inventoryQueryUseCase.getCurrentStock(productId, warehouseId);
        if (stock == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapToStockDTO(stock));
    }

    // 3. Importar inventario (carga masiva)
    @PostMapping("/import")
    public ResponseEntity<InventoryImportResultDTO> importInventory(
            @RequestParam Long periodId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        InventoryImportRequestDTO request = new InventoryImportRequestDTO();
        request.setIdPeriod(periodId);
        request.setIdWarehouse(warehouseId);
        request.setFile(file);
        // El nombre completo se obtiene en el servicio usando principal.getName()
        InventoryImportResultDTO result = inventoryImportUseCase.importInventory(request, principal.getName());
        return ResponseEntity.ok(result);
    }

    // 4. Exportar inventario (solo estructura, lógica en el servicio)
    @GetMapping("/export")
    public ResponseEntity<?> exportInventory(
            @RequestParam Long periodId,
            @RequestParam Long warehouseId,
            @RequestParam String format) {
        return ResponseEntity.ok("Exportación no implementada en este ejemplo");
    }

    // 5. Listar productos
    @GetMapping("/products")
    public ResponseEntity<List<Product>> listProducts() {
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(products);
    }

    // 6. Listar almacenes
    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> listWarehouses() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        return ResponseEntity.ok(warehouses);
    }

    @GetMapping("/all-periods")
    public ResponseEntity<List<Period>> listPeriods() {
        List<Period> periods = periodRepository.findAll(Pageable.unpaged()).getContent();
        return ResponseEntity.ok(periods);
    }

    // Mapeos mínimos a DTO
    private InventorySnapshotDTO mapToSnapshotDTO(InventorySnapshot snapshot) {
        InventorySnapshotDTO dto = new InventorySnapshotDTO();
        dto.setId(snapshot.getId());
        dto.setProductId(snapshot.getProduct() != null ? snapshot.getProduct().getId() : null);
        dto.setWarehouseId(snapshot.getWarehouse() != null ? snapshot.getWarehouse().getId() : null);
        dto.setPeriodId(snapshot.getPeriod() != null ? snapshot.getPeriod().getId() : null);
        dto.setExistQty(snapshot.getExistQty());
        dto.setCreatedAt(snapshot.getCreatedAt());
        return dto;
    }

    private InventoryStockDTO mapToStockDTO(InventoryStock stock) {
        InventoryStockDTO dto = new InventoryStockDTO();
        dto.setId(stock.getId());
        dto.setProductId(stock.getProductId());
        dto.setWarehouseId(stock.getWarehouseId());
        dto.setExistQty(stock.getExistQty());
        dto.setStatus(stock.getStatus());
        dto.setUpdatedAt(stock.getUpdatedAt());
        return dto;
    }
}
