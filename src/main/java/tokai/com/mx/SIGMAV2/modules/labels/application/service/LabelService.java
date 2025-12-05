package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchResponseDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.PrintRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelSummaryResponseDTO;
import java.util.List;

public interface LabelService {

    void requestLabels(LabelRequestDTO dto, Long userId, String userRole);

    GenerateBatchResponseDTO generateBatch(GenerateBatchDTO dto, Long userId, String userRole);

    void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole);

    byte[] printLabels(PrintRequestDTO dto, Long userId, String userRole);

    LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole);

    LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole);

    List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole);

    tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelStatusResponseDTO getLabelStatus(Long folio, Long periodId, Long warehouseId, Long userId, String userRole);

    long countLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId);

    // Métodos para marbetes cancelados
    List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO> getCancelledLabels(Long periodId, Long warehouseId, Long userId, String userRole);

    tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelCancelledDTO updateCancelledStock(tokai.com.mx.SIGMAV2.modules.labels.application.dto.UpdateCancelledStockDTO dto, Long userId, String userRole);

    // Método para obtener detalles de marbetes de un producto
    List<tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelDetailDTO> getLabelsByProduct(Long productId, Long periodId, Long warehouseId, Long userId, String userRole);
}
