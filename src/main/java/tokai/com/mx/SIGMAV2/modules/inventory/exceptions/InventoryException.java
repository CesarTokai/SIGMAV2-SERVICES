package tokai.com.mx.SIGMAV2.modules.inventory.exceptions;

public class InventoryException extends RuntimeException {
    
    private String errorCode;
    
    public InventoryException(String message) {
        super(message);
    }
    
    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public InventoryException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public InventoryException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}