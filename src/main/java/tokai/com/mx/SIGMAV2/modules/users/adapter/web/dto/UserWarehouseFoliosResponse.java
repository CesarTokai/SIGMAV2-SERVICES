package tokai.com.mx.SIGMAV2.modules.users.adapter.web.dto;

import lombok.Data;

import java.sql.Timestamp;

/**
 * DTO que muestra por cada batch de generación:
 * - Email del usuario que generó
 * - Clave del almacén
 * - Nombre del almacén
 * - Primer folio del rango generado
 * - Último folio del rango generado
 * - Fecha en que se generó
 */
@Data
public class UserWarehouseFoliosResponse {

    private String usuario;
    private String claveAlmacen;
    private String nombreAlmacen;
    private Long primerFolio;
    private Long ultimoFolio;
    private Timestamp generadoAt;

    /**
     * Constructor ÚNICO para mapeo desde native query.
     * Orden exacto del SELECT: usuario, claveAlmacen, nombreAlmacen, primerFolio, ultimoFolio, generadoAt
     */
    public UserWarehouseFoliosResponse(String usuario, String claveAlmacen,
                                       String nombreAlmacen, Long primerFolio,
                                       Long ultimoFolio, Timestamp generadoAt) {
        this.usuario = usuario;
        this.claveAlmacen = claveAlmacen;
        this.nombreAlmacen = nombreAlmacen;
        this.primerFolio = primerFolio;
        this.ultimoFolio = ultimoFolio;
        this.generadoAt = generadoAt;
    }
}

