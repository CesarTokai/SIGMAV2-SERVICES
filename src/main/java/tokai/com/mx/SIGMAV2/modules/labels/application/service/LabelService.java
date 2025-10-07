package tokai.com.mx.SIGMAV2.modules.labels.application.service;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchDTO;
import tokai.com.mx.SIGMAV2.modules.labels.application.dto.LabelRequestDTO;

public interface LabelService {

    void requestLabels(LabelRequestDTO dto, Long userId);

    void generateBatch(GenerateBatchDTO dto, Long userId);
}

