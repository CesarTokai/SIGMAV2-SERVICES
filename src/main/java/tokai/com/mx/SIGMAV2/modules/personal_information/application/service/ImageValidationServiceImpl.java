package tokai.com.mx.SIGMAV2.modules.personal_information.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.ImageValidationResult;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input.ImageValidationService;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ImageValidationServiceImpl implements ImageValidationService {
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    );

    @Override
    public ImageValidationResult validateImage(MultipartFile file) {
        log.debug("Validando imagen: {}", file.getOriginalFilename());
        
        // Verificar si el archivo está vacío
        if (file.isEmpty()) {
            return ImageValidationResult.invalid("No se seleccionó ningún archivo");
        }
        
        // Verificar tamaño del archivo
        if (file.getSize() > MAX_FILE_SIZE) {
            return ImageValidationResult.invalid("La imagen no puede ser mayor a 5MB. Tamaño actual: " + (file.getSize() / 1024 / 1024) + "MB");
        }
        
        // Verificar nombre del archivo
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            return ImageValidationResult.invalid("El archivo debe tener un nombre válido");
        }
        
        // Verificar extensión del archivo
        if (!hasValidExtension(originalFilename)) {
            return ImageValidationResult.invalid("Extensión de archivo no válida. Extensiones permitidas: .jpg, .jpeg, .png, .gif, .bmp, .webp");
        }
        
        // Verificar tipo de contenido
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return ImageValidationResult.invalid("Tipo de archivo no permitido. Tipo detectado: " + contentType + ". Formatos válidos: JPG, JPEG, PNG, GIF, BMP, WEBP");
        }
        
        // Verificar que realmente sea una imagen válida
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0) {
                return ImageValidationResult.invalid("El archivo está vacío o corrupto");
            }
            
            if (!isValidImageBytes(bytes)) {
                return ImageValidationResult.invalid("El archivo no es una imagen válida o está corrupto");
            }
        } catch (Exception e) {
            log.error("Error al leer bytes del archivo {}: {}", originalFilename, e.getMessage(), e);
            return ImageValidationResult.invalid("Error al procesar el archivo: " + e.getMessage());
        }
        
        log.debug("Imagen validada exitosamente: {}", originalFilename);
        return ImageValidationResult.valid();
    }

    @Override
    public ImageValidationResult validateImageBytes(byte[] imageBytes, String originalFilename) {
        if (imageBytes.length == 0) {
            return ImageValidationResult.invalid("Los datos de la imagen están vacíos");
        }
        
        if (imageBytes.length > MAX_FILE_SIZE) {
            return ImageValidationResult.invalid("La imagen no puede ser mayor a 5MB");
        }
        
        if (originalFilename != null && !hasValidExtension(originalFilename)) {
            return ImageValidationResult.invalid("Extensión de archivo no válida");
        }
        
        if (!isValidImageBytes(imageBytes)) {
            return ImageValidationResult.invalid("Los datos no corresponden a una imagen válida");
        }
        
        return ImageValidationResult.valid();
    }
    
    private boolean hasValidExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(lowerFilename::endsWith);
    }
    
    private boolean isValidImageBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return false;
        }
        
        try {
            // JPEG (FF D8 FF)
            boolean isJpeg = bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xD8 && 
                (bytes[2] & 0xFF) == 0xFF;

            // PNG (89 50 4E 47 0D 0A 1A 0A)
            boolean isPng = bytes.length >= 8 &&
                (bytes[0] & 0xFF) == 0x89 &&
                (bytes[1] & 0xFF) == 0x50 && 
                (bytes[2] & 0xFF) == 0x4E && 
                (bytes[3] & 0xFF) == 0x47 &&
                (bytes[4] & 0xFF) == 0x0D && 
                (bytes[5] & 0xFF) == 0x0A && 
                (bytes[6] & 0xFF) == 0x1A && 
                (bytes[7] & 0xFF) == 0x0A;

            // GIF (47 49 46 38 [37|39] 61)
            boolean isGif = bytes.length >= 6 &&
                (bytes[0] & 0xFF) == 0x47 &&
                (bytes[1] & 0xFF) == 0x49 && 
                (bytes[2] & 0xFF) == 0x46 &&
                (bytes[3] & 0xFF) == 0x38 && 
                ((bytes[4] & 0xFF) == 0x37 || (bytes[4] & 0xFF) == 0x39) && 
                (bytes[5] & 0xFF) == 0x61;

            // BMP (42 4D)
            boolean isBmp = (bytes[0] & 0xFF) == 0x42 &&
                (bytes[1] & 0xFF) == 0x4D;

            // WEBP (52 49 46 46 ... 57 45 42 50)
            boolean isWebp = bytes.length >= 12 &&
                (bytes[0] & 0xFF) == 0x52 &&
                (bytes[1] & 0xFF) == 0x49 && 
                (bytes[2] & 0xFF) == 0x46 && 
                (bytes[3] & 0xFF) == 0x46 &&
                (bytes[8] & 0xFF) == 0x57 && 
                (bytes[9] & 0xFF) == 0x45 && 
                (bytes[10] & 0xFF) == 0x42 && 
                (bytes[11] & 0xFF) == 0x50;

            return isJpeg || isPng || isGif || isBmp || isWebp;

        } catch (Exception e) {
            log.error("Error al validar magic numbers: {}", e.getMessage());
            return false;
        }
    }
}