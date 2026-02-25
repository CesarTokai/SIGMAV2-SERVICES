package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa un producto que fue marcado automáticamente como BAJA
 * durante una importación de multialmacén, porque no apareció en el archivo Excel.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoBajaDTO {

    private String claveProducto;
    private String nombreProducto;
    private String claveAlmacen;
    private String nombreAlmacen;
    private BigDecimal existenciasAnteriores;
}

