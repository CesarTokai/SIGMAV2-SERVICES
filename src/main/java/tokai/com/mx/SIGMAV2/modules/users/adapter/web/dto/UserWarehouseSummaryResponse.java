package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO resumen de usuarios con almacenes asignados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWarehouseSummaryResponse {
    private Long userId;
    private String email;
    private String role;
    private boolean status;
    private long warehousesCount;
    private List<Long> warehouseIds;
}
