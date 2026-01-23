package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "labels")
public class Label {

    @Id
    private Long folio;

    @Column(name = "id_label_request")
    private Long labelRequestId;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "id_product", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State estado = State.GENERADO;

    @Column(name = "impreso_at")
    private LocalDateTime impresoAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum State {GENERADO, IMPRESO, CANCELADO}

    public Long getFolio() { return folio; }
    public void setFolio(Long folio) { this.folio = folio; }

    public Long getLabelRequestId() { return labelRequestId; }
    public void setLabelRequestId(Long labelRequestId) { this.labelRequestId = labelRequestId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public State getEstado() { return estado; }
    public void setEstado(State estado) { this.estado = estado; }

    public LocalDateTime getImpresoAt() { return impresoAt; }
    public void setImpresoAt(LocalDateTime impresoAt) { this.impresoAt = impresoAt; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
