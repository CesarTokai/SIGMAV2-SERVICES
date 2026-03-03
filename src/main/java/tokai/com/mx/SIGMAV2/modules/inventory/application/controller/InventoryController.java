package tokai.com.mx.SIGMAV2.modules.inventory.application.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.InventoryStock;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryImportUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.InventoryQueryUseCase;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.PeriodRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.ProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.WarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventorySnapshotJpaEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventorySnapshotRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/inventory")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','USUARIO')")
public class InventoryController {

    private final InventoryQueryUseCase inventoryQueryUseCase;
    private final InventoryImportUseCase inventoryImportUseCase;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final JpaInventorySnapshotRepository jpaInventorySnapshotRepository;
    private final JpaProductRepository jpaProductRepository;
    private final PeriodRepository periodRepository;

    public InventoryController(
            InventoryQueryUseCase inventoryQueryUseCase,
            InventoryImportUseCase inventoryImportUseCase,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            JpaInventorySnapshotRepository jpaInventorySnapshotRepository,
            JpaProductRepository jpaProductRepository,
            @Qualifier("inventoryPeriodRepositoryAdapter") PeriodRepository periodRepository) {
        this.inventoryQueryUseCase = inventoryQueryUseCase;
        this.inventoryImportUseCase = inventoryImportUseCase;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.jpaInventorySnapshotRepository = jpaInventorySnapshotRepository;
        this.jpaProductRepository = jpaProductRepository;
        this.periodRepository = periodRepository;
    }

    // ── Stock ─────────────────────────────────────────────────────────────

    @GetMapping("/stock")
    public ResponseEntity<InventoryStockDTO> getCurrentStock(
            @RequestParam("productId") Long productId,
            @RequestParam("warehouseId") Long warehouseId,
            @RequestParam("periodId") Long periodId) {
        InventoryStock stock = inventoryQueryUseCase.getCurrentStock(productId, warehouseId, periodId);
        if (stock == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(mapToStockDTO(stock));
    }

    // ── Importación ───────────────────────────────────────────────────────

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA')")
    public ResponseEntity<InventoryImportResultDTO> importInventory(
            @RequestParam("periodId") Long periodId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        InventoryImportRequestDTO request = new InventoryImportRequestDTO();
        request.setIdPeriod(periodId);
        request.setIdWarehouse(warehouseId);
        request.setFile(file);
        InventoryImportResultDTO result = inventoryImportUseCase.importInventory(
                request, principal != null ? principal.getName() : "sistema");
        return ResponseEntity.ok(result);
    }

    // ── Productos ─────────────────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<List<Product>> listProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    // ── Almacenes ─────────────────────────────────────────────────────────

    @GetMapping("/warehouses")
    public ResponseEntity<List<Warehouse>> listWarehouses() {
        return ResponseEntity.ok(warehouseRepository.findAll());
    }

    // ── Periodos ──────────────────────────────────────────────────────────

    @GetMapping("/all-periods")
    public ResponseEntity<List<Period>> listPeriods() {
        List<Period> periods = periodRepository.findAll(Pageable.unpaged()).getContent();
        return ResponseEntity.ok(periods);
    }

    @GetMapping("/latest-period")
    public ResponseEntity<Period> getLatestPeriod() {
        return periodRepository.findLatest()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Reporte por periodo ───────────────────────────────────────────────

    @GetMapping("/period-report")
    public ResponseEntity<Page<InventoryPeriodReportDTO>> periodReport(
            @RequestParam("periodId") Long periodId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "existQty,asc") String[] sort) {

        List<Sort.Order> orders = buildSortOrders(sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

        Page<InventorySnapshotJpaEntity> entitiesPage =
                jpaInventorySnapshotRepository.findByPeriodWithSearch(periodId, warehouseId, search, pageable);

        Page<InventoryPeriodReportDTO> reportPage = entitiesPage.map(e -> {
            InventoryPeriodReportDTO dto = new InventoryPeriodReportDTO();
            jpaProductRepository.findById(e.getProductId()).ifPresentOrElse(pe -> {
                dto.setCveArt(pe.getCveArt());
                dto.setDescr(pe.getDescr());
                dto.setUniMed(pe.getUniMed());
            }, () -> {
                dto.setCveArt("N/A");
                dto.setDescr("Producto no encontrado");
                dto.setUniMed("-");
            });
            dto.setExistQty(e.getExistQty());
            dto.setStatus(e.getStatus());
            return dto;
        });

        return ResponseEntity.ok(reportPage);
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    /**
     * Construye los criterios de ordenamiento mapeando campos del frontend
     * a columnas reales de la entidad JPA.
     * ✅ Fix: ahora ordena por el campo correcto de la tabla, no siempre por productId.
     */
    private List<Sort.Order> buildSortOrders(String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParam : sort) {
            String[] parts = sortParam.split(",");
            String field = mapSortField(parts[0].trim());
            Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, field));
        }
        if (orders.isEmpty()) {
            orders.add(new Sort.Order(Sort.Direction.ASC, "existQty"));
        }
        return orders;
    }

    /**
     * Mapea nombres de campo del frontend a campos de InventorySnapshotJpaEntity.
     * Para campos de producto (cveArt, descr, uniMed) se ordena por productId como
     * aproximación hasta que se implemente un UseCase con JOIN real.
     */
    private String mapSortField(String field) {
        return switch (field.toLowerCase()) {
            case "existqty", "existencias", "exist_qty" -> "existQty";
            case "status", "estado"                     -> "status";
            case "createdat", "created_at"              -> "createdAt";
            // Para campos de producto, el orden exacto requeriría un JOIN;
            // se usa productId como proxy hasta migrar period-report a un UseCase dedicado.
            default -> "productId";
        };
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
