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
     * AGRUPADOS DE 3 EN 3 para mostrar 3 por fila en el PDF
     * 
     * @param periodId ID del período
     * @param warehouseId ID del almacén
     * @return Lista de MarbeteReportDTO (agrupados de 3 en 3) con QR generados
     */
    public List<MarbeteReportDTO> generarMarbetesConQR(Long periodId, Long warehouseId) {
        log.info("🎯 Generando marbetes con QR para período={}, almacén={} (agrupados de 3 en 3)", periodId, warehouseId);
        
        // 1. Obtener todos los marbetes del período y almacén
        List<Label> marbetes = labelRepository.findByPeriodIdAndWarehouseId(periodId, warehouseId);
        
        List<MarbeteReportDTO> gruposAgrupadosPor3 = new ArrayList<>();
        
        // 2. Agrupar de 3 en 3
        for (int i = 0; i < marbetes.size(); i += 3) {
            try {
                Label m1 = marbetes.get(i);
                Label m2 = (i + 1 < marbetes.size()) ? marbetes.get(i + 1) : null;
                Label m3 = (i + 2 < marbetes.size()) ? marbetes.get(i + 2) : null;
                
                MarbeteReportDTO grupoDTO = agruparTresMarbetes(m1, m2, m3);
                gruposAgrupadosPor3.add(grupoDTO);
                
            } catch (Exception e) {
                log.error("❌ Error al generar grupo de QR en índice {}: {}", i, e.getMessage());
            }
        }
        
        log.info("✅ {} grupos de marbetes (3 por fila) procesados con QR", gruposAgrupadosPor3.size());
        return gruposAgrupadosPor3;
    }

    /**
     * Agrupa 3 marbetes en un solo DTO con campos nomMarbete1-3, clave1-3, etc.
     * 
     * @param m1 Primer marbete (izquierda)
     * @param m2 Segundo marbete (centro) - puede ser null
     * @param m3 Tercer marbete (derecha) - puede ser null
     * @return DTO con los 3 marbetes agrupados
     */
    private MarbeteReportDTO agruparTresMarbetes(Label m1, Label m2, Label m3) {
        log.debug("Agrupando 3 marbetes: m1={}, m2={}, m3={}", 
                  m1 != null ? m1.getFolio() : "null",
                  m2 != null ? m2.getFolio() : "null",
                  m3 != null ? m3.getFolio() : "null");
        
        MarbeteReportDTO dto = new MarbeteReportDTO();
        
        // Marbete 1 (siempre existe)
        if (m1 != null) {
            BufferedImage qr1 = qrGeneratorService.generarQR(String.valueOf(m1.getFolio()));
            ProductEntity p1 = productRepository.findById(m1.getProductId()).orElse(null);
            WarehouseEntity w1 = warehouseRepository.findById(m1.getWarehouseId()).orElse(null);
            
            dto.setNomMarbete1(String.valueOf(m1.getFolio()));
            dto.setClave1(p1 != null ? p1.getCveArt() : "N/A");
            dto.setDescr1(p1 != null ? p1.getDescr() : "N/A");
            dto.setAlmacen1(w1 != null ? w1.getNameWarehouse() : "N/A");
            dto.setFecha1(m1.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setQrImage1(qr1);
        }
        
        // Marbete 2 (si existe)
        if (m2 != null) {
            BufferedImage qr2 = qrGeneratorService.generarQR(String.valueOf(m2.getFolio()));
            ProductEntity p2 = productRepository.findById(m2.getProductId()).orElse(null);
            WarehouseEntity w2 = warehouseRepository.findById(m2.getWarehouseId()).orElse(null);
            
            dto.setNomMarbete2(String.valueOf(m2.getFolio()));
            dto.setClave2(p2 != null ? p2.getCveArt() : "N/A");
            dto.setDescr2(p2 != null ? p2.getDescr() : "N/A");
            dto.setAlmacen2(w2 != null ? w2.getNameWarehouse() : "N/A");
            dto.setFecha2(m2.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setQrImage2(qr2);
        }
        
        // Marbete 3 (si existe)
        if (m3 != null) {
            BufferedImage qr3 = qrGeneratorService.generarQR(String.valueOf(m3.getFolio()));
            ProductEntity p3 = productRepository.findById(m3.getProductId()).orElse(null);
            WarehouseEntity w3 = warehouseRepository.findById(m3.getWarehouseId()).orElse(null);
            
            dto.setNomMarbete3(String.valueOf(m3.getFolio()));
            dto.setClave3(p3 != null ? p3.getCveArt() : "N/A");
            dto.setDescr3(p3 != null ? p3.getDescr() : "N/A");
            dto.setAlmacen3(w3 != null ? w3.getNameWarehouse() : "N/A");
            dto.setFecha3(m3.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setQrImage3(qr3);
        }
        
        return dto;
    }

    /**
     * Genera QR para un único marbete
     * (Método heredado - mantiene compatibilidad)
     * 
     * @param label El marbete
     * @return DTO con QR incluido (solo en posición 1)
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
        String fecha = label.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        
        // Crear DTO usando el nuevo formato (en posición 1)
        MarbeteReportDTO dto = new MarbeteReportDTO();
        dto.setNomMarbete1(String.valueOf(label.getFolio()));
        dto.setClave1(clave);
        dto.setDescr1(descripcion);
        dto.setAlmacen1(nombreAlmacen);
        dto.setFecha1(fecha);
        dto.setQrImage1(qrImage);
        
        log.debug("✅ QR generado para marbete {}", label.getFolio());
        return dto;
    }

    /**
     * Genera QR para una lista específica de folios (números de marbete)
     * TAMBIÉN AGRUPADOS DE 3 EN 3
     * 
     * @param folios Lista de folios (ej: [42, 43, 44])
     * @param periodId ID del período
     * @param warehouseId ID del almacén
     * @return Lista de DTOs con QR (agrupados de 3 en 3)
     */
    public List<MarbeteReportDTO> generarMarbetesEspecificosConQR(List<Long> folios, Long periodId, Long warehouseId) {
        log.info("Generando {} marbetes específicos con QR (agrupados de 3 en 3)", folios.size());
        
        List<Label> marbetesObtenidos = new ArrayList<>();
        
        for (Long folio : folios) {
            try {
                Label label = labelRepository.findByFolioAndPeriodIdAndWarehouseId(folio, periodId, warehouseId)
                    .orElse(null);
                    
                if (label != null) {
                    marbetesObtenidos.add(label);
                } else {
                    log.warn("Marbete no encontrado: folio={}, periodId={}, warehouseId={}", folio, periodId, warehouseId);
                }
            } catch (Exception e) {
                log.error("Error procesando marbete folio {}: {}", folio, e.getMessage());
            }
        }
        
        // Agrupar de 3 en 3
        List<MarbeteReportDTO> resultado = new ArrayList<>();
        for (int i = 0; i < marbetesObtenidos.size(); i += 3) {
            try {
                Label m1 = marbetesObtenidos.get(i);
                Label m2 = (i + 1 < marbetesObtenidos.size()) ? marbetesObtenidos.get(i + 1) : null;
                Label m3 = (i + 2 < marbetesObtenidos.size()) ? marbetesObtenidos.get(i + 2) : null;
                
                resultado.add(agruparTresMarbetes(m1, m2, m3));
                
            } catch (Exception e) {
                log.error("Error agrupando marbetes en índice {}: {}", i, e.getMessage());
            }
        }
        
        return resultado;
    }

    /**
     * Genera código de barras en lugar de QR (alternativa)
     * 
     * @param label El marbete
     * @return DTO con código de barras (en posición 1)
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
        String fecha = label.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        
        // Crear DTO usando el nuevo formato (en posición 1)
        MarbeteReportDTO dto = new MarbeteReportDTO();
        dto.setNomMarbete1(String.valueOf(label.getFolio()));
        dto.setClave1(clave);
        dto.setDescr1(descripcion);
        dto.setAlmacen1(nombreAlmacen);
        dto.setFecha1(fecha);
        dto.setQrImage1(barcodeImage);
        
        return dto;
    }
}

