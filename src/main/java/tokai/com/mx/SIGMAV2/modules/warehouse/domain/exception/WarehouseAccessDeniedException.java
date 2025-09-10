package tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class WarehouseAccessDeniedException extends RuntimeException {
    public WarehouseAccessDeniedException(String message) {
        super(message);
    }
}
