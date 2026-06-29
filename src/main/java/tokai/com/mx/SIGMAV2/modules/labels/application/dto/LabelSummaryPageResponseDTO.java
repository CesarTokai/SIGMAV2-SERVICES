package tokai.com.mx.SIGMAV2.modules.labels.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para respuesta paginada del resumen de marbetes (/labels/summary).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelSummaryPageResponseDTO {

    private List<LabelSummaryResponseDTO> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
