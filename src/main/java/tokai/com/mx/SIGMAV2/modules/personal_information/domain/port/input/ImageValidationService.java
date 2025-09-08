package tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input;

import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.personal_information.domain.model.ImageValidationResult;

public interface ImageValidationService {
    ImageValidationResult validateImage(MultipartFile file);
    ImageValidationResult validateImageBytes(byte[] imageBytes, String originalFilename);
}
