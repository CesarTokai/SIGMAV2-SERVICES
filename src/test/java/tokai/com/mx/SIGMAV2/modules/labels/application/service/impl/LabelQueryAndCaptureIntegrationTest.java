package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.*;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.*;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LabelQueryAndCaptureIntegrationTest {

    @Autowired private LabelService labelService;
    @Autowired private LabelQueryService labelQueryService;
    @Autowired private LabelGenerationService labelGenerationService;
    @Autowired private LabelCancelService labelCancelService;
    @Autowired private JpaLabelRepository labelRepository;
    @Autowired private JpaLabelRequestRepository labelRequestRepository;
    @Autowired private JpaLabelCancelledRepository labelCancelledRepository;
    @Autowired private JpaWarehouseRepository warehouseRepository;
    @Autowired private JpaProductRepository productRepository;
    @MockitoBean private WarehouseAccessService warehouseAccessService;

    private Label testLabel;
    private LabelRequest testLabelRequest;
    private LabelCancelled testLabelCancelled;
    private Long testWarehouseId;
    private Long testProductId;
    private Long testProductId2;

    @BeforeEach
    void setUp() {
        // Usar almacén y periodo fijo que existen en BD de prueba
        testWarehouseId = 1L;
        testProductId = 1L;
        testProductId2 = 2L;

        // Mock security
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ───────────────────── SUMMARY (CONSULTA) ─────────────────────

    @Test
    void getLabelSummary_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        LabelSummaryRequestDTO dto = new LabelSummaryRequestDTO();
        dto.setPeriodId(1L);
        dto.setWarehouseId(testWarehouseId);

        assertThatThrownBy(() -> labelService.getLabelSummary(dto, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getLabelSummary_throws_when_period_not_exists() {
        LabelSummaryRequestDTO dto = new LabelSummaryRequestDTO();
        dto.setPeriodId(999L);
        dto.setWarehouseId(testWarehouseId);

        assertThatThrownBy(() -> labelService.getLabelSummary(dto, 1L, "AUXILIAR"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    // ───────────────────── GENERATE BATCH ─────────────────────

    @Test
    void generateBatchList_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        GenerateBatchListDTO dto = new GenerateBatchListDTO();
        dto.setPeriodId(1L);
        dto.setWarehouseId(testWarehouseId);
        dto.setProducts(List.of());

        assertThatThrownBy(() -> labelGenerationService.generateBatchList(dto, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void generateBatchList_throws_when_product_not_exists() {
        GenerateBatchListDTO dto = new GenerateBatchListDTO();
        dto.setPeriodId(1L);
        dto.setWarehouseId(testWarehouseId);
        GenerateBatchListDTO.ProductBatchDTO productBatch = new GenerateBatchListDTO.ProductBatchDTO();
        productBatch.setProductId(999L);
        productBatch.setLabelsToGenerate(5);
        dto.setProducts(List.of(productBatch));

        assertThatThrownBy(() -> labelGenerationService.generateBatchList(dto, 1L, "AUXILIAR"))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("no existen");
    }

    // ───────────────────── UPDATE CANCELLED STOCK ─────────────────────

    @Test
    void updateCancelledStock_throws_when_cancelled_not_exists() {
        UpdateCancelledStockDTO dto = new UpdateCancelledStockDTO();
        dto.setFolio(999L);
        dto.setPeriodId(1L);
        dto.setExistenciasActuales(0);

        assertThatThrownBy(() -> labelCancelService.updateCancelledStock(dto, 1L, "AUXILIAR"))
                .isInstanceOf(LabelNotFoundException.class)
                .hasMessageContaining("no encontrado");
    }

}
