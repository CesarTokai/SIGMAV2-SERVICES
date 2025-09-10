package tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseCreateDTO {
    @NotBlank(message = "La clave del almacén es requerida")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "La clave debe contener solo letras mayúsculas y números")
    @Size(min = 2, max = 10, message = "La clave debe tener entre 2 y 10 caracteres")
    private String warehouseKey;

    @NotBlank(message = "El nombre del almacén es requerido")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nameWarehouse;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observations;
}
