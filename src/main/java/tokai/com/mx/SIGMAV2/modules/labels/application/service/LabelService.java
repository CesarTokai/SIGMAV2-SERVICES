package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
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

    void generateBatch(GenerateBatchDTO dto, Long userId, String userRole);

    LabelPrint printLabels(PrintRequestDTO dto, Long userId, String userRole);

    LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole);

    LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole);

    List<LabelSummaryResponseDTO> getLabelSummary(LabelSummaryRequestDTO dto, Long userId, String userRole);
}
