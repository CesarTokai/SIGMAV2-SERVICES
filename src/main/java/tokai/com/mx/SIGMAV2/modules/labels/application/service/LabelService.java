package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelPrint;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.PrintRequestDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.CountEventDTO;
import tokai.com.mx.SIGMAV2.modules.labels.domain.model.LabelCountEvent;

public interface LabelService {

    void requestLabels(LabelRequestDTO dto, Long userId);

    void generateBatch(GenerateBatchDTO dto, Long userId);

    LabelPrint printLabels(PrintRequestDTO dto, Long userId);

    LabelCountEvent registerCountC1(CountEventDTO dto, Long userId, String userRole);

    LabelCountEvent registerCountC2(CountEventDTO dto, Long userId, String userRole);
}
