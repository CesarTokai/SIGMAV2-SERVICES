package tokai.com.mx.SIGMAV2.modules.inventory.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.model.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.input.*;
import tokai.com.mx.SIGMAV2.modules.inventory.domain.ports.output.*;
import tokai.com.mx.SIGMAV2.modules.periods.domain.model.Period;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventorySnapshotRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.InventorySnapshotJpaEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;

import tokai.com.mx.SIGMAV2.modules.inventory.application.dto.InventoryPeriodReportDTO;

import tokai.com.mx.SIGMAV2.modules.personal_information.infrastructure.persistence.JpaPersonalInformationRepository;

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
    private JpaInventorySnapshotRepository jpaInventorySnapshotRepository;

    @Autowired
    private JpaProductRepository jpaProductRepository;

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

    @GetMapping("/latest-period")
    public ResponseEntity<Period> getLatestPeriod() {
        Optional<Period> latestPeriod = periodRepository.findLatest();
        return latestPeriod.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Nuevo endpoint: reporte por periodo y almacén con paginación, búsqueda y ordenación
    @GetMapping("/period-report")
    public ResponseEntity<Page<InventoryPeriodReportDTO>> periodReport(
            @RequestParam Long periodId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "cveArt,asc") String[] sort) {

        // Crear Pageable con ordenación
        List<Sort.Order> orders = new ArrayList<>();
        if (sort[0].contains(",")) {
            // sort="field,direction"
            for (String sortOrder : sort) {
                String[] _sort = sortOrder.split(",");
                String field = mapSortField(_sort[0]);
                Sort.Direction direction = _sort.length > 1 && _sort[1].equalsIgnoreCase("desc")
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
                orders.add(new Sort.Order(direction, field));
            }
        } else {
            // sort=field&direction=asc/desc
            String field = mapSortField(sort[0]);
            Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, field));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));

        // Buscar con paginación
        Page<InventorySnapshotJpaEntity> entitiesPage =
            jpaInventorySnapshotRepository.findByPeriodWithSearch(periodId, warehouseId, search, pageable);

        // Mapear a DTO
        Page<InventoryPeriodReportDTO> reportPage = entitiesPage.map(e -> {
            InventoryPeriodReportDTO dto = new InventoryPeriodReportDTO();
            // obtener producto
            Optional<ProductEntity> prodOpt = jpaProductRepository.findById(e.getProductId());
            if (prodOpt.isPresent()) {
                ProductEntity pe = prodOpt.get();
                dto.setCveArt(pe.getCveArt());
                dto.setDescr(pe.getDescr());
                dto.setUniMed(pe.getUniMed());
            } else {
                dto.setCveArt(null);
                dto.setDescr(null);
                dto.setUniMed(null);
            }
            dto.setExistQty(e.getExistQty());
            dto.setStatus(e.getStatus());
            return dto;
        });

        return ResponseEntity.ok(reportPage);
    }

    /**
     * Mapea los nombres de campo del frontend a los nombres de campo de la base de datos
     */
    private String mapSortField(String field) {
        return switch (field) {
            case "cveArt" -> "productId"; // Ordenar por ID ya que no tenemos join directo
            case "descr" -> "productId";
            case "uniMed" -> "productId";
            case "existQty", "existencias" -> "existQty";
            case "status", "estado" -> "status";
            default -> "productId";
        };
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
