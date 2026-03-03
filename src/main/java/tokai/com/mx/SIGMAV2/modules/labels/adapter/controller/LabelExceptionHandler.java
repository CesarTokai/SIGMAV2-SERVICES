package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.CatalogNotLoadedException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.CountSequenceException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.DuplicateCountException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.labels.domain.exception.LabelAlreadyCancelledException;

import java.util.Map;

/**
 * Manejador global de excepciones para el módulo de marbetes.
 * Centraliza el manejo de errores que antes se repetía en cada método del controlador.
 */
@RestControllerAdvice(assignableTypes = LabelsController.class)
@Slf4j
public class LabelExceptionHandler {

    @ExceptionHandler(LabelNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(LabelNotFoundException ex) {
        log.warn("Folio no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Folio no encontrado", "message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidLabelStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(InvalidLabelStateException ex) {
        log.warn("Estado inválido de marbete: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Estado inválido", "message", ex.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<Map<String, String>> handlePermissionDenied(PermissionDeniedException ex) {
        log.warn("Permiso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Permiso denegado", "message", ex.getMessage()));
    }

    @ExceptionHandler(CatalogNotLoadedException.class)
    public ResponseEntity<Map<String, String>> handleCatalogNotLoaded(CatalogNotLoadedException ex) {
        log.warn("Catálogos no cargados: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Catálogos no cargados", "message", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateCountException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateCount(DuplicateCountException ex) {
        log.warn("Conteo duplicado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conteo duplicado", "message", ex.getMessage()));
    }

    @ExceptionHandler(CountSequenceException.class)
    public ResponseEntity<Map<String, String>> handleCountSequence(CountSequenceException ex) {
        log.warn("Secuencia de conteo inválida: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Secuencia inválida", "message", ex.getMessage()));
    }

    @ExceptionHandler(LabelAlreadyCancelledException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyCancelled(LabelAlreadyCancelledException ex) {
        log.warn("Marbete ya cancelado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Marbete ya cancelado", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        log.warn("Estado ilegal: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Operación inválida", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Parámetro inválido", "message", ex.getMessage()));
    }
}

