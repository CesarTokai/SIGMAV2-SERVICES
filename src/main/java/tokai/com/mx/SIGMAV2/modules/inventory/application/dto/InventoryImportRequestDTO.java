package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class InventoryImportRequestDTO {
    private MultipartFile file;
    private Long idPeriod;
    private Long idWarehouse; // opcional
    private Long userId;
}

