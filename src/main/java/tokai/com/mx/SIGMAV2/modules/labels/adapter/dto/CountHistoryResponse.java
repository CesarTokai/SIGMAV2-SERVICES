package tokai.com.mx.SIGMAV2.modules.labels.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para mostrar el historial de conteos registrados por usuarios
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountHistoryResponse {

    private Long id;

    /** Email del usuario que registró el conteo */
    private String email;

    /** Nombre completo del usuario (si está disponible) */
    private String fullName;

    /** Rol del usuario al momento del registro */
    private String role;

    /** Folio del marbete */
    private Long folio;

    /** Tipo de conteo: "C1" o "C2" */
    private String countType;

    /** Valor del conteo registrado */
    private Integer countValue;

    /** Estado: "REGISTRADO" o "ACTUALIZADO" */
    private String status;

    /** ID del período */
    private Long periodId;

    /** ID del almacén */
    private Long warehouseId;

    /** Fecha y hora del registro */
    private LocalDateTime createdAt;

    /** Fecha y hora de la última actualización */
    private LocalDateTime updatedAt;

    /** Descripción del evento (ej: "Actualizado de 100 a 150") */
    private String description;
}

