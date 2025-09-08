package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseUpdateDTO {

    @NotBlank(message = "El nombre del almacén es obligatorio")
    @Size(min = 1, max = 255, message = "El nombre debe tener entre 1 y 255 caracteres")
    private String nameWarehouse;

    @Size(max = 1000, message = "Las observaciones no pueden exceder 1000 caracteres")
    private String observations;
}