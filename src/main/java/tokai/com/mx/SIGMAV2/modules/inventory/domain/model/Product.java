package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import java.time.LocalDateTime;

public class Product {

    private Long id;
    private String cveArt;
    private String descr;
    private String uniMed;
    private Status status;
    private LocalDateTime createdAt;
    private String linProd;

    public enum Status { A, B }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCveArt() { return cveArt; }
    public void setCveArt(String cveArt) { this.cveArt = cveArt; }

    public String getDescr() { return descr; }
    public void setDescr(String descr) { this.descr = descr; }

    public String getUniMed() { return uniMed; }
    public void setUniMed(String uniMed) { this.uniMed = uniMed; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getLinProd() { return linProd; }
    public void setLinProd(String linProd) { this.linProd = linProd; }
}
