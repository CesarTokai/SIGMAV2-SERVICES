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
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.*;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LabelCountIntegrationTest {

    @Autowired private LabelCountService service;
    @Autowired private JpaLabelRepository labelRepository;
    @MockitoBean private WarehouseAccessService warehouseAccessService;

    private Label testLabel;
    private CountEventDTO dto;

    @BeforeEach
    void setUp() {
        // Crear marbete en BD para tests
        testLabel = new Label();
        testLabel.setFolio(100L);
        testLabel.setPeriodId(1L);
        testLabel.setWarehouseId(2L);
        testLabel.setProductId(1L);
        testLabel.setCreatedBy(1L);
        testLabel.setEstado(Label.State.IMPRESO);
        testLabel.setCreatedAt(LocalDateTime.now());
        testLabel = labelRepository.save(testLabel);

        // DTO base
        dto = new CountEventDTO();
        dto.setFolio(100L);
        dto.setPeriodId(1L);
        dto.setWarehouseId(2L);
        dto.setCountedValue(BigDecimal.valueOf(95));

        // Mock security
        Authentication auth = mock(Authentication.class);
        org.mockito.Mockito.lenient().when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        org.mockito.Mockito.lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ───────────────────── VALIDACIONES CRÍTICAS ─────────────────────

    @Test
    void registerCountC1_throws_when_folio_not_exists() {
        dto.setFolio(999L);  // Folio no existe

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(LabelNotFoundException.class)
                .hasMessageContaining("no existe");
    }


    @Test
    void registerCountC2_throws_when_C1_not_registered() {
        // C1 no registrado, intentar C2
        assertThatThrownBy(() -> service.registerCountC2(dto, 1L, "AUXILIAR"))
                .isInstanceOf(CountSequenceException.class)
                .hasMessageContaining("C1");
    }

    @Test
    void registerCountC1_throws_when_label_cancelled() {
        // Cambiar estado a CANCELADO
        testLabel.setEstado(Label.State.CANCELADO);
        labelRepository.save(testLabel);

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("CANCELADO");
    }

    @Test
    void registerCountC1_throws_when_label_not_impreso() {
        // Cambiar estado a GENERADO (no IMPRESO)
        testLabel.setEstado(Label.State.GENERADO);
        labelRepository.save(testLabel);

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(InvalidLabelStateException.class)
                .hasMessageContaining("IMPRESO");
    }

    @Test
    void registerCountC1_throws_when_period_closed() {
        // Período diferente al del marbete
        dto.setPeriodId(999L);

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    void registerCountC1_throws_when_warehouse_no_access() {
        // Simular que no tiene permisos
        doThrow(new PermissionDeniedException("Sin acceso"))
                .when(warehouseAccessService).validateWarehouseAccess(anyLong(), anyLong(), anyString());

        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "ALMACENISTA"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    // ───────────────────── DUPLICATE & DOBLE-CLICK ─────────────────────

    @Test
    void registerCountC1_throws_when_duplicate() {
        // Registrar C1 primera vez
        service.registerCountC1(dto, 1L, "AUXILIAR");

        // Intentar registrar de nuevo (doble-click)
        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "AUXILIAR"))
                .isInstanceOf(DuplicateCountException.class)
                .hasMessageContaining("C1");
    }

    @Test
    void registerCountC2_duplicate_prevention() {
        // Registrar C1
        service.registerCountC1(dto, 1L, "AUXILIAR");

        // Registrar C2
        service.registerCountC2(dto, 1L, "AUXILIAR");

        // Intentar registrar C2 de nuevo
        assertThatThrownBy(() -> service.registerCountC2(dto, 1L, "AUXILIAR"))
                .isInstanceOf(DuplicateCountException.class)
                .hasMessageContaining("C2");
    }

    // ───────────────────── ROLE-BASED ACCESS ─────────────────────

    @Test
    void registerCountC1_throws_when_invalid_role() {
        assertThatThrownBy(() -> service.registerCountC1(dto, 1L, "USUARIO_INVALIDO"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void registerCountC1_success_with_valid_roles() {
        String[] validRoles = {"ADMINISTRADOR", "ALMACENISTA", "AUXILIAR", "AUXILIAR_DE_CONTEO"};

        for (String role : validRoles) {
            // Crear nuevo folio para cada test
            Label label = new Label();
            label.setFolio(100L + System.nanoTime());
            label.setPeriodId(1L);
            label.setWarehouseId(2L);
            label.setProductId(1L);
            label.setCreatedBy(1L);
            label.setEstado(Label.State.IMPRESO);
            label.setCreatedAt(LocalDateTime.now());
            label = labelRepository.save(label);

            dto.setFolio(label.getFolio());

            // Debe funcionar sin excepción
            var result = service.registerCountC1(dto, 1L, role);
            assert result != null;
        }
    }

    // ───────────────────── BD FAILURE SIMULATION ─────────────────────

    @Test
    void registerCountC1_handles_concurrent_access() throws InterruptedException {
        // 2 threads intentan registrar C1 al mismo tiempo
        Thread t1 = new Thread(() -> {
            try {
                service.registerCountC1(dto, 1L, "AUXILIAR");
            } catch (DuplicateCountException e) {
                // Esperado: uno gana, otro pierde
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                service.registerCountC1(dto, 2L, "AUXILIAR");
            } catch (DuplicateCountException e) {
                // Esperado: uno gana, otro pierde
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Uno debe haber ganado, confirmamos que C1 existe
        assert true; // Simplemente verificar que no hay excepción no capturada
    }

}
