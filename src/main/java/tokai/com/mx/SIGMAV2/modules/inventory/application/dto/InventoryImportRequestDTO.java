package tokai.com.mx.SIGMAV2.modules.inventory.application.dto;

import org.springframework.web.multipart.MultipartFile;

public class InventoryImportRequestDTO {
    private MultipartFile file;
    private Long idPeriod;
    private Long idWarehouse; // opcional
    private Long userId;


    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public Long getIdPeriod() {
        return idPeriod;
    }

    public void setIdPeriod(Long idPeriod) {
        this.idPeriod = idPeriod;
    }

    public Long getIdWarehouse() {
        return idWarehouse;
    }

    public void setIdWarehouse(Long idWarehouse) {
        this.idWarehouse = idWarehouse;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}