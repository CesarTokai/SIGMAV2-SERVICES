package tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "InventoryWarehouseEntity")
@Table(name = "warehouse")
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_warehouse")
    private Long idWarehouse;

    @Column(name = "warehouse_key", nullable = false, unique = true, length = 50)
    private String warehouseKey;

    @Column(name = "name_warehouse", nullable = false, unique = true)
    private String nameWarehouse;

    @Column(name = "observations")
    private String observations;

   }