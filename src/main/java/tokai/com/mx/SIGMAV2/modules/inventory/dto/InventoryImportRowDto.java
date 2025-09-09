package tokai.com.mx.SIGMAV2.modules.inventory.dto;

import java.math.BigDecimal;

public class InventoryImportRowDto {
    
    private String cveArt;
    private String descr;
    private String uniMed;
    private BigDecimal exist;
    private String status;
    private String warehouseKey; // opcional si viene en archivo
    private int rowNumber;
    
    // Constructors
    public InventoryImportRowDto() {}
    
    public InventoryImportRowDto(String cveArt, String descr, String uniMed, BigDecimal exist, String status) {
        this.cveArt = cveArt;
        this.descr = descr;
        this.uniMed = uniMed;
        this.exist = exist;
        this.status = status;
    }
    
    // Getters and Setters
    public String getCveArt() {
        return cveArt;
    }
    
    public void setCveArt(String cveArt) {
        this.cveArt = cveArt;
    }
    
    public String getDescr() {
        return descr;
    }
    
    public void setDescr(String descr) {
        this.descr = descr;
    }
    
    public String getUniMed() {
        return uniMed;
    }
    
    public void setUniMed(String uniMed) {
        this.uniMed = uniMed;
    }
    
    public BigDecimal getExist() {
        return exist;
    }
    
    public void setExist(BigDecimal exist) {
        this.exist = exist;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getWarehouseKey() {
        return warehouseKey;
    }
    
    public void setWarehouseKey(String warehouseKey) {
        this.warehouseKey = warehouseKey;
    }
    
    public int getRowNumber() {
        return rowNumber;
    }
    
    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }
}