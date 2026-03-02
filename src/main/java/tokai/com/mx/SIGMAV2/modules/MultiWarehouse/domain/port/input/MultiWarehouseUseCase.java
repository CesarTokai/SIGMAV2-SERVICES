package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.port.input;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseSearchDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.adapter.web.dto.MultiWarehouseWizardStepDTO;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.ExportResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.ImportResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result.WizardStepResult;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseExistence;
import tokai.com.mx.SIGMAV2.modules.MultiWarehouse.domain.model.MultiWarehouseImportLog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de entrada (input port) del caso de uso MultiWarehouse.
 *
 * <p>Sigue el patrón de Arquitectura Hexagonal:
 * <ul>
 *   <li>Este puerto es la única puerta de entrada al dominio desde la capa de adaptadores.</li>
 *   <li>Retorna objetos de dominio/aplicación — nunca {@code ResponseEntity} ni clases HTTP.</li>
 *   <li>El adaptador web (controlador) es responsable de convertir estos resultados a HTTP.</li>
 * </ul>
 *
 * <p>Comparar con {@link tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input.WarehouseService}
 * que aplica el mismo patrón en el módulo Warehouse.
 */
public interface MultiWarehouseUseCase {

    // -------------------------------------------------------------------------
    // Consulta
    // -------------------------------------------------------------------------

    /**
     * Busca existencias de multialmacén con filtros opcionales y paginación.
     */
    Page<MultiWarehouseExistence> findExistences(MultiWarehouseSearchDTO search, Pageable pageable);

    /**
     * Obtiene el stock de un producto en un almacén para un periodo dado.
     *
     * @return Optional vacío si no existe el registro
     */
    Optional<BigDecimal> getStock(String productCode, String warehouseKey, Long periodId);

    /**
     * Obtiene el log de una importación por su ID.
     */
    Optional<MultiWarehouseImportLog> getImportLog(Long id);

    /**
     * Obtiene los productos marcados como BAJA (status=B) en un periodo.
     */
    List<MultiWarehouseExistence> getProductosDadosDeBaja(Long periodId);

    // -------------------------------------------------------------------------
    // Importación
    // -------------------------------------------------------------------------

    /**
     * Importa un archivo Excel/CSV de existencias multialmacén para un periodo.
     *
     * <p>Aplica las reglas de negocio RN-MWH-002 a RN-MWH-006:
     * crear almacenes/productos faltantes, actualizar existentes, marcar bajas.
     *
     * @return {@link ImportResult} con el detalle del proceso (éxito, warnings, totales)
     */
    ImportResult importFile(MultipartFile file, String period);

    // -------------------------------------------------------------------------
    // Exportación
    // -------------------------------------------------------------------------

    /**
     * Exporta las existencias filtradas en formato CSV.
     *
     * @return {@link ExportResult} con los bytes del CSV y metadatos para la descarga
     */
    ExportResult exportExistences(MultiWarehouseSearchDTO search);

    // -------------------------------------------------------------------------
    // Wizard
    // -------------------------------------------------------------------------

    /**
     * Procesa un paso del wizard de importación.
     *
     * @return {@link WizardStepResult} con el resultado del paso
     */
    WizardStepResult processWizardStep(MultiWarehouseWizardStepDTO stepDTO);
}

