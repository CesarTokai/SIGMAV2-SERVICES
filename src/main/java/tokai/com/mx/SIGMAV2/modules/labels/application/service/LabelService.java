package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchResponseDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.PrintRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryResponseDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CancelLabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.reports.*;
import java.util.List;

public interface LabelService {

    void requestLabels(LabelRequestDTO dto, Long userId, String userRole);

    GenerateBatchResponseDTO generateBatch(GenerateBatchDTO dto, Long userId, String userRole);

    void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole);

    byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole);

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
}


