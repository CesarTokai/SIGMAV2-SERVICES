package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.users.infrastructure.persistence.JpaUserRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelQueryServiceTest {

    @Mock LabelsPersistenceAdapter persistence;
    @Mock WarehouseAccessService warehouseAccessService;
    @Mock JpaProductRepository productRepository;
    @Mock JpaWarehouseRepository warehouseRepository;
    @Mock JpaInventoryStockRepository inventoryStockRepository;
    @Mock JpaLabelRequestRepository labelRequestRepository;
    @Mock JpaUserRepository userRepository;
    @Mock JpaLabelRepository jpaLabelRepository;
    @Mock JpaLabelCountEventRepository jpaLabelCountEventRepository;
    @Mock JpaPeriodRepository jpaPeriodRepository;

    @InjectMocks LabelQueryService service;

    private Long periodId = 1L;
    private Long warehouseId = 10L;
    private Long productId = 100L;
    private Long userId = 1L;
    private Long folio = 1000L;
    private String userRole = "ALMACENISTA";

    @BeforeEach
    void setUp() {
        when(warehouseAccessService.hasFullAccess(anyString())).thenReturn(false);
    }

    @Test
    void getPendingPrintCount_happy_path() {
        PendingPrintCountRequestDTO dto = new PendingPrintCountRequestDTO();
        dto.setPeriodId(periodId);
        dto.setWarehouseId(warehouseId);

        Label label1 = new Label();
        label1.setFolio(1L);
        label1.setEstado(Label.State.GENERADO);

        Label label2 = new Label();
        label2.setFolio(2L);
        label2.setEstado(Label.State.GENERADO);

        when(persistence.findPendingLabelsByPeriodAndWarehouse(periodId, warehouseId))
                .thenReturn(List.of(label1, label2));
        when(warehouseRepository.findById(warehouseId))
                .thenReturn(Optional.of(createWarehouseEntity(warehouseId, "Almacén 1")));

        PendingPrintCountResponseDTO result = service.getPendingPrintCount(dto, userId, userRole);

        assertThat(result).isNotNull();
        assertThat(result.getTotalPending()).isEqualTo(2L);
        verify(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);
        verify(persistence).findPendingLabelsByPeriodAndWarehouse(periodId, warehouseId);
    }

    @Test
    void getPendingPrintCount_with_product_filter() {
        PendingPrintCountRequestDTO dto = new PendingPrintCountRequestDTO();
        dto.setPeriodId(periodId);
        dto.setWarehouseId(warehouseId);
        dto.setProductId(productId);

        Label label = new Label();
        label.setFolio(1L);
        label.setEstado(Label.State.GENERADO);
        label.setProductId(productId);

        when(persistence.findPendingLabelsByPeriodWarehouseAndProduct(periodId, warehouseId, productId))
                .thenReturn(List.of(label));
        when(warehouseRepository.findById(warehouseId))
                .thenReturn(Optional.of(createWarehouseEntity(warehouseId, "Almacén 1")));

        PendingPrintCountResponseDTO result = service.getPendingPrintCount(dto, userId, userRole);

        assertThat(result.getTotalPending()).isEqualTo(1L);
        verify(persistence).findPendingLabelsByPeriodWarehouseAndProduct(periodId, warehouseId, productId);
    }

    @Test
    void getPendingPrintCount_empty_results() {
        PendingPrintCountRequestDTO dto = new PendingPrintCountRequestDTO();
        dto.setPeriodId(periodId);
        dto.setWarehouseId(warehouseId);

        when(persistence.findPendingLabelsByPeriodAndWarehouse(periodId, warehouseId))
                .thenReturn(Collections.emptyList());

        PendingPrintCountResponseDTO result = service.getPendingPrintCount(dto, userId, userRole);

        assertThat(result.getTotalPending()).isEqualTo(0L);
    }

    @Test
    void getPendingPrintCount_warehouse_access_validation() {
        PendingPrintCountRequestDTO dto = new PendingPrintCountRequestDTO();
        dto.setPeriodId(periodId);
        dto.setWarehouseId(warehouseId);

        doThrow(new RuntimeException("Access denied"))
                .when(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);

        assertThatThrownBy(() -> service.getPendingPrintCount(dto, userId, userRole))
                .hasMessage("Access denied");
    }

    @Test
    void countLabelsByPeriodAndWarehouse() {
        when(persistence.countByPeriodIdAndWarehouseId(periodId, warehouseId))
                .thenReturn(100L);

        long result = service.countLabelsByPeriodAndWarehouse(periodId, warehouseId);

        assertThat(result).isEqualTo(100L);
        verify(persistence).countByPeriodIdAndWarehouseId(periodId, warehouseId);
    }

    @Test
    void countLabelsByPeriodAndWarehouse_empty() {
        when(persistence.countByPeriodIdAndWarehouseId(periodId, warehouseId))
                .thenReturn(0L);

        long result = service.countLabelsByPeriodAndWarehouse(periodId, warehouseId);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void getLabelStatus_returns_correct_data() {
        Label label = new Label();
        label.setFolio(folio);
        label.setPeriodId(periodId);
        label.setWarehouseId(warehouseId);
        label.setEstado(Label.State.IMPRESO);

        when(persistence.findByFolioAndPeriodId(folio, periodId))
                .thenReturn(Optional.of(label));
        when(persistence.countEventsForFolio(folio))
                .thenReturn(0L);

        LabelStatusResponseDTO result = service.getLabelStatus(folio, periodId, warehouseId, userId, userRole);

        assertThat(result).isNotNull();
        assertThat(result.getFolio()).isEqualTo(folio);
        assertThat(result.getEstado()).isEqualTo("IMPRESO");
    }

    @Test
    void getLabelStatus_throws_when_not_found() {
        when(persistence.findByFolioAndPeriodId(folio, periodId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLabelStatus(folio, periodId, warehouseId, userId, userRole))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    void getLabelsByProduct_happy_path() {
        Label label1 = new Label();
        label1.setFolio(1L);
        label1.setProductId(productId);

        Label label2 = new Label();
        label2.setFolio(2L);
        label2.setProductId(productId);

        when(persistence.findByProductPeriodWarehouse(productId, periodId, warehouseId))
                .thenReturn(List.of(label1, label2));
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(createProductEntity(productId, "PROD001", "Producto 1")));

        List<LabelDetailDTO> results = service.getLabelsByProduct(productId, periodId, warehouseId, userId, userRole);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getProductId()).isEqualTo(productId);
        verify(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);
    }

    @Test
    void getLabelsByProduct_empty_results() {
        when(persistence.findByProductPeriodWarehouse(productId, periodId, warehouseId))
                .thenReturn(Collections.emptyList());

        List<LabelDetailDTO> results = service.getLabelsByProduct(productId, periodId, warehouseId, userId, userRole);

        assertThat(results).isEmpty();
    }

    @Test
    void getLabelForCount_happy_path() {
        Label label = new Label();
        label.setFolio(folio);
        label.setPeriodId(periodId);
        label.setWarehouseId(warehouseId);
        label.setProductId(productId);
        label.setEstado(Label.State.IMPRESO);

        when(persistence.findByFolioAndPeriodId(folio, periodId))
                .thenReturn(Optional.of(label));
        when(persistence.findAllCountEvents(folio))
                .thenReturn(Collections.emptyList());
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(createProductEntity(productId, "PROD001", "Producto 1")));

        LabelForCountDTO result = service.getLabelForCount(folio, periodId, warehouseId, userId, userRole);

        assertThat(result).isNotNull();
        assertThat(result.getFolio()).isEqualTo(folio);
        assertThat(result.getEstado()).isEqualTo("IMPRESO");
    }

    @Test
    void getLabelForCount_throws_when_not_found() {
        when(persistence.findByFolioAndPeriodId(folio, periodId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLabelForCount(folio, periodId, warehouseId, userId, userRole))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    void getLabelForCount_bypassess_warehouse_validation_for_auxiliar_de_conteo() {
        Label label = new Label();
        label.setFolio(folio);
        label.setPeriodId(periodId);
        label.setWarehouseId(warehouseId);
        label.setProductId(productId);

        when(warehouseAccessService.hasFullAccess("AUXILIAR_DE_CONTEO"))
                .thenReturn(true);
        when(persistence.findByFolioAndPeriodId(folio, periodId))
                .thenReturn(Optional.of(label));
        when(persistence.findAllCountEvents(folio))
                .thenReturn(Collections.emptyList());
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(createProductEntity(productId, "PROD001", "Producto 1")));

        LabelForCountDTO result = service.getLabelForCount(folio, periodId, warehouseId, userId, "AUXILIAR_DE_CONTEO");

        assertThat(result).isNotNull();
        verify(warehouseAccessService, never()).validateWarehouseAccess(anyLong(), anyLong(), anyString());
    }

    // Helper methods
    private WarehouseEntity createWarehouseEntity(Long id, String name) {
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setIdWarehouse(id);
        warehouse.setNameWarehouse(name);
        return warehouse;
    }

    private ProductEntity createProductEntity(Long id, String code, String name) {
        ProductEntity product = new ProductEntity();
        product.setIdProduct(id);
        product.setClaveProducto(code);
        product.setNombreProducto(name);
        return product;
    }
}
