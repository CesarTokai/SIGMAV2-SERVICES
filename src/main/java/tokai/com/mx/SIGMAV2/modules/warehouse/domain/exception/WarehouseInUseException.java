package tokai.com.mx.SIGMAV2.modules.warehouse.domain.exception;

public class WarehouseInUseException extends RuntimeException {
    public WarehouseInUseException(String message) {
        super(message);
    }
    
    public WarehouseInUseException(Long warehouseId) {
        super("No se puede eliminar, almacén en uso: " + warehouseId);
    }
}