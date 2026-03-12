package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.Data;

/**
 * Resumen de rangos de folios generados por usuario y almacén.
 *
 * <p><b>Usado por:</b> GET /api/sigmav2/admin/users/with-warehouses</p>
 *
 * <p>Cada fila representa un usuario + almacén con el rango global
 * de folios que ha generado (primer folio y último folio).</p>
 */
@Data
public class UserWarehouseSummaryResponse {

    /** Email del usuario que generó los marbetes */
    private String usuario;

    /** Clave identificadora del almacén */
    private String claveAlmacen;

    /** Nombre del almacén */
    private String nombreAlmacen;

    /** Primer folio generado (MIN de todos sus batches) */
    private Long primerFolio;

    /** Último folio generado (MAX de todos sus batches) */
    private Long ultimoFolio;

    /**
     * Constructor ÚNICO para mapeo desde native query (Hibernate).
     * Orden exacto del SELECT: usuario, claveAlmacen, nombreAlmacen, primerFolio, ultimoFolio
     */
    public UserWarehouseSummaryResponse(String usuario, String claveAlmacen,
                                        String nombreAlmacen,
                                        Long primerFolio, Long ultimoFolio) {
        this.usuario      = usuario;
        this.claveAlmacen = claveAlmacen;
        this.nombreAlmacen = nombreAlmacen;
        this.primerFolio  = primerFolio;
        this.ultimoFolio  = ultimoFolio;
    }
}
