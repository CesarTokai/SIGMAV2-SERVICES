package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

@Data
public class AssignWarehousesDTO {
    @NotEmpty(message = "Debe especificar al menos un almacén")
    @Size(max = 50, message = "No puede asignar más de 50 almacenes a la vez")
    private Set<Long> warehouseIds;
}
