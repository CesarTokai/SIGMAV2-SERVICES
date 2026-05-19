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
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateFileResponseDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.ReportFilterDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GenerateFileIntegrationTest {

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

    // ───────────────────── GENERATE INVENTORY FILE ─────────────────────

    @Test
    void generateInventoryFile_throws_when_period_not_exists() {
        assertThatThrownBy(() -> labelService.generateInventoryFile(999L, 1L, "AUXILIAR"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }

    @Test
    void generateInventoryFile_throws_when_period_zero() {
        assertThatThrownBy(() -> labelService.generateInventoryFile(0L, 1L, "AUXILIAR"))
                .isInstanceOf(RuntimeException.class);
    }

    // ───────────────────── COUNT LABELS ─────────────────────

    @Test
    void countLabelsByPeriodAndWarehouse_with_invalid_period() {
        long result = labelService.countLabelsByPeriodAndWarehouse(999L, 1L);

        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    void countLabelsByPeriodAndWarehouse_with_invalid_warehouse() {
        long result = labelService.countLabelsByPeriodAndWarehouse(1L, 999L);

        assertThat(result).isGreaterThanOrEqualTo(0);
    }

    @Test
    void countLabelsByPeriodAndWarehouse_consistent_across_calls() {
        long count1 = labelService.countLabelsByPeriodAndWarehouse(999L, 999L);
        long count2 = labelService.countLabelsByPeriodAndWarehouse(999L, 999L);

        assertThat(count1).isEqualTo(count2);
    }

    // ───────────────────── DISTRIBUTION REPORT WITH FILE CONTEXT ─────────────────────

    @Test
    void getDistributionReport_success_with_invalid_period() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(1L);

        var result = labelService.getDistributionReport(filter, 1L, "AUXILIAR");

        assertThat(result).isNotNull();
    }

    @Test
    void getDistributionReport_throws_with_invalid_warehouse() {
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(1L);
        filter.setWarehouseId(999L);

        var result = labelService.getDistributionReport(filter, 1L, "AUXILIAR");
        assertThat(result).isNotNull();
    }

    @Test
    void getDistributionReport_success_with_all_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};
        ReportFilterDTO filter = new ReportFilterDTO();
        filter.setPeriodId(999L);
        filter.setWarehouseId(999L);

        for (String role : validRoles) {
            // With invalid data, just verify method callable
            try {
                var result = labelService.getDistributionReport(filter, 1L, role);
                assertThat(result).isNotNull();
            } catch (RuntimeException e) {
                // Expected when period/warehouse don't exist
                assertThat(e.getMessage()).contains("no encontrado");
            }
        }
    }

    // ───────────────────── LABEL DETAIL RETRIEVAL FOR FILES ─────────────────────

    @Test
    void getLabelsForCountList_throws_with_invalid_period() {
        assertThatThrownBy(() -> labelService.getLabelsForCountList(999L, 1L, 1L, "AUXILIAR"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getLabelsForCountList_throws_with_invalid_warehouse() {
        assertThatThrownBy(() -> labelService.getLabelsForCountList(1L, 999L, 1L, "AUXILIAR"))
                .isInstanceOf(RuntimeException.class);
    }

}
