package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class CountEventDTO {

    @NotNull
    private Long folio;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal countedValue;

    public Long getFolio() { return folio; }
    public void setFolio(Long folio) { this.folio = folio; }

    public BigDecimal getCountedValue() { return countedValue; }
    public void setCountedValue(BigDecimal countedValue) { this.countedValue = countedValue; }
}

