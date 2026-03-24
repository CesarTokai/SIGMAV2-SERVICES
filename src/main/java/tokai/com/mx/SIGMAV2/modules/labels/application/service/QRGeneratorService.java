package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Servicio para generar códigos QR que se incrustaran en los marbetes PDF.
 * 
 * Flujo:
 * 1. Recibe el número del marbete (ej: 42)
 * 2. Genera imagen QR en memoria (BufferedImage)
 * 3. Convierte a byte[] para pasar a JasperReports
 * 4. Se incrusta en la plantilla JRXML como imagen
 */
@Service
@Slf4j
public class QRGeneratorService {

    private static final int QR_WIDTH = 200;  // Ancho del QR en píxeles
    private static final int QR_HEIGHT = 200; // Alto del QR en píxeles

    /**
     * Genera un código QR a partir del número del marbete
     * 
     * @param numeroMarbete El número del marbete (ej: 42)
     * @return BufferedImage con el QR renderizado
     */
    public BufferedImage generarQR(String numeroMarbete) {
        try {
            log.info("Generando QR para marbete: {}", numeroMarbete);
            
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(
                numeroMarbete,              // Dato a codificar
                BarcodeFormat.QR_CODE,      // Formato QR
                QR_WIDTH, 
                QR_HEIGHT
            );
            
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            log.debug("QR generado exitosamente para marbete {}", numeroMarbete);
            return qrImage;
            
        } catch (WriterException e) {
            log.error("Error al generar QR para marbete {}: {}", numeroMarbete, e.getMessage());
            throw new RuntimeException("No se pudo generar el código QR", e);
        }
    }

    /**
     * Genera un código QR y lo convierte a byte[] para pasar a JasperReports
     * 
     * @param numeroMarbete El número del marbete
     * @return byte[] de la imagen QR en formato PNG
     */
    public byte[] generarQRBytes(String numeroMarbete) {
        try {
            BufferedImage qrImage = generarQR(numeroMarbete);
            
            // Convertir BufferedImage a bytes (PNG)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(qrImage, "PNG", baos);
            baos.flush();
            byte[] qrBytes = baos.toByteArray();
            baos.close();
            
            log.debug("QR convertido a bytes: {} bytes para marbete {}", qrBytes.length, numeroMarbete);
            return qrBytes;
            
        } catch (IOException e) {
            log.error("Error al convertir QR a bytes: {}", e.getMessage());
            throw new RuntimeException("No se pudo convertir QR a bytes", e);
        }
    }

    /**
     * Genera un código de barras linear (Code128) para casos que no sea QR
     * 
     * @param numeroMarbete El número del marbete
     * @return BufferedImage con el código de barras
     */
    public BufferedImage generarCodigoBarras(String numeroMarbete) {
        try {
            log.info("Generando código de barras Code128 para marbete: {}", numeroMarbete);
            
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(
                numeroMarbete,
                BarcodeFormat.CODE_128,
                200, 
                100  // Más bajo para código de barras
            );
            
            BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            log.debug("Código de barras generado exitosamente para marbete {}", numeroMarbete);
            return barcodeImage;
            
        } catch (WriterException e) {
            log.error("Error al generar código de barras: {}", e.getMessage());
            throw new RuntimeException("No se pudo generar el código de barras", e);
        }
    }

    /**
     * Obtiene el tamaño del QR en píxeles
     * @return Objeto con ancho y alto
     */
    public QRDimension getQRDimension() {
        return new QRDimension(QR_WIDTH, QR_HEIGHT);
    }

    /**
     * DTO para dimensiones del QR
     */
    public static class QRDimension {
        public int width;
        public int height;

        public QRDimension(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}

