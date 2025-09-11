package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MultiWarehouseImportDTO {
    private String period;
    private MultipartFile file;
}

