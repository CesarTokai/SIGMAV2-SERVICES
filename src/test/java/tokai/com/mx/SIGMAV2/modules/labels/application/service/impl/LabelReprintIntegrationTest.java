package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LabelReprintIntegrationTest {

    @Autowired private LabelService labelService;
    @MockBean private WarehouseAccessService warehouseAccessService;

    @BeforeEach
    void setUp() {
        // Mock security
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ───────────────────── EXTRAORDINARY REPRINT ─────────────────────

    @Test
    void extraordinaryReprint_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        PrintRequestDTO dto = new PrintRequestDTO();
        dto.setPeriodId(1L);
        dto.setWarehouseId(1L);
        dto.setFolios(List.of(100L));

        assertThatThrownBy(() -> labelService.extraordinaryReprint(dto, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    // ───────────────────── GET PRINTED LABEL PDF ─────────────────────

    @Test
    void getPrintedLabelPdf_throws_when_label_not_found() {
        assertThatThrownBy(() -> labelService.getPrintedLabelPdf(999L, 1L, 1L, "AUXILIAR"))
                .isInstanceOf(LabelNotFoundException.class);
    }

    // ───────────────────── REPRINT SIMPLE ─────────────────────

    @Test
    void reprintSimple_throws_when_label_not_found() {
        assertThatThrownBy(() -> labelService.reprintSimple(999L, 1L, 1L, "AUXILIAR"))
                .isInstanceOf(LabelNotFoundException.class);
    }

}
