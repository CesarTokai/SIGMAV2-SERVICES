package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.ProductEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaProductRepository;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.MarbeteReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.WarehouseEntity;
import tokai.com.mx.SIGMAV2.modules.inventory.infrastructure.persistence.JpaWarehouseRepository;

import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio que integra la generación de QR con los datos de los marbetes.
 * 
 * Flujo:
 * 1. Obtiene los marbetes de BD (folio = número del marbete)
 * 2. Para cada marbete, genera su QR usando QRGeneratorService
 * 3. Obtiene datos de ProductEntity y WarehouseEntity
 * 4. Crea un DTO con todos los datos + la imagen QR
 * 5. Pasa la lista al JasperReportPdfService para generar el PDF
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarbeteQRIntegrationService {

    private final QRGeneratorService qrGeneratorService;
    private final JpaLabelRepository labelRepository;
    private final JpaProductRepository productRepository;
    private final JpaWarehouseRepository warehouseRepository;

    /**
     * Genera la lista de DTOs con QR para los marbetes de impresión
     * 
     * @param periodId ID del período
     * @param warehouseId ID del almacén
     * @return Lista de MarbeteReportDTO con QR generados
     */
    public List<MarbeteReportDTO> generarMarbetesConQR(Long periodId, Long warehouseId) {
        log.info("🎯 Generando marbetes con QR para período={}, almacén={}", periodId, warehouseId);
        
        // 1. Obtener todos los marbetes del período y almacén
        List<Label> marbetes = labelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId);
        
        List<MarbeteReportDTO> marBetesConQR = new ArrayList<>();
        
        // 2. Para cada marbete, generar su QR
        for (Label label : marbetes) {
            try {
                marBetesConQR.add(generarMarbeteConQR(label));
                
            } catch (Exception e) {
                log.error("❌ Error al generar QR para marbete {}: {}", label.getFolio(), e.getMessage());
                // Continuar con el siguiente marbete sin fallar todo el proceso
            }
        }
        
        log.info("✅ {} marbetes procesados con QR", marBetesConQR.size());
        return marBetesConQR;
    }

    /**
     * Genera QR para un único marbete
     * 
     * @param label El marbete
     * @return DTO con QR incluido
     */
    public MarbeteReportDTO generarMarbeteConQR(Label label) {
        log.debug("Generando QR para marbete único: {}", label.getFolio());
        
        // Generar imagen QR usando el folio (número del marbete)
        BufferedImage qrImage = qrGeneratorService.generarQR(String.valueOf(label.getFolio()));
        
        // Obtener datos del producto
        ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
        String clave = product != null ? product.getCveArt() : "N/A";
        String descripcion = product != null ? product.getDescr() : "N/A";
        
        // Obtener datos del almacén
        WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
        String nombreAlmacen = warehouse != null ? warehouse.getNameWarehouse() : "N/A";
        
        // Crear DTO con todos los datos
        MarbeteReportDTO dto = new MarbeteReportDTO(
            String.valueOf(label.getFolio()),                                    // NomMarbete (folio = número)
            clave,                                                               // CLAVE
            descripcion,                                                         // DESCR
            clave,                                                               // Codigo
            descripcion,                                                         // Descripcion
            nombreAlmacen,                                                       // Almacen
            label.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), // Fecha
            qrImage                                                              // QRImage ← LA IMAGEN
        );
        
        log.debug("✅ QR generado para marbete {}", label.getFolio());
        return dto;
    }

    /**
     * Genera QR para una lista específica de folios (números de marbete)
     * 
     * @param folios Lista de folios (ej: [42, 43, 44])
     * @param periodId ID del período
     * @param warehouseId ID del almacén
     * @return Lista de DTOs con QR
     */
    public List<MarbeteReportDTO> generarMarbetesEspecificosConQR(List<Long> folios, Long periodId, Long warehouseId) {
        log.info("Generando {} marbetes específicos con QR", folios.size());
        
        List<MarbeteReportDTO> resultado = new ArrayList<>();
        
        for (Long folio : folios) {
            try {
                // Usar findByFolioAndPeriodIdAndWarehouseId para obtener el marbete exacto
                Label label = labelRepository.findByFolioAndPeriodIdAndWarehouseId(folio, periodId, warehouseId)
                    .orElse(null);
                    
                if (label != null) {
                    resultado.add(generarMarbeteConQR(label));
                } else {
                    log.warn("Marbete no encontrado: folio={}, periodId={}, warehouseId={}", folio, periodId, warehouseId);
                }
            } catch (Exception e) {
                log.error("Error procesando marbete folio {}: {}", folio, e.getMessage());
            }
        }
        
        return resultado;
    }

    /**
     * Genera código de barras en lugar de QR (alternativa)
     * 
     * @param label El marbete
     * @return DTO con código de barras
     */
    public MarbeteReportDTO generarMarbeteConCodigoBarras(Label label) {
        log.debug("Generando código de barras para marbete: {}", label.getFolio());
        
        BufferedImage barcodeImage = qrGeneratorService.generarCodigoBarras(String.valueOf(label.getFolio()));
        
        // Obtener datos del producto
        ProductEntity product = productRepository.findById(label.getProductId()).orElse(null);
        String clave = product != null ? product.getCveArt() : "N/A";
        String descripcion = product != null ? product.getDescr() : "N/A";
        
        // Obtener datos del almacén
        WarehouseEntity warehouse = warehouseRepository.findById(label.getWarehouseId()).orElse(null);
        String nombreAlmacen = warehouse != null ? warehouse.getNameWarehouse() : "N/A";
        
        return new MarbeteReportDTO(
            String.valueOf(label.getFolio()),
            clave,
            descripcion,
            clave,
            descripcion,
            nombreAlmacen,
            label.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            barcodeImage
        );
    }
}

