package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.CountSequenceException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.DuplicateCountException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.CountHistoryService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelCountEventRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;
import tokai.com.mx.SIGMAV2.modules.periods.adapter.persistence.JpaPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabelCountServiceTest {

    @Mock LabelsPersistenceAdapter persistence;
    @Mock WarehouseAccessService warehouseAccessService;
    @Mock JpaLabelCountEventRepository jpaLabelCountEventRepository;
    @Mock JpaPeriodRepository jpaPeriodRepository;
    @Mock JpaWarehouseRepository warehouseRepository;
    @Mock CountHistoryService countHistoryService;

    @InjectMocks LabelCountService service;

    private Label impressedLabel;
    private CountEventDTO dto;

    @BeforeEach
    void setUp() {
        impressedLabel = new Label();
        impressedLabel.setFolio(100L);
        impressedLabel.setPeriodId(1L);
        impressedLabel.setWarehouseId(2L);
        impressedLabel.setEstado(Label.State.IMPRESO);

        dto = new CountEventDTO();
        dto.setFolio(100L);
        dto.setPeriodId(1L);
        dto.setWarehouseId(2L);
        dto.setCountedValue(BigDecimal.TEN);

        // Solo el happy path llega a countHistoryService.recordCountRegistration() —
        // tests que tiran excepción antes no consumen estos stubs → lenient
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── registerCountC1 ────────────────────────────────────────────────────

    @Test
    void registerCountC1_success() {
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));
        when(persistence.hasCountNumber(100L, 1)).thenReturn(false);
        when(persistence.hasCountNumber(100L, 2)).thenReturn(false);
        LabelCountEvent event = new LabelCountEvent();
        when(persistence.saveCountEvent(anyLong(), anyLong(), eq(1), any(), any(), anyBoolean())).thenReturn(event);

        service.registerCountC1(dto, 1L, "AUXILIAR");

        verify(persistence).saveCountEvent(eq(100L), eq(1L), eq(1), eq(BigDecimal.TEN), any(), anyBoolean());
    }

    @Test
    void registerCountC1_throws_when_C1_already_exists() {
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));
        when(persistence.hasCountNumber(100L, 1)).thenReturn(true);

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(DuplicateCountException.class)
                .hasMessageContaining("C1 ya fue registrado");
    }

    @Test
    void registerCountC1_throws_when_C2_exists_before_C1() {
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));
        when(persistence.hasCountNumber(100L, 1)).thenReturn(false);
        when(persistence.hasCountNumber(100L, 2)).thenReturn(true);

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(CountSequenceException.class)
                .hasMessageContaining("C2");
    }

    @Test
    void registerCountC1_throws_when_label_not_found() {
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    void registerCountC1_throws_when_label_not_impreso() {
        impressedLabel.setEstado(Label.State.GENERADO);
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("IMPRESO");
    }

    @Test
    void registerCountC1_throws_when_label_is_cancelled() {
        impressedLabel.setEstado(Label.State.CANCELADO);
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("CANCELADO");
    }

    @Test
    void registerCountC1_throws_when_role_not_allowed() {
        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "USUARIO"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void registerCountC1_throws_when_role_null() {
        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, null))
                .isInstanceOf(PermissionDeniedException.class);
    }

    // ── registerCountC2 ────────────────────────────────────────────────────

    @Test
    void registerCountC2_success() {
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));
        when(persistence.hasCountNumber(100L, 1)).thenReturn(true);
        when(persistence.hasCountNumber(100L, 2)).thenReturn(false);
        LabelCountEvent event = new LabelCountEvent();
        when(persistence.saveCountEvent(anyLong(), anyLong(), eq(2), any(), any(), anyBoolean())).thenReturn(event);

        service.registerCountC2(dto, 1L, "AUXILIAR");

        verify(persistence).saveCountEvent(eq(100L), eq(1L), eq(2), eq(BigDecimal.TEN), any(), anyBoolean());
    }

    @Test
    void registerCountC2_throws_when_C1_not_exists() {
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));
        when(persistence.hasCountNumber(100L, 1)).thenReturn(false);

        assertThatThrownBy(() -> service.registerCountC2(dto, 1L, "AUXILIAR"))
                .isInstanceOf(CountSequenceException.class)
                .hasMessageContaining("C1 previo");
    }

    @Test
    void registerCountC2_throws_when_C2_already_exists() {
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));
        when(persistence.hasCountNumber(100L, 1)).thenReturn(true);
        when(persistence.hasCountNumber(100L, 2)).thenReturn(true);

        assertThatThrownBy(() -> service.registerCountC2(dto, 1L, "AUXILIAR"))
                .isInstanceOf(DuplicateCountException.class)
                .hasMessageContaining("C2 ya fue registrado");
    }

    @Test
    void registerCountC2_AUXILIAR_DE_CONTEO_bypasses_warehouse_validation() {
        dto.setWarehouseId(null);
        when(persistence.findByFolioAndPeriodId(100L, 1L)).thenReturn(Optional.of(impressedLabel));
        when(persistence.hasCountNumber(100L, 1)).thenReturn(true);
        when(persistence.hasCountNumber(100L, 2)).thenReturn(false);
        LabelCountEvent event = new LabelCountEvent();
        when(persistence.saveCountEvent(anyLong(), anyLong(), eq(2), any(), any(), anyBoolean())).thenReturn(event);

        service.registerCountC2(dto, 1L, "AUXILIAR_DE_CONTEO");

        // No debe llamar a validateWarehouseAccess
        verify(warehouseAccessService, never()).validateWarehouseAccess(anyLong(), anyLong(), anyString());
    }

}
