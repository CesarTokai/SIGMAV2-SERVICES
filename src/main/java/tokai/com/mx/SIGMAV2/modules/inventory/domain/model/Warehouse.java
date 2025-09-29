package tokai.com.mx.SIGMAV2.modules.inventory.domain.model;

public class Warehouse {
    private Long id;
    private String name;
    private String observations;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Warehouse(Long id, String name, String observations) {
        this.id = id;
        this.name = name;
        this.observations = observations;
    }

    public Warehouse() {
    }
}
