package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Warehouse {
    private Long id;
    private String name;
    private String observations;



    public Warehouse(Long id, String name, String observations) {
        this.id = id;
        this.name = name;
        this.observations = observations;
    }

    public Warehouse() {
    }
}
