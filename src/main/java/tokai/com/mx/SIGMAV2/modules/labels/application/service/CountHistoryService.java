package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.CountHistory;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.CountHistoryRepository;

import java.time.LocalDateTime;

/**
 * Servicio para registrar automáticamente en el historial de conteos
 * cada vez que se registra o actualiza un conteo C1/C2
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CountHistoryService {

    private final CountHistoryRepository countHistoryRepository;

    /**
     * Registra un nuevo conteo en el historial
     * @param userId ID del usuario que registra el conteo
     * @param email Email del usuario
     * @param folio Folio del marbete
     * @param countType Tipo de conteo (1 para C1, 2 para C2)
     * @param countValue Valor del conteo
     * @param role Rol del usuario al momento del registro
     * @param warehouseId ID del almacén
     * @param periodId ID del período
     */
    @Transactional
    public void recordCountRegistration(
            Long userId,
            String email,
            Long folio,
            Integer countType,
            Integer countValue,
            String role,
            Long warehouseId,
            Long periodId
    ) {
        try {
            CountHistory history = CountHistory.builder()
                    .userId(userId)
                    .email(email != null ? email : "DESCONOCIDO")
                    .fullName(email)
                    .folio(folio)
                    .countType(countType == 1 ? 1L : 2L) // 1 para C1, 2 para C2
                    .countValue(countValue)
                    .status("REGISTRADO")
                    .role(role)
                    .warehouseId(warehouseId)
                    .periodId(periodId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            countHistoryRepository.save(history);

        } catch (Exception ignored) {

        }
    }

    /**
     * Registra una actualización de conteo en el historial
     * @param userId ID del usuario que actualiza el conteo
     * @param email Email del usuario
     * @param folio Folio del marbete
     * @param countType Tipo de conteo (1 para C1, 2 para C2)
     * @param newValue Nuevo valor del conteo
     * @param previousValue Valor anterior
     * @param role Rol del usuario al momento de la actualización
     * @param warehouseId ID del almacén
     * @param periodId ID del período
     */
    @Transactional
    public void recordCountUpdate(
            Long userId,
            String email,
            Long folio,
            Integer countType,
            Integer newValue,
            Integer previousValue,
            String role,
            Long warehouseId,
            Long periodId
    ) {
        try {
            CountHistory history = CountHistory.builder()
                    .userId(userId)
                    .email(email != null ? email : "DESCONOCIDO")
                    .fullName(email)
                    .folio(folio)
                    .countType(countType == 1 ? 1L : 2L)
                    .countValue(newValue)
                    .status("ACTUALIZADO")
                    .role(role)
                    .warehouseId(warehouseId)
                    .periodId(periodId)
                    .description(String.format("Actualizado de %d a %d", previousValue, newValue))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            countHistoryRepository.save(history);
            log.info("Actualización de conteo registrada: Usuario={} ({}), Folio={}, Tipo=C{}, Anterior={}, Nuevo={}", 
                    userId, email, folio, countType, previousValue, newValue);
        } catch (Exception e) {
            log.error("Error al registrar actualización de conteo en historial", e);
            // No lanzamos la excepción para no afectar el flujo principal
        }
    }
}

