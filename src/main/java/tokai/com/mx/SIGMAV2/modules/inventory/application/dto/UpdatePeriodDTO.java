package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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


}
