package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdatePeriodDTO {
    @NotNull
    private Long id;

    @Size(max = 255)
    private String comments;

    public UpdatePeriodDTO() {}

    public UpdatePeriodDTO(Long id, String comments) {
        this.id = id;
        this.comments = comments;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
