package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaInventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CancelLabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCancelledStockDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.exception.LabelAlreadyCancelledException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCancelled;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCancelledRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRequestRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelCancelServiceTest {

    @Mock LabelsPersistenceAdapter persistence;
    @Mock WarehouseAccessService warehouseAccessService;
    @Mock JpaProductRepository productRepository;
    @Mock JpaWarehouseRepository warehouseRepository;
    @Mock JpaInventoryStockRepository inventoryStockRepository;
    @Mock JpaLabelRequestRepository labelRequestRepository;
    @Mock JpaLabelRepository jpaLabelRepository;
    @Mock JpaLabelCancelledRepository jpaLabelCancelledRepository;
    @Mock JpaLabelCountEventRepository jpaLabelCountEventRepository;

    @InjectMocks LabelCancelService service;

    private Label impressedLabel;

    @BeforeEach
    void setUp() {
        impressedLabel = new Label();
        impressedLabel.setFolio(100L);
        impressedLabel.setPeriodId(1L);
        impressedLabel.setWarehouseId(2L);
        impressedLabel.setProductId(3L);
        impressedLabel.setEstado(Label.State.IMPRESO);
        impressedLabel.setCreatedBy(1L);
    }

    // ── cancelLabel ────────────────────────────────────────────────────────

    @Test
    void cancelLabel_changes_state_to_cancelled() {
        CancelLabelRequestDTO dto = new CancelLabelRequestDTO();
        dto.setFolio(100L);
        dto.setMotivoCancelacion("Prueba");

        when(jpaLabelRepository.findById(100L)).thenReturn(Optional.of(impressedLabel));
        when(jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(100L)).thenReturn(List.of());
        when(inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        service.cancelLabel(dto, 1L, "ADMINISTRADOR");

        verify(jpaLabelRepository).save(argThat(l -> l.getEstado() == Label.State.CANCELADO));
        verify(jpaLabelCancelledRepository).save(any(LabelCancelled.class));
    }

    @Test
    void cancelLabel_throws_when_already_cancelled() {
        impressedLabel.setEstado(Label.State.CANCELADO);
        CancelLabelRequestDTO dto = new CancelLabelRequestDTO();
        dto.setFolio(100L);

        when(jpaLabelRepository.findById(100L)).thenReturn(Optional.of(impressedLabel));

        assertThatThrownBy(() -> service.cancelLabel(dto, 1L, "ADMINISTRADOR"))
                .isInstanceOf(LabelAlreadyCancelledException.class);
    }

    @Test
    void cancelLabel_throws_when_folio_not_found() {
        CancelLabelRequestDTO dto = new CancelLabelRequestDTO();
        dto.setFolio(999L);

        when(jpaLabelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelLabel(dto, 1L, "ADMINISTRADOR"))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    void cancelLabel_throws_when_folio_null() {
        CancelLabelRequestDTO dto = new CancelLabelRequestDTO();
        dto.setFolio(null);

        assertThatThrownBy(() -> service.cancelLabel(dto, 1L, "ADMINISTRADOR"))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("folio");
    }

    @Test
    void cancelLabel_saves_cancelled_with_reason() {
        CancelLabelRequestDTO dto = new CancelLabelRequestDTO();
        dto.setFolio(100L);
        dto.setMotivoCancelacion("Producto dañado");

        when(jpaLabelRepository.findById(100L)).thenReturn(Optional.of(impressedLabel));
        when(jpaLabelCountEventRepository.findByFolioOrderByCreatedAtAsc(100L)).thenReturn(List.of());
        when(inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        service.cancelLabel(dto, 1L, "ADMINISTRADOR");

        ArgumentCaptor<LabelCancelled> captor = ArgumentCaptor.forClass(LabelCancelled.class);
        verify(jpaLabelCancelledRepository).save(captor.capture());
        assertThat(captor.getValue().getMotivoCancelacion()).isEqualTo("Producto dañado");
    }

    // ── updateCancelledStock ───────────────────────────────────────────────

    @Test
    void updateCancelledStock_reactivates_when_existencias_positive() {
        UpdateCancelledStockDTO dto = new UpdateCancelledStockDTO();
        dto.setFolio(100L);
        dto.setExistenciasActuales(5);

        LabelCancelled cancelled = new LabelCancelled();
        cancelled.setFolio(100L);
        cancelled.setWarehouseId(2L);
        cancelled.setProductId(3L);
        cancelled.setPeriodId(1L);
        cancelled.setReactivado(false);

        when(persistence.findCancelledByFolio(100L)).thenReturn(Optional.of(cancelled));
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(warehouseRepository.findById(anyLong())).thenReturn(Optional.empty());

        service.updateCancelledStock(dto, 1L, "ADMINISTRADOR");

        assertThat(cancelled.getReactivado()).isTrue();
        verify(persistence).save(any(Label.class));
        verify(persistence).saveCancelled(cancelled);
    }

    @Test
    void updateCancelledStock_does_not_reactivate_when_existencias_zero() {
        UpdateCancelledStockDTO dto = new UpdateCancelledStockDTO();
        dto.setFolio(100L);
        dto.setExistenciasActuales(0);

        LabelCancelled cancelled = new LabelCancelled();
        cancelled.setFolio(100L);
        cancelled.setWarehouseId(2L);
        cancelled.setProductId(3L);
        cancelled.setPeriodId(1L);
        cancelled.setReactivado(false);

        when(persistence.findCancelledByFolio(100L)).thenReturn(Optional.of(cancelled));
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(warehouseRepository.findById(anyLong())).thenReturn(Optional.empty());

        service.updateCancelledStock(dto, 1L, "ADMINISTRADOR");

        assertThat(cancelled.getReactivado()).isFalse();
        verify(persistence, never()).save(any(Label.class));
    }

    @Test
    void updateCancelledStock_throws_when_cancelled_not_found() {
        UpdateCancelledStockDTO dto = new UpdateCancelledStockDTO();
        dto.setFolio(999L);

        when(persistence.findCancelledByFolio(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCancelledStock(dto, 1L, "ADMINISTRADOR"))
                .isInstanceOf(LabelNotFoundException.class);
    }
}
