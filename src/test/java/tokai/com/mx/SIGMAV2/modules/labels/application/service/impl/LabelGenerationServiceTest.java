package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelRequest;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelGenerationServiceTest {

    @Mock LabelsPersistenceAdapter persistence;
    @Mock WarehouseAccessService warehouseAccessService;
    @Mock JpaProductRepository productRepository;

    @InjectMocks LabelGenerationService service;

    private GenerateBatchListDTO dto;
    private Long periodId = 1L;
    private Long warehouseId = 10L;
    private Long productId = 100L;
    private Long userId = 1L;
    private String userRole = "ALMACENISTA";

    @BeforeEach
    void setUp() {
        dto = new GenerateBatchListDTO();
        dto.setPeriodId(periodId);
        dto.setWarehouseId(warehouseId);

        ProductBatchDTO product = new ProductBatchDTO();
        product.setProductId(productId);
        product.setLabelsToGenerate(5);
        product.setComment("Test comment");
        dto.setProducts(List.of(product));
    }

    @Test
    void generateBatchList_happy_path() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.empty());
        when(persistence.allocateFolioRange(periodId, 5))
                .thenReturn(new long[]{1L, 5L});

        LabelRequest savedRequest = new LabelRequest();
        savedRequest.setIdLabelRequest(1L);
        when(persistence.save(any(LabelRequest.class))).thenReturn(savedRequest);

        service.generateBatchList(dto, userId, userRole);

        verify(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);
        verify(productRepository).existsById(productId);
        verify(persistence).allocateFolioRange(periodId, 5);
        verify(persistence).saveAll(argThat(list -> list.size() == 5 && list.get(0).getEstado() == Label.State.GENERADO));
    }

    @Test
    void generateBatchList_fails_when_all_products_already_generated() {
        LabelRequest existing = new LabelRequest();
        existing.setIdLabelRequest(1L);
        existing.setFoliosGenerados(5);

        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.generateBatchList(dto, userId, userRole))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("Todos los productos ya tienen folios generados");

        verify(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);
    }

    @Test
    void generateBatchList_fails_when_product_not_found() {
        when(productRepository.existsById(productId)).thenReturn(false);
        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateBatchList(dto, userId, userRole))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("no existen en el catálogo");

        verify(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);
        verify(productRepository).existsById(productId);
    }

    @Test
    void generateBatchList_warehouse_access_validation() {
        doThrow(new RuntimeException("Access denied"))
                .when(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);

        assertThatThrownBy(() -> service.generateBatchList(dto, userId, userRole))
                .hasMessage("Access denied");

        verify(warehouseAccessService).validateWarehouseAccess(userId, warehouseId, userRole);
        verifyNoMoreInteractions(persistence, productRepository);
    }

    @Test
    void generateBatchList_reuses_existing_label_request() {
        LabelRequest existing = new LabelRequest();
        existing.setIdLabelRequest(1L);
        existing.setRequestedLabels(10);
        existing.setFoliosGenerados(0);

        when(productRepository.existsById(productId)).thenReturn(true);
        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.of(existing));
        when(persistence.allocateFolioRange(periodId, 5))
                .thenReturn(new long[]{1L, 5L});

        service.generateBatchList(dto, userId, userRole);

        verify(persistence).save(argThat(lr -> lr.getIdLabelRequest().equals(1L) && lr.getFoliosGenerados() == 5));
        verify(persistence).saveAll(argThat(list -> list.size() == 5));
    }

    @Test
    void generateBatchList_creates_new_label_request_when_none_exists() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.empty());
        when(persistence.allocateFolioRange(periodId, 5))
                .thenReturn(new long[]{1L, 5L});

        LabelRequest saved = new LabelRequest();
        saved.setIdLabelRequest(1L);
        when(persistence.save(any(LabelRequest.class))).thenReturn(saved);

        service.generateBatchList(dto, userId, userRole);

        ArgumentCaptor<LabelRequest> captor = ArgumentCaptor.forClass(LabelRequest.class);
        verify(persistence).save(captor.capture());

        LabelRequest created = captor.getValue();
        assertThat(created.getProductId()).isEqualTo(productId);
        assertThat(created.getWarehouseId()).isEqualTo(warehouseId);
        assertThat(created.getPeriodId()).isEqualTo(periodId);
        assertThat(created.getRequestedLabels()).isEqualTo(5);
        assertThat(created.getFoliosGenerados()).isEqualTo(5);
    }

    @Test
    void generateBatchList_assigns_folios_correctly() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.empty());
        when(persistence.allocateFolioRange(periodId, 5))
                .thenReturn(new long[]{100L, 104L});

        LabelRequest saved = new LabelRequest();
        saved.setIdLabelRequest(1L);
        when(persistence.save(any(LabelRequest.class))).thenReturn(saved);

        service.generateBatchList(dto, userId, userRole);

        ArgumentCaptor<List<Label>> captor = ArgumentCaptor.forClass(List.class);
        verify(persistence).saveAll(captor.capture());

        List<Label> labels = captor.getValue();
        assertThat(labels).hasSize(5);
        assertThat(labels.get(0).getFolio()).isEqualTo(100L);
        assertThat(labels.get(4).getFolio()).isEqualTo(104L);
        assertThat(labels).allMatch(l -> l.getEstado() == Label.State.GENERADO);
        assertThat(labels).allMatch(l -> l.getPeriodId().equals(periodId));
        assertThat(labels).allMatch(l -> l.getWarehouseId().equals(warehouseId));
        assertThat(labels).allMatch(l -> l.getProductId().equals(productId));
    }

    @Test
    void generateBatchList_preserves_individual_label_comment() {
        when(productRepository.existsById(productId)).thenReturn(true);
        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.empty());
        when(persistence.allocateFolioRange(periodId, 5))
                .thenReturn(new long[]{1L, 5L});

        LabelRequest saved = new LabelRequest();
        saved.setIdLabelRequest(1L);
        when(persistence.save(any(LabelRequest.class))).thenReturn(saved);

        service.generateBatchList(dto, userId, userRole);

        ArgumentCaptor<List<Label>> captor = ArgumentCaptor.forClass(List.class);
        verify(persistence).saveAll(captor.capture());

        List<Label> labels = captor.getValue();
        assertThat(labels).allMatch(l -> l.getComment().equals("Test comment"));
    }

    @Test
    void generateBatchList_multiple_products() {
        Long productId2 = 200L;
        ProductBatchDTO product2 = new ProductBatchDTO();
        product2.setProductId(productId2);
        product2.setLabelsToGenerate(3);
        product2.setComment("Comment 2");

        dto.setProducts(List.of(
                dto.getProducts().get(0),
                product2
        ));

        when(productRepository.existsById(productId)).thenReturn(true);
        when(productRepository.existsById(productId2)).thenReturn(true);
        when(persistence.findByProductWarehousePeriod(productId, warehouseId, periodId))
                .thenReturn(Optional.empty());
        when(persistence.findByProductWarehousePeriod(productId2, warehouseId, periodId))
                .thenReturn(Optional.empty());
        when(persistence.allocateFolioRange(periodId, 5))
                .thenReturn(new long[]{1L, 5L});
        when(persistence.allocateFolioRange(periodId, 3))
                .thenReturn(new long[]{6L, 8L});

        LabelRequest saved1 = new LabelRequest();
        saved1.setIdLabelRequest(1L);
        LabelRequest saved2 = new LabelRequest();
        saved2.setIdLabelRequest(2L);
        when(persistence.save(any(LabelRequest.class))).thenReturn(saved1, saved2);

        service.generateBatchList(dto, userId, userRole);

        verify(persistence, times(2)).save(any(LabelRequest.class));
        verify(persistence, times(2)).saveAll(any());
    }
}
