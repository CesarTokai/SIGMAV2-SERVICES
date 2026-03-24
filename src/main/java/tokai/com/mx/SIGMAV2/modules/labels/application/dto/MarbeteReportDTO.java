package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import java.awt.image.BufferedImage;

/**
 * DTO para pasar datos del marbete a JasperReports, incluyendo la imagen QR
 */
public class MarbeteReportDTO {
    private String nomMarbete;      // Número del marbete (ej: "42")
    private String clave;            // Clave del producto
    private String descr;            // Descripción del producto
    private String codigo;           // Código del producto
    private String descripcion;      // Descripción completa
    private String almacen;          // Nombre del almacén
    private String fecha;            // Fecha del marbete
    private BufferedImage qrImage;   // Imagen del QR generada

    public MarbeteReportDTO() {
    }

    public MarbeteReportDTO(String nomMarbete, String clave, String descr, 
                           String codigo, String descripcion, String almacen, 
                           String fecha, BufferedImage qrImage) {
        this.nomMarbete = nomMarbete;
        this.clave = clave;
        this.descr = descr;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.almacen = almacen;
        this.fecha = fecha;
        this.qrImage = qrImage;
    }

    // Getters and Setters
    public String getNomMarbete() {
        return nomMarbete;
    }

    public void setNomMarbete(String nomMarbete) {
        this.nomMarbete = nomMarbete;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getAlmacen() {
        return almacen;
    }

    public void setAlmacen(String almacen) {
        this.almacen = almacen;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public BufferedImage getQrImage() {
        return qrImage;
    }

    public void setQrImage(BufferedImage qrImage) {
        this.qrImage = qrImage;
    }
}

