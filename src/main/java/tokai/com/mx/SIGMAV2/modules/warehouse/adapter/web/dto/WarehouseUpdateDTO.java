package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseUpdateDTO {
    @NotBlank(message = "El nombre del almacén es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nameWarehouse;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observations;
}
