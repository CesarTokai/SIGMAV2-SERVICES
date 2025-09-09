package tokai.com.mx.SIGMAV2.modules.inventory.dto;

import java.math.BigDecimal;

public class InventoryDto {
    
    private String cveArt;
    private String description;
    private String unitOfMeasure;
    private BigDecimal existQty;
    private String status;
    private String warehouseKey;
    private String warehouseName;
    private String period;
    
    // Constructors
    public InventoryDto() {}
    
    public InventoryDto(String cveArt, String description, String unitOfMeasure, 
                       BigDecimal existQty, String status, String warehouseKey, 
                       String warehouseName, String period) {
        this.cveArt = cveArt;
        this.description = description;
        this.unitOfMeasure = unitOfMeasure;
        this.existQty = existQty;
        this.status = status;
        this.warehouseKey = warehouseKey;
        this.warehouseName = warehouseName;
        this.period = period;
    }
    
    // Getters and Setters
    public String getCveArt() {
        return cveArt;
    }
    
    public void setCveArt(String cveArt) {
        this.cveArt = cveArt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    
    public BigDecimal getExistQty() {
        return existQty;
    }
    
    public void setExistQty(BigDecimal existQty) {
        this.existQty = existQty;
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
    
    public String getWarehouseName() {
        return warehouseName;
    }
    
    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }
    
    public String getPeriod() {
        return period;
    }
    
    public void setPeriod(String period) {
        this.period = period;
    }
}