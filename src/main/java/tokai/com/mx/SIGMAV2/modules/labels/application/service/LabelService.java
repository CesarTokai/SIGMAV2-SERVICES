package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import java.util.List;

public interface LabelService {

    void requestLabels(LabelRequestDTO dto, Long userId, String userRole);

    GenerateBatchResponseDTO generateBatch(GenerateBatchDTO dto, Long userId, String userRole);

    void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole);

    byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole);

    /**
     * 🔄 REIMPRESIÓN EXTRAORDINARIA: Reimprimir marbetes ya impresos
     * Método específico para reimpresión extraordinaria de marbetes en estado IMPRESO.
     *
     * @param dto DTO con folios específicos a reimprimir
     * @param userId ID del usuario que hace la reimpresión
     * @param userRole Rol del usuario
     * @return byte[] PDF con los marbetes reimprimidos
     */
    byte[] extraordinaryReprint(PrintRequestDTO dto, Long userId, String userRole);

    // Contar marbetes pendientes de impresión
    tokai.com.mx.SIGMAV2.modules.labels.application.dto.PendingPrintCountResponseDTO getPendingPrintCount(
        tokai.com.mx.SIGMAV2.modules.labels.application.dto.PendingPrintCountRequestDTO dto,
        Long userId,
        String userRole
    );

    LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole);

    LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole);

    // Métodos para actualizar conteos existentes
    LabelCountEvent updateCountC1(tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCountDTO dto, Long userId, String userRole);

    LabelCountEvent updateCountC2(tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCountDTO dto, Long userId, String userRole);

    List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole);

    tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelStatusResponseDTO getLabelStatus(Long folio, Long periodId, Long warehouseId, Long userId, String userRole);

    long countLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId);

    // Métodos para marbetes cancelados
    List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO> getCancelledLabels(Long periodId, Long warehouseId, Long userId, String userRole);

    tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO updateCancelledStock(tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCancelledStockDTO dto, Long userId, String userRole);

    // Método para obtener detalles de marbetes de un producto
    List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelDetailDTO> getLabelsByProduct(Long productId, Long periodId, Long warehouseId, Long userId, String userRole);

    // Método para cancelar un marbete
    void cancelLabel(CancelLabelRequestDTO dto, Long userId, String userRole);

    // Método para obtener un marbete por folio para la interfaz de conteo
    tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO getLabelForCount(Long folio, Long periodId, Long warehouseId, Long userId, String userRole);

    // Método para listar todos los marbetes disponibles para conteo en un periodo/almacén
    List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelForCountDTO> getLabelsForCountList(Long periodId, Long warehouseId, Long userId, String userRole);

    // Métodos de reportes
    List<DistributionReportDTO> getDistributionReport(ReportFilterDTO filter, Long userId, String userRole);

    List<LabelListReportDTO> getLabelListReport(ReportFilterDTO filter, Long userId, String userRole);

    List<PendingLabelsReportDTO> getPendingLabelsReport(ReportFilterDTO filter, Long userId, String userRole);

    List<DifferencesReportDTO> getDifferencesReport(ReportFilterDTO filter, Long userId, String userRole);

    List<CancelledLabelsReportDTO> getCancelledLabelsReport(ReportFilterDTO filter, Long userId, String userRole);

    List<ComparativeReportDTO> getComparativeReport(ReportFilterDTO filter, Long userId, String userRole);

    List<WarehouseDetailReportDTO> getWarehouseDetailReport(ReportFilterDTO filter, Long userId, String userRole);

    List<ProductDetailReportDTO> getProductDetailReport(ReportFilterDTO filter, Long userId, String userRole);

    // Método para generar archivo TXT de existencias
    tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateFileResponseDTO generateInventoryFile(Long periodId, Long userId, String userRole);

    /**
     * 🔍 GET /labels/{folio}/pdf
     * Obtiene el PDF de un marbete ya impreso
     * @param folio ID del folio
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return byte[] con el PDF del marbete
     */
    byte[] getPrintedLabelPdf(Long folio, Long userId, String userRole);

    /**
     * 🔄 POST /labels/{folio}/reprint-simple
     * Reimprimir un marbete ya impreso actualizando solo el timestamp
     * Sin cambiar su estado ni generar nuevos folios
     * @param folio ID del folio
     * @param userId ID del usuario que reimprimen
     * @param userRole Rol del usuario
     * @return byte[] con el PDF reimprimido
     */
    byte[] reprintSimple(Long folio, Long userId, String userRole);

    /**
     * 📋 GET /labels/{folio}/full-info
     * Obtiene TODA la información de un marbete en un solo lugar
     * Incluye: datos del marbete, usuario que lo registró, conteos,
     * impresiones, cancelaciones, historial completo, existencias, etc.
     * 
     * @param folio ID del folio del marbete
     * @param userId ID del usuario consultando
     * @param userRole Rol del usuario
     * @return DTO con toda la información del marbete
     */
    LabelFullDetailDTO getLabelFullDetail(Long folio, Long userId, String userRole);

    /**
     * 📊 GET /labels/full-list
     * Obtiene la lista COMPLETA de todos los marbetes con información detallada
     * Soporta paginación, filtros y búsqueda
     * @param filter DTO con filtros, búsqueda, paginación y ordenamiento
     * @param userId ID del usuario consultando
     * @param userRole Rol del usuario
     * @return Página con lista de marbetes con información completa
     */
    org.springframework.data.domain.Page<LabelFullDetailDTO> getLabelFullDetailList(
            LabelListFilterDTO filter, Long userId, String userRole);
}


