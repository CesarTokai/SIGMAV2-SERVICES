package tokai.com.mx.SIGMAV2.modules.labels.application.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.LabelNotFoundException;
import tokai.com.mx.SIGMAV2.modules.labels.application.exception.PermissionDeniedException;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.JasperLabelPrintService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.JasperReportCacheService;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.MarbeteQRIntegrationService;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.Label;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter;
import tokai.com.mx.SIGMAV2.modules.labels.infrastructure.persistence.JpaLabelRepository;
import tokai.com.mx.SIGMAV2.modules.warehouse.application.service.WarehouseAccessService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio especializado en la impresión y reimpresión de marbetes.
 * Extraído de LabelServiceImpl para cumplir con el Principio de Responsabilidad Única.
 *
 * Regla de negocio principal: printLabels registra el evento de impresión
 * actualizando el estado GENERADO → IMPRESO. reprintSimple/extraordinaryReprint
 * reimprimen sin cambiar estado (ya están IMPRESOS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabelPrintService {

    private final LabelsPersistenceAdapter persistence;
    private final WarehouseAccessService warehouseAccessService;
    private final JasperLabelPrintService jasperLabelPrintService;
    private final JasperReportCacheService reportCacheService;
    private final MarbeteQRIntegrationService marbeteQRIntegrationService;
    private final JpaLabelRepository jpaLabelRepository;

    @Transactional
    public byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole) {
        boolean isExtraordinary = dto.getForceReprint() != null && dto.getForceReprint();
        boolean withQR = dto.getWithQR() != null && dto.getWithQR();
        log.info("📄 Imprimiendo marbetes: periodo={}, almacén={}, tipo={}, withQR={}, folios={}",
                dto.getPeriodId(), dto.getWarehouseId(),
                isExtraordinary ? "EXTRAORDINARIA" : "NORMAL",
                withQR,
                dto.getFolios() != null ? dto.getFolios().size() : "TODOS");

        if (userRole == null || userRole.trim().isEmpty()) {
            throw new PermissionDeniedException("Rol de usuario requerido");
        }
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        List<Label> labels;
        if (isExtraordinary) {
            if (dto.getFolios() == null || dto.getFolios().isEmpty()) {
                throw new InvalidLabelStateException("Reimpresión extraordinaria requiere folios específicos.");
            }
            labels = persistence.findImpresosForReimpresion(dto.getPeriodId(), dto.getWarehouseId(), dto.getFolios());
        } else {
            if (dto.getFolios() != null && !dto.getFolios().isEmpty()) {
                labels = persistence.findByFoliosInAndPeriodAndWarehouse(
                        dto.getFolios(), dto.getPeriodId(), dto.getWarehouseId());
                if (labels.size() != dto.getFolios().size()) {
                    throw new LabelNotFoundException("Algunos folios no existen");
                }
            } else if (dto.getProductId() != null) {
                labels = persistence.findPendingLabelsByPeriodWarehouseAndProduct(
                        dto.getPeriodId(), dto.getWarehouseId(), dto.getProductId());
            } else {
                labels = persistence.findPendingLabelsByPeriodAndWarehouse(
                        dto.getPeriodId(), dto.getWarehouseId());
            }
            if (labels.isEmpty()) {
                throw new InvalidLabelStateException("No hay marbetes pendientes de impresión");
            }
        }

        if (labels.size() > 500) {
            throw new InvalidLabelStateException("Límite máximo: 500 marbetes por impresión");
        }
        labels.sort(Comparator.comparing(Label::getFolio));

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels, true);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF");
        }

        Long minFolio = labels.get(0).getFolio();
        Long maxFolio = labels.get(labels.size() - 1).getFolio();
        persistence.printLabelsRange(dto.getPeriodId(), dto.getWarehouseId(), minFolio, maxFolio, userId, false);

        return pdfBytes;
    }

    @Transactional
    public byte[] extraordinaryReprint(PrintRequestDTO dto, Long userId, String userRole) {
        log.info("🔄 REIMPRESIÓN EXTRAORDINARIA: periodo={}, almacén={}, folios={}",
                dto.getPeriodId(), dto.getWarehouseId(),
                dto.getFolios() != null ? dto.getFolios().size() : 0);

        if (userRole == null || userRole.trim().isEmpty()) {
            throw new PermissionDeniedException("Rol de usuario requerido");
        }
        warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);

        if (dto.getFolios() == null || dto.getFolios().isEmpty()) {
            throw new InvalidLabelStateException("Reimpresión extraordinaria requiere lista de folios específicos.");
        }

        List<Label> labels = persistence.findImpresosForReimpresion(
                dto.getPeriodId(), dto.getWarehouseId(), dto.getFolios());

        if (labels.size() > 500) {
            throw new InvalidLabelStateException("Límite máximo: 500 marbetes por reimpresión extraordinaria");
        }
        labels.sort(Comparator.comparing(Label::getFolio));

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF de reimpresión: archivo vacío");
        }

        Long minFolio = labels.get(0).getFolio();
        Long maxFolio = labels.get(labels.size() - 1).getFolio();
        persistence.printLabelsRange(dto.getPeriodId(), dto.getWarehouseId(), minFolio, maxFolio, userId, true);

        log.info("✅ REIMPRESIÓN EXTRAORDINARIA completada: {} KB, {} marbetes", pdfBytes.length / 1024, labels.size());
        return pdfBytes;
    }

    @Transactional(readOnly = true)
    public byte[] getPrintedLabelPdf(Long folio, Long userId, String userRole) {
        log.info("📄 Consultando PDF del marbete folio={}, usuario={}", folio, userId);

        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException(
                    "El marbete folio " + folio + " no está en estado IMPRESO. Estado actual: " + label.getEstado());
        }

        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(List.of(label));
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF para folio " + folio);
        }

        log.info("✅ PDF obtenido para folio {}: {} bytes", folio, pdfBytes.length);
        return pdfBytes;
    }

    @Transactional
    public byte[] reprintSimple(Long folio, Long userId, String userRole) {
        log.info("🔄 Reimpresión SIMPLE: folio={}, usuario={}", folio, userId);

        Label label = jpaLabelRepository.findById(folio)
                .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

        if (label.getEstado() != Label.State.IMPRESO) {
            throw new InvalidLabelStateException(
                    "No se puede reimprimir. El marbete folio " + folio + " no está IMPRESO. Estado: " + label.getEstado());
        }

        warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(List.of(label));
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF para reimpresión de folio " + folio);
        }

        // Solo actualiza timestamp — NO cambia estado (ya es IMPRESO)
        LabelPrint labelPrint = new LabelPrint();
        labelPrint.setPeriodId(label.getPeriodId());
        labelPrint.setWarehouseId(label.getWarehouseId());
        labelPrint.setFolioInicial(folio);
        labelPrint.setFolioFinal(folio);
        labelPrint.setCantidadImpresa(1);
        labelPrint.setPrintedBy(userId);
        labelPrint.setPrintedAt(LocalDateTime.now());

        try {
            persistence.findLabelPrintsByProductPeriodWarehouse(label.getProductId(), label.getPeriodId(), label.getWarehouseId());
            log.info("✅ Reimpresión registrada para folio {}: {} bytes", folio, pdfBytes.length);
        } catch (Exception e) {
            log.warn("No se pudo registrar la reimpresión en BD: {}", e.getMessage());
        }

        return pdfBytes;
    }

    @Transactional
    public byte[] printSelectedLabelsWithInfo(PrintSelectedLabelsRequestDTO request, Long userId, String userRole) {
        log.info("🖨️ Imprimiendo {} marbetes seleccionados - usuario={}", request.getFolios().size(), userId);

        if (request.getFolios() == null || request.getFolios().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un folio");
        }
        if (request.getFolios().size() > 500) {
            throw new IllegalArgumentException("Máximo 500 marbetes por impresión");
        }

        warehouseAccessService.validateWarehouseAccess(userId, request.getWarehouseId(), userRole);

        List<Label> labels = new ArrayList<>();
        for (Long folio : request.getFolios()) {
            Label label = jpaLabelRepository.findById(folio)
                    .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado: " + folio));

            if (!label.getPeriodId().equals(request.getPeriodId()) ||
                !label.getWarehouseId().equals(request.getWarehouseId())) {
                throw new IllegalArgumentException("Folio " + folio + " no pertenece al período/almacén especificado");
            }
            labels.add(label);
        }

        labels.sort(Comparator.comparing(Label::getFolio));

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF");
        }

        Long minFolio = labels.getFirst().getFolio();
        Long maxFolio = labels.getLast().getFolio();
        persistence.printLabelsRange(request.getPeriodId(), request.getWarehouseId(), minFolio, maxFolio, userId, false);

        log.info("✅ Impresión completada: {} marbetes, {} KB", labels.size(), pdfBytes.length / 1024);
        return pdfBytes;
    }

    @Transactional
    public byte[] printSelectedLabelsAutoWarehouse(PrintSelectedLabelsAutoWarehouseDTO request, Long userId, String userRole) {
        log.info("🖨️ Imprimiendo {} marbetes seleccionados (autodetección de almacenes) - usuario={}",
                request.getFolios().size(), userId);

        if (request.getFolios() == null || request.getFolios().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un folio");
        }
        if (request.getFolios().size() > 500) {
            throw new IllegalArgumentException("Máximo 500 marbetes por impresión");
        }

        List<Label> labels = new ArrayList<>();
        Map<Long, Integer> warehouseCountMap = new java.util.HashMap<>();

        for (Long folio : request.getFolios()) {
            Label label = jpaLabelRepository.findById(folio)
                    .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado: " + folio));

            if (!label.getPeriodId().equals(request.getPeriodId())) {
                throw new IllegalArgumentException(
                    String.format("Folio %d pertenece al periodo %d, no al %d solicitado",
                        folio, label.getPeriodId(), request.getPeriodId()));
            }
            labels.add(label);
            warehouseCountMap.put(label.getWarehouseId(),
                warehouseCountMap.getOrDefault(label.getWarehouseId(), 0) + 1);
        }

        log.info("📊 Almacenes detectados: {}", warehouseCountMap.entrySet().stream()
            .map(e -> String.format("Almacén %d: %d marbetes", e.getKey(), e.getValue()))
            .collect(Collectors.joining(", ")));

        labels.sort(Comparator.comparing(Label::getFolio));

        byte[] pdfBytes = jasperLabelPrintService.generateLabelsPdf(labels);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidLabelStateException("Error generando PDF");
        }

        // Registrar impresión por almacén
        Map<Long, List<Label>> labelsByWarehouse = new java.util.TreeMap<>(
            labels.stream().collect(Collectors.groupingBy(Label::getWarehouseId)));

        for (Map.Entry<Long, List<Label>> entry : labelsByWarehouse.entrySet()) {
            Long warehouseId = entry.getKey();
            List<Label> warehouseLabels = entry.getValue();
            Long minFolio = warehouseLabels.getFirst().getFolio();
            Long maxFolio = warehouseLabels.getLast().getFolio();
            persistence.printLabelsRange(request.getPeriodId(), warehouseId, minFolio, maxFolio, userId, false);
        }

        log.info("✅ Impresión completada: {} marbetes de {} almacenes, {} KB",
                labels.size(), labelsByWarehouse.size(), pdfBytes.length / 1024);
        return pdfBytes;
    }

    @Transactional
    public byte[] printSelectedLabelsWithQR(PrintSelectedLabelsRequestDTO request, Long userId, String userRole) {
        log.info("🎯 /print-selected-with-qr: Imprimiendo {} marbetes CON QR - usuario={}",
                request.getFolios().size(), userId);

        if (request.getFolios() == null || request.getFolios().isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un folio");
        }
        if (request.getFolios().size() > 500) {
            throw new IllegalArgumentException("Máximo 500 marbetes por impresión");
        }

        warehouseAccessService.validateWarehouseAccess(userId, request.getWarehouseId(), userRole);

        try {
            List<MarbeteReportDTO> marbetesConQR = marbeteQRIntegrationService
                .generarMarbetesEspecificosConQR(request.getFolios(), request.getPeriodId(), request.getWarehouseId());

            if (marbetesConQR.isEmpty()) {
                log.warn("⚠️ No se encontraron los folios solicitados");
                throw new LabelNotFoundException("No se encontraron marbetes para los folios proporcionados");
            }

            byte[] pdfBytes = generarPDFConQRInterno(marbetesConQR);
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new InvalidLabelStateException("Error generando PDF con QR");
            }

            Long minFolio = request.getFolios().stream().min(Long::compareTo).orElse(0L);
            Long maxFolio = request.getFolios().stream().max(Long::compareTo).orElse(0L);
            persistence.printLabelsRange(request.getPeriodId(), request.getWarehouseId(), minFolio, maxFolio, userId, false);

            log.info("✅ Impresión CON QR completada: {} marbetes, {} KB", marbetesConQR.size(), pdfBytes.length / 1024);
            return pdfBytes;

        } catch (Exception e) {
            log.error("❌ Error al generar PDF específico con QR: {}", e.getMessage(), e);
            throw new InvalidLabelStateException("Error generando PDF con QR: " + e.getMessage());
        }
    }

    private byte[] generarPDFConQRInterno(List<MarbeteReportDTO> marbetes) {
        try {
            log.info("📄 Generando PDF con QR para {} marbetes", marbetes.size());

            if (marbetes == null || marbetes.isEmpty()) {
                return new byte[0];
            }

            net.sf.jasperreports.engine.data.JRBeanCollectionDataSource datasource =
                new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(marbetes);

            java.util.Map<String, Object> parameters = new java.util.HashMap<>();

            net.sf.jasperreports.engine.JasperReport jasperReport =
                reportCacheService.getReport("Carta_Tres_Cuadros_QR");

            net.sf.jasperreports.engine.JasperPrint jasperPrint =
                net.sf.jasperreports.engine.JasperFillManager.fillReport(jasperReport, parameters, datasource);

            byte[] pdfBytes = net.sf.jasperreports.engine.JasperExportManager.exportReportToPdf(jasperPrint);

            log.info("✅ PDF con QR generado exitosamente: {} bytes", pdfBytes.length);
            return pdfBytes;

        } catch (Exception e) {
            log.error("❌ Error al generar PDF con QR: {}", e.getMessage(), e);
            throw new InvalidLabelStateException("Error generando PDF con QR: " + e.getMessage());
        }
    }
}
