package tokai.com.mx.SIGMAV2.modules.personal_information.domain.port.input;

import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.personal_information.adapter.web.dto.ImageResponse;

import java.util.Optional;

public interface ImageService {
    ImageResponse uploadImage(Long userId, MultipartFile file);
    ImageResponse updateImage(Long userId, MultipartFile file);
    Optional<ImageResponse> getImageByUserId(Long userId);
    void deleteImageByUserId(Long userId);
    boolean hasImage(Long userId);
}
