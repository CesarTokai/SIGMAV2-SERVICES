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
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
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
class LabelReportIntegrationTest {

    @Autowired private LabelService labelService;
    @MockitoBean private WarehouseAccessService warehouseAccessService;

    @BeforeEach
    void setUp() {
        // Mock security
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ───────────────────── DISTRIBUTION REPORT ─────────────────────

    @Test
    void getDistributionReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getDistributionReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getDistributionReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getDistributionReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getDistributionReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getDistributionReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getDistributionReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getDistributionReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

    // ───────────────────── LABEL LIST REPORT ─────────────────────

    @Test
    void getLabelListReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getLabelListReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getLabelListReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getLabelListReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getLabelListReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getLabelListReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getLabelListReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getLabelListReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

    // ───────────────────── PENDING LABELS REPORT ─────────────────────

    @Test
    void getPendingLabelsReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getPendingLabelsReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getPendingLabelsReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getPendingLabelsReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getPendingLabelsReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getPendingLabelsReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getPendingLabelsReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getPendingLabelsReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

    // ───────────────────── DIFFERENCES REPORT ─────────────────────

    @Test
    void getDifferencesReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getDifferencesReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getDifferencesReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getDifferencesReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getDifferencesReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getDifferencesReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getDifferencesReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getDifferencesReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

    // ───────────────────── CANCELLED LABELS REPORT ─────────────────────

    @Test
    void getCancelledLabelsReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getCancelledLabelsReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getCancelledLabelsReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getCancelledLabelsReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getCancelledLabelsReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getCancelledLabelsReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getCancelledLabelsReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getCancelledLabelsReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

    // ───────────────────── COMPARATIVE REPORT ─────────────────────

    @Test
    void getComparativeReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getComparativeReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getComparativeReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getComparativeReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getComparativeReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getComparativeReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getComparativeReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getComparativeReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

    // ───────────────────── WAREHOUSE DETAIL REPORT ─────────────────────

    @Test
    void getWarehouseDetailReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getWarehouseDetailReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getWarehouseDetailReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getWarehouseDetailReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getWarehouseDetailReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getWarehouseDetailReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getWarehouseDetailReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getWarehouseDetailReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

    // ───────────────────── PRODUCT DETAIL REPORT ─────────────────────

    @Test
    void getProductDetailReport_success_empty() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getProductDetailReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getProductDetailReport_throws_when_warehouse_access_denied() {
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(1L);

        assertThatThrownBy(() -> labelService.getProductDetailReport(filter, 1L, "AUXILIAR"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void getProductDetailReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        List<?> result = labelService.getProductDetailReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getProductDetailReport_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            ReportFilterDTO filter = new ReportFilterDTO();
            filter.setPeriodId(1L);
            filter.setWarehouseId(1L);

            List<?> result = labelService.getProductDetailReport(filter, 1L, role);

            assertThat(result).isNotNull();
        }
    }

}
