package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignWarehousesDTO {

    @NotNull(message = "La lista de almacenes es obligatoria")
    @NotEmpty(message = "Debe asignar al menos un almacén")
    private List<Long> warehouseIds;

    private String reason; // Razón de la asignación (opcional)
}