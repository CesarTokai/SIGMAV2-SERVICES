package tokai.com.mx.SIGMAV2.modules.labels.infrastructure.mapper;

import org.springframework.stereotype.Component;
import tokai.com.mx.SIGMAV2.modules.labels.adapter.dto.CountHistoryResponse;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.CountHistory;

/**
 * Mapper para convertir CountHistory a CountHistoryResponse
 */
@Component
public class CountHistoryMapper {

    public CountHistoryResponse toResponse(CountHistory entity) {
        if (entity == null) {
            return null;
        }

        return CountHistoryResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .role(entity.getRole())
                .folio(entity.getFolio())
                .countType(entity.getCountType() != null ? 
                        (entity.getCountType() == 1L ? "C1" : "C2") : null)
                .countValue(entity.getCountValue())
                .status(entity.getStatus())
                .periodId(entity.getPeriodId())
                .warehouseId(entity.getWarehouseId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .description(entity.getDescription())
                .build();
    }
}

