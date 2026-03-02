package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.result;

import lombok.Builder;
import lombok.Value;

/**
 * Resultado del caso de uso de exportación de existencias multialmacén.
 *
 * <p>Contiene los bytes del CSV generado y los metadatos necesarios para
 * que el adaptador web construya la respuesta de descarga de archivo.
 */
@Value
@Builder
public class ExportResult {

    /** Bytes del archivo CSV listo para enviar al cliente. */
    byte[] csvBytes;

    /** Nombre sugerido para el archivo descargado. */
    String fileName;

    /** Content-Type del archivo (e.g. "text/csv; charset=UTF-8"). */
    String contentType;

    /** Número de filas de datos exportadas (sin contar la cabecera). */
    int totalRows;
}

