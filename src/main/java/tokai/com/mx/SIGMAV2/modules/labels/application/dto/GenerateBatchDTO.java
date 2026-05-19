package tokai.com.mx.SIGMAV2.modules.labels.application.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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


}

