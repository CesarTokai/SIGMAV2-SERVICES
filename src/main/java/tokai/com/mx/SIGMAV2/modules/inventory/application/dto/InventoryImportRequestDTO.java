package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import org.springframework.web.multipart.MultipartFile;

public class InventoryImportRequestDTO {
    private MultipartFile file;
    private Long idPeriod;
    private Long idWarehouse; // opcional
}