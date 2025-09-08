package tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception;

public class WarehouseAccessDeniedException extends RuntimeException {
    public WarehouseAccessDeniedException(String message) {
        super(message);
    }
    
    public WarehouseAccessDeniedException(Long userId, Long warehouseId) {
        super("No tienes acceso a este almacén. Usuario: " + userId + ", Almacén: " + warehouseId);
    }
}