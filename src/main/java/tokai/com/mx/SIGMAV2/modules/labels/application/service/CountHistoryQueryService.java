package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.labels.adapter.dto.CountHistoryResponse;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.mapper.CountHistoryMapper;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.CountHistoryRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para consultar el historial de conteos de usuarios
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CountHistoryQueryService {

    private final CountHistoryRepository countHistoryRepository;
    private final CountHistoryMapper countHistoryMapper;

    /**
     * Obtiene el historial de conteos de un usuario específico
     * @param userId ID del usuario
     * @param pageable Información de paginación
     * @return Página con el historial de conteos del usuario
     */
    public Page<CountHistoryResponse> getCountHistoryByUserId(Long userId, Pageable pageable) {
        log.debug("Consultando historial de conteos para usuario {}", userId);
        return countHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(countHistoryMapper::toResponse);
    }

    /**
     * Obtiene el historial de conteos de un almacén específico
     * @param warehouseId ID del almacén
     * @param pageable Información de paginación
     * @return Página con el historial de conteos del almacén
     */
    public Page<CountHistoryResponse> getCountHistoryByWarehouse(Long warehouseId, Pageable pageable) {
        log.debug("Consultando historial de conteos para almacén {}", warehouseId);
        return countHistoryRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId, pageable)
                .map(countHistoryMapper::toResponse);
    }

    /**
     * Obtiene el historial de conteos de un período específico
     * @param periodId ID del período
     * @param pageable Información de paginación
     * @return Página con el historial de conteos del período
     */
    public Page<CountHistoryResponse> getCountHistoryByPeriod(Long periodId, Pageable pageable) {
        log.debug("Consultando historial de conteos para período {}", periodId);
        return countHistoryRepository.findByPeriodIdOrderByCreatedAtDesc(periodId, pageable)
                .map(countHistoryMapper::toResponse);
    }

    /**
     * Obtiene el historial de conteos para un folio específico
     * @param folio Folio del marbete
     * @return Lista con el historial completo del folio (C1, C2, actualizaciones)
     */
    public List<CountHistoryResponse> getCountHistoryByFolio(Long folio) {
        log.debug("Consultando historial de conteos para folio {}", folio);
        return countHistoryRepository.findByFolioOrderByCreatedAtAsc(folio)
                .stream()
                .map(countHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el historial de conteos filtrado por estado
     * @param status Estado del conteo (ej: "REGISTRADO", "ACTUALIZADO")
     * @param pageable Información de paginación
     * @return Página con el historial filtrado
     */
    public Page<CountHistoryResponse> getCountHistoryByStatus(String status, Pageable pageable) {
        log.debug("Consultando historial de conteos con estado {}", status);
        return countHistoryRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(countHistoryMapper::toResponse);
    }

    /**
     * Obtiene el historial de conteos de un usuario en un período específico
     * @param userId ID del usuario
     * @param periodId ID del período
     * @param pageable Información de paginación
     * @return Página con el historial filtrado
     */
    public Page<CountHistoryResponse> getCountHistoryByUserAndPeriod(Long userId, Long periodId, Pageable pageable) {
        log.debug("Consultando historial de conteos para usuario {} en período {}", userId, periodId);
        return countHistoryRepository.findByUserIdAndPeriodIdOrderByCreatedAtDesc(userId, periodId, pageable)
                .map(countHistoryMapper::toResponse);
    }

    /**
     * Cuenta los conteos registrados por un usuario en un período
     * @param userId ID del usuario
     * @param periodId ID del período
     * @return Cantidad de conteos
     */
    public Long countUserContosInPeriod(Long userId, Long periodId) {
        return countHistoryRepository.countByUserIdAndPeriodId(userId, periodId);
    }

    /**
     * Obtiene TODOS los conteos registrados de todos los almacenes
     * @param pageable Información de paginación
     * @return Página con todos los conteos ordenados por fecha descendente
     */
    public Page<CountHistoryResponse> getAllCounts(Pageable pageable) {
        log.debug("Consultando TODOS los conteos registrados");
        return countHistoryRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(countHistoryMapper::toResponse);
    }
}
