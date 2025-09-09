package tokai.com.mx.SIGMAV2.modules.inventory.exceptions;

import org.springframework.http.HttpStatus;

public class InventoryBusinessException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public InventoryBusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public InventoryBusinessException(String message, String errorCode) {
        this(message, errorCode, HttpStatus.BAD_REQUEST);
    }
    
    public InventoryBusinessException(String message) {
        this(message, "INVENTORY_ERROR", HttpStatus.BAD_REQUEST);
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    // Métodos estáticos para crear excepciones comunes
    public static InventoryBusinessException periodNotFound(Long periodId) {
        return new InventoryBusinessException(
            "Periodo no encontrado con ID: " + periodId + 
            ". Verifique que el periodo exista en el sistema.",
            "PERIOD_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }
    
    public static InventoryBusinessException warehouseNotFound(Long warehouseId) {
        return new InventoryBusinessException(
            "Almacén no encontrado con ID: " + warehouseId + 
            ". Verifique que el almacén exista en el sistema.",
            "WAREHOUSE_NOT_FOUND",
            HttpStatus.NOT_FOUND
        );
    }
    
    public static InventoryBusinessException productNotFound(String productCode) {
        return new InventoryBusinessException(
            "Producto no encontrado con código: " + productCode + 
            ". Verifique que el producto exista en el catálogo.",
            "PRODUCT_NOT_FOUND",
            HttpStatus.BAD_REQUEST
        );
    }
    
    public static InventoryBusinessException invalidFileFormat(String filename) {
        return new InventoryBusinessException(
            "Formato de archivo no válido: " + filename + 
            ". Solo se permiten archivos CSV, XLS o XLSX.",
            "INVALID_FILE_FORMAT",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE
        );
    }
    
    public static InventoryBusinessException emptyFile() {
        return new InventoryBusinessException(
            "El archivo está vacío o no contiene datos válidos para importar.",
            "EMPTY_FILE",
            HttpStatus.BAD_REQUEST
        );
    }
    
    public static InventoryBusinessException validationErrors(String details) {
        return new InventoryBusinessException(
            "Errores de validación en el archivo: " + details,
            "VALIDATION_ERRORS",
            HttpStatus.BAD_REQUEST
        );
    }
    
    public static InventoryBusinessException duplicateImport(String checksum) {
        return new InventoryBusinessException(
            "Ya existe una importación idéntica. Use mode=REPLACE para sobrescribir.",
            "DUPLICATE_IMPORT",
            HttpStatus.CONFLICT
        );
    }
}