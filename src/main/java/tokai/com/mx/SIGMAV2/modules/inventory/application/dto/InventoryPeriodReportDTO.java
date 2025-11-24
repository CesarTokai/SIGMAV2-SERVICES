package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import java.math.BigDecimal;

public class InventoryPeriodReportDTO {
    private String cveArt;
    private String descr;
    private String uniMed;
    private BigDecimal existQty;
    private String status;

    public String getCveArt() { return cveArt; }
    public void setCveArt(String cveArt) { this.cveArt = cveArt; }

    public String getDescr() { return descr; }
    public void setDescr(String descr) { this.descr = descr; }

    public String getUniMed() { return uniMed; }
    public void setUniMed(String uniMed) { this.uniMed = uniMed; }

    public BigDecimal getExistQty() { return existQty; }
    public void setExistQty(BigDecimal existQty) { this.existQty = existQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

