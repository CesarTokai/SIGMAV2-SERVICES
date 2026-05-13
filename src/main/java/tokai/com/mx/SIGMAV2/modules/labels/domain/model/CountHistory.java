package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla folio_request_history
 * Reutilizada como historial de conteos (C1 y C2)
 * 
 * Registra cada vez que un usuario registra o actualiza un conteo,
 * incluyendo: usuario, email, tipo de conteo, valor, fecha, etc.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "folio_request_history")
public class CountHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "role")
    private String role;

    /**
     * Folio del marbete donde se registró el conteo
     * (Reutilizamos folio_start para este dato)
     */
    @Column(name = "folio_start", nullable = false)
    private Long folio;

    /**
     * Tipo de conteo: "C1" o "C2"
     * (Reutilizamos folio_end para este dato)
     */
    @Column(name = "folio_end")
    private Long countType;

    /**
     * Valor del conteo registrado
     * (Reutilizamos quantity para este dato)
     */
    @Column(name = "quantity", nullable = false)
    private Integer countValue;

    /**
     * Estado del registro: "REGISTRADO", "ACTUALIZADO", etc.
     */
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "period_id")
    private Long periodId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Descripción adicional del evento (ej: "Actualizado de 100 a 150")
     * Se guardaría en un comentario interno
     */
    @Transient
    private String description;
}

