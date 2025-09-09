package tokai.com.mx.SIGMAV2.modules.inventory.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouse")
public class Warehouse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_warehouse")
    private Long idWarehouse;
    
    @Column(name = "warehouse_key", unique = true, nullable = false, length = 50)
    private String warehouseKey;
    
    @Column(name = "name_warehouse", unique = true, nullable = false)
    private String nameWarehouse;
    
    @Column(name = "observations")
    private String observations;
    
    // Constructors
    public Warehouse() {}
    
    public Warehouse(String warehouseKey, String nameWarehouse, String observations) {
        this.warehouseKey = warehouseKey;
        this.nameWarehouse = nameWarehouse;
        this.observations = observations;
    }
    
    // Getters and Setters
    public Long getIdWarehouse() {
        return idWarehouse;
    }
    
    public void setIdWarehouse(Long idWarehouse) {
        this.idWarehouse = idWarehouse;
    }
    
    public String getWarehouseKey() {
        return warehouseKey;
    }
    
    public void setWarehouseKey(String warehouseKey) {
        this.warehouseKey = warehouseKey;
    }
    
    public String getNameWarehouse() {
        return nameWarehouse;
    }
    
    public void setNameWarehouse(String nameWarehouse) {
        this.nameWarehouse = nameWarehouse;
    }
    
    public String getObservations() {
        return observations;
    }
    
    public void setObservations(String observations) {
        this.observations = observations;
    }
}