package tokai.com.mx.SIGMAV2.modules.labels.application.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateBatchDTO {

    @NotNull
    private Long productId;

    @NotNull
    private Long warehouseId;

    @NotNull
    private Long periodId;

    @NotNull
    @Min(1)
    private Integer labelsToGenerate;

    @Size(max = 600, message = "El comentario no puede exceder 600 caracteres")
    private String comment;

}

