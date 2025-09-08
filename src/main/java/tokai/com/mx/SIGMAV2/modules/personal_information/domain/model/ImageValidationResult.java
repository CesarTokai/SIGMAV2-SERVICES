package tokai.com.mx.SIGMAV2.modules.personal_information.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageValidationResult {
    private final boolean valid;
    private final String errorMessage;
    
    public static ImageValidationResult valid() {
        return new ImageValidationResult(true, null);
    }
    
    public static ImageValidationResult invalid(String errorMessage) {
        return new ImageValidationResult(false, errorMessage);
    }
}
