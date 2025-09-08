package tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception;

public class WarehouseNotFoundException extends RuntimeException {
    public WarehouseNotFoundException(String message) {
        super(message);
    }
    
    public WarehouseNotFoundException(Long id) {
        super("Almacén no encontrado con ID: " + id);
    }
}