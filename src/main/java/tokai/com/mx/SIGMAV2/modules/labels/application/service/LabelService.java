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
    LabelCountEvent updateCountC1(CountEventDTO dto, Long userId, String userRole);

    LabelCountEvent updateCountC2(CountEventDTO dto, Long userId, String userRole);

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
     * 📄 GET /labels/{folio}/pdf
     * Obtiene el PDF de un marbete ya impreso
     * @param folio ID del folio
     * @param periodId ID del periodo (requerido para buscar el marbete)
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return byte[] con el PDF del marbete
     */
    byte[] getPrintedLabelPdf(Long folio, Long periodId, Long userId, String userRole);

    /**
     * 🔄 POST /labels/{folio}/reprint-simple
     * Reimprimir un marbete ya impreso actualizando solo el timestamp
     * Sin cambiar su estado ni generar nuevos folios
     * @param folio ID del folio
     * @param periodId ID del periodo (requerido para buscar el marbete)
     * @param userId ID del usuario que reimprimen
     * @param userRole Rol del usuario
     * @return byte[] con el PDF reimprimido
     */
    byte[] reprintSimple(Long folio, Long periodId, Long userId, String userRole);

    /**
     * 📋 GET /labels/{folio}/full-info
     * Obtiene TODA la información de un marbete en un solo lugar
     * Incluye: datos del marbete, usuario que lo registró, conteos,
     * impresiones, cancelaciones, historial completo, existencias, etc.
     * 
     * @param folio ID del folio del marbete
     * @param periodId ID del periodo (requerido para buscar el marbete)
     * @param userId ID del usuario consultando
     * @param userRole Rol del usuario
     * @return DTO con toda la información del marbete
     */
    LabelFullDetailDTO getLabelFullDetail(Long folio, Long periodId, Long userId, String userRole);

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

    /**
     * 📋 GET /labels/selected-info
     * Consulta la información de marbetes específicos (por folios)
     * Útil para que el usuario vea los datos antes de imprimir
     * 
     * @param folios Lista de folios a consultar
     * @param periodId Período de los marbetes
     * @param warehouseId Almacén de los marbetes
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return Lista con información de los marbetes seleccionados
     */
    java.util.List<LabelDetailForPrintDTO> getSelectedLabelsInfo(
            java.util.List<Long> folios, Long periodId, Long warehouseId, Long userId, String userRole);

    /**
     * 🖨️ POST /labels/print-selected
     * Imprime marbetes específicos con su información completa
     * El usuario proporciona los folios y se genera PDF con su información
     * 
     * @param request DTO con folios a imprimir
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return byte[] PDF con los marbetes y su información
     */
    byte[] printSelectedLabelsWithInfo(PrintSelectedLabelsRequestDTO request, Long userId, String userRole);

    /**
     * 🖨️ POST /labels/print-selected-auto
     * Imprime marbetes específicos de múltiples almacenes SIN requerir warehouseId
     * El sistema autodetecta el almacén de cada marbete automáticamente
     * 
     * @param request DTO con folios a imprimir (periodo, folios, infoType)
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return byte[] PDF con los marbetes y su información
     * @throws IllegalArgumentException si los marbetes están en diferentes periodos
     */
    byte[] printSelectedLabelsAutoWarehouse(PrintSelectedLabelsAutoWarehouseDTO request, Long userId, String userRole);

    /**
     * 📊 GET /labels/report/with-comments
     * Reporte de Marbetes con Comentarios de Conteos
     * Retorna lista de marbetes con:
     * - Información del producto y almacén
     * - Conteos C1 y C2 con sus comentarios
     * - Análisis de diferencias
     * 
     * @param filter DTO con filtros (periodId, warehouseId)
     * @param userId ID del usuario consultando
     * @param userRole Rol del usuario
     * @return Lista de marbetes con comentarios de conteos
     */
    List<LabelWithCommentsReportDTO> getLabelListWithComments(ReportFilterDTO filter, Long userId, String userRole);

    /**
     * 🖨️ POST /labels/print-selected-with-qr
     * Imprime marbetes específicos CON QR
     * El usuario proporciona los folios y se genera PDF con QR incluido
     * 
     * @param request DTO con folios a imprimir
     * @param userId ID del usuario
     * @param userRole Rol del usuario
     * @return byte[] PDF con los marbetes y sus códigos QR
     */
    byte[] printSelectedLabelsWithQR(PrintSelectedLabelsRequestDTO request, Long userId, String userRole);
 }
