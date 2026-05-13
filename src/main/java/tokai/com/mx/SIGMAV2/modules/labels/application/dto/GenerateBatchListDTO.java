package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class GenerateBatchListDTO {
    @NotNull
    private Long warehouseId;

    @NotNull
    private Long periodId;

    @NotNull
    private List<ProductBatchDTO> products;

    @Getter
    @Setter
    public static class ProductBatchDTO {
        @NotNull
        private Long productId;
        @NotNull
        @Min(1)
        private Integer labelsToGenerate;
    }
}

