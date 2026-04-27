package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.MarbeteReportDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.ProductInfoPort;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.ProductInfoPort.ProductInfo;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.WarehouseInfoPort;
import tokai.com.mx.SIGMAV2.modules.labels.domain.port.output.WarehouseInfoPort.WarehouseInfo;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;

import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ProductInfoPort productInfoPort;
    private final WarehouseInfoPort warehouseInfoPort;

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

        // Excluir marbetes en estado GENERADO (no deben imprimirse)
        marbetes = marbetes.stream()
                .filter(m -> m.getEstado() != Label.State.GENERADO)
                .toList();
        
        List<MarbeteReportDTO> gruposAgrupadosPor3 = buildMarbeteGroups(marbetes);
        log.info("✅ {} grupos de marbetes (3 por fila) procesados con QR", gruposAgrupadosPor3.size());
        return gruposAgrupadosPor3;
    }

    private List<MarbeteReportDTO> buildMarbeteGroups(List<Label> labels) {
        Set<Long> productIds = labels.stream().map(Label::getProductId).collect(Collectors.toSet());
        Set<Long> warehouseIds = labels.stream().map(Label::getWarehouseId).collect(Collectors.toSet());

        Map<Long, ProductInfo> productCache = productInfoPort.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductInfo::id, p -> p));
        Map<Long, WarehouseInfo> warehouseCache = warehouseInfoPort.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseInfo::id, w -> w));

        List<MarbeteReportDTO> result = new ArrayList<>();
        for (int i = 0; i < labels.size(); i += 3) {
            try {
                Label m1 = labels.get(i);
                Label m2 = (i + 1 < labels.size()) ? labels.get(i + 1) : null;
                Label m3 = (i + 2 < labels.size()) ? labels.get(i + 2) : null;
                result.add(agruparTresMarbetes(m1, m2, m3, productCache, warehouseCache));
            } catch (Exception e) {
                log.error("Error agrupando marbetes en índice {}: {}", i, e.getMessage());
            }
        }
        return result;
    }

    private MarbeteReportDTO agruparTresMarbetes(Label m1, Label m2, Label m3,
            Map<Long, ProductInfo> productCache, Map<Long, WarehouseInfo> warehouseCache) {
        MarbeteReportDTO dto = new MarbeteReportDTO();

        if (m1 != null) {
            ProductInfo p1 = productCache.get(m1.getProductId());
            WarehouseInfo w1 = warehouseCache.get(m1.getWarehouseId());
            dto.setNomMarbete1(String.valueOf(m1.getFolio()));
            dto.setClave1(p1 != null ? p1.cveArt() : "N/A");
            dto.setDescr1(p1 != null ? p1.descr() : "N/A");
            dto.setAlmacen1(w1 != null ? w1.nameWarehouse() : "N/A");
            dto.setFecha1(m1.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setQrImage1(qrGeneratorService.generarQR(String.valueOf(m1.getFolio())));
        }

        if (m2 != null) {
            ProductInfo p2 = productCache.get(m2.getProductId());
            WarehouseInfo w2 = warehouseCache.get(m2.getWarehouseId());
            dto.setNomMarbete2(String.valueOf(m2.getFolio()));
            dto.setClave2(p2 != null ? p2.cveArt() : "N/A");
            dto.setDescr2(p2 != null ? p2.descr() : "N/A");
            dto.setAlmacen2(w2 != null ? w2.nameWarehouse() : "N/A");
            dto.setFecha2(m2.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setQrImage2(qrGeneratorService.generarQR(String.valueOf(m2.getFolio())));
        }

        if (m3 != null) {
            ProductInfo p3 = productCache.get(m3.getProductId());
            WarehouseInfo w3 = warehouseCache.get(m3.getWarehouseId());
            dto.setNomMarbete3(String.valueOf(m3.getFolio()));
            dto.setClave3(p3 != null ? p3.cveArt() : "N/A");
            dto.setDescr3(p3 != null ? p3.descr() : "N/A");
            dto.setAlmacen3(w3 != null ? w3.nameWarehouse() : "N/A");
            dto.setFecha3(m3.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setQrImage3(qrGeneratorService.generarQR(String.valueOf(m3.getFolio())));
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
        
        BufferedImage qrImage = qrGeneratorService.generarQR(String.valueOf(label.getFolio()));

        ProductInfo product = productInfoPort.findById(label.getProductId()).orElse(null);
        String clave = product != null ? product.cveArt() : "N/A";
        String descripcion = product != null ? product.descr() : "N/A";

        WarehouseInfo warehouse = warehouseInfoPort.findById(label.getWarehouseId()).orElse(null);
        String nombreAlmacen = warehouse != null ? warehouse.nameWarehouse() : "N/A";
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
        
        List<Label> marbetesObtenidos = labelRepository
                .findByFolioInAndPeriodIdAndWarehouseId(folios, periodId, warehouseId)
                .stream()
                .filter(m -> {
                    if (m.getEstado() == Label.State.GENERADO) {
                        log.warn("Marbete {} está en estado GENERADO — se omite de impresión", m.getFolio());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        Set<Long> requestedFolios = new java.util.HashSet<>(folios);
        Set<Long> foundFolios = marbetesObtenidos.stream().map(Label::getFolio).collect(Collectors.toSet());
        requestedFolios.stream()
                .filter(f -> !foundFolios.contains(f))
                .forEach(f -> log.warn("Marbete no encontrado: folio={}, periodId={}, warehouseId={}", f, periodId, warehouseId));

        return buildMarbeteGroups(marbetesObtenidos);
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
        
        ProductInfo product = productInfoPort.findById(label.getProductId()).orElse(null);
        String clave = product != null ? product.cveArt() : "N/A";
        String descripcion = product != null ? product.descr() : "N/A";

        WarehouseInfo warehouse = warehouseInfoPort.findById(label.getWarehouseId()).orElse(null);
        String nombreAlmacen = warehouse != null ? warehouse.nameWarehouse() : "N/A";
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

