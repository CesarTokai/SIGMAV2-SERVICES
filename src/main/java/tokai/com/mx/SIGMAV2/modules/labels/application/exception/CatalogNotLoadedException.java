package tokai.com.mx.SIGMAV2.modules.labels.application.exception;

/**
 * Excepción lanzada cuando se intenta imprimir marbetes sin que los catálogos
 * de inventario y multialmacén hayan sido cargados.
 */
public class CatalogNotLoadedException extends RuntimeException {
    public CatalogNotLoadedException(String message) {
        super(message);
    }
}

