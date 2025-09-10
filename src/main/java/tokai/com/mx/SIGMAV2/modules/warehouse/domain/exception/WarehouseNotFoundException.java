package tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WarehouseNotFoundException extends RuntimeException {
    public WarehouseNotFoundException(Long id) {
        super("No se encontró el almacén con ID: " + id);
    }

    public WarehouseNotFoundException(String message) {
        super(message);
    }
}
