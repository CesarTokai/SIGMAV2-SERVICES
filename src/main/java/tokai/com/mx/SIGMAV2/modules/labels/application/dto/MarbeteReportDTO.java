package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import java.awt.image.BufferedImage;

/**
 * DTO para pasar datos del marbete a JasperReports, incluyendo la imagen QR.
 * Diseñado para mostrar 3 marbetes por fila.
 */
public class MarbeteReportDTO {
    // Marbete 1 (izquierda)
    private String nomMarbete1;
    private String clave1;
    private String descr1;
    private String almacen1;
    private String fecha1;
    private BufferedImage qrImage1;

    // Marbete 2 (centro)
    private String nomMarbete2;
    private String clave2;
    private String descr2;
    private String almacen2;
    private String fecha2;
    private BufferedImage qrImage2;

    // Marbete 3 (derecha)
    private String nomMarbete3;
    private String clave3;
    private String descr3;
    private String almacen3;
    private String fecha3;
    private BufferedImage qrImage3;

    public MarbeteReportDTO() {
    }

    public MarbeteReportDTO(String nomMarbete1, String clave1, String descr1, String almacen1, String fecha1, BufferedImage qrImage1,
                           String nomMarbete2, String clave2, String descr2, String almacen2, String fecha2, BufferedImage qrImage2,
                           String nomMarbete3, String clave3, String descr3, String almacen3, String fecha3, BufferedImage qrImage3) {
        this.nomMarbete1 = nomMarbete1;
        this.clave1 = clave1;
        this.descr1 = descr1;
        this.almacen1 = almacen1;
        this.fecha1 = fecha1;
        this.qrImage1 = qrImage1;

        this.nomMarbete2 = nomMarbete2;
        this.clave2 = clave2;
        this.descr2 = descr2;
        this.almacen2 = almacen2;
        this.fecha2 = fecha2;
        this.qrImage2 = qrImage2;

        this.nomMarbete3 = nomMarbete3;
        this.clave3 = clave3;
        this.descr3 = descr3;
        this.almacen3 = almacen3;
        this.fecha3 = fecha3;
        this.qrImage3 = qrImage3;
    }

    // Getters y Setters para Marbete 1
    public String getNomMarbete1() { return nomMarbete1; }
    public void setNomMarbete1(String nomMarbete1) { this.nomMarbete1 = nomMarbete1; }
    public String getClave1() { return clave1; }
    public void setClave1(String clave1) { this.clave1 = clave1; }
    public String getDescr1() { return descr1; }
    public void setDescr1(String descr1) { this.descr1 = descr1; }
    public String getAlmacen1() { return almacen1; }
    public void setAlmacen1(String almacen1) { this.almacen1 = almacen1; }
    public String getFecha1() { return fecha1; }
    public void setFecha1(String fecha1) { this.fecha1 = fecha1; }
    public BufferedImage getQrImage1() { return qrImage1; }
    public void setQrImage1(BufferedImage qrImage1) { this.qrImage1 = qrImage1; }

    // Getters y Setters para Marbete 2
    public String getNomMarbete2() { return nomMarbete2; }
    public void setNomMarbete2(String nomMarbete2) { this.nomMarbete2 = nomMarbete2; }
    public String getClave2() { return clave2; }
    public void setClave2(String clave2) { this.clave2 = clave2; }
    public String getDescr2() { return descr2; }
    public void setDescr2(String descr2) { this.descr2 = descr2; }
    public String getAlmacen2() { return almacen2; }
    public void setAlmacen2(String almacen2) { this.almacen2 = almacen2; }
    public String getFecha2() { return fecha2; }
    public void setFecha2(String fecha2) { this.fecha2 = fecha2; }
    public BufferedImage getQrImage2() { return qrImage2; }
    public void setQrImage2(BufferedImage qrImage2) { this.qrImage2 = qrImage2; }

    // Getters y Setters para Marbete 3
    public String getNomMarbete3() { return nomMarbete3; }
    public void setNomMarbete3(String nomMarbete3) { this.nomMarbete3 = nomMarbete3; }
    public String getClave3() { return clave3; }
    public void setClave3(String clave3) { this.clave3 = clave3; }
    public String getDescr3() { return descr3; }
    public void setDescr3(String descr3) { this.descr3 = descr3; }
    public String getAlmacen3() { return almacen3; }
    public void setAlmacen3(String almacen3) { this.almacen3 = almacen3; }
    public String getFecha3() { return fecha3; }
    public void setFecha3(String fecha3) { this.fecha3 = fecha3; }
    public BufferedImage getQrImage3() { return qrImage3; }
    public void setQrImage3(BufferedImage qrImage3) { this.qrImage3 = qrImage3; }
}

