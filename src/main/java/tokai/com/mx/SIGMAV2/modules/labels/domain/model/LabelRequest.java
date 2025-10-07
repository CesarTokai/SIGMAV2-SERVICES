package tokai.com.mx.SIGMAV2.modules.labels.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "label_requests", uniqueConstraints = {@UniqueConstraint(columnNames = {"id_product","id_warehouse","id_period"})})
public class LabelRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_label_request")
    private Long idLabelRequest;

    @Column(name = "id_product", nullable = false)
    private Long productId;

    @Column(name = "id_warehouse", nullable = false)
    private Long warehouseId;

    @Column(name = "id_period", nullable = false)
    private Long periodId;

    @Column(name = "requested_labels", nullable = false)
    private Integer requestedLabels;

    @Column(name = "folios_generados", nullable = false)
    private Integer foliosGenerados = 0;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getIdLabelRequest() { return idLabelRequest; }
    public void setIdLabelRequest(Long idLabelRequest) { this.idLabelRequest = idLabelRequest; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Long getPeriodId() { return periodId; }
    public void setPeriodId(Long periodId) { this.periodId = periodId; }

    public Integer getRequestedLabels() { return requestedLabels; }
    public void setRequestedLabels(Integer requestedLabels) { this.requestedLabels = requestedLabels; }

    public Integer getFoliosGenerados() { return foliosGenerados; }
    public void setFoliosGenerados(Integer foliosGenerados) { this.foliosGenerados = foliosGenerados; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
