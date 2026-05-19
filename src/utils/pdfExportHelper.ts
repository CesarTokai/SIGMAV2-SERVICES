import axiosConfiguration from '@/config/axiosConfig';
import { ToastError, LoadAlert } from '@/utils/SweetAlert';

interface ExportPDFOptions {
  endpoint: string;
  periodId: number;
  warehouseId?: number | null;
  fileName: string;
  onStart?: () => void;
  onComplete?: () => void;
}

/**
 * Exporta un reporte a PDF
 * @param options Opciones de exportación
 */
export const exportReportToPDF = async (options: ExportPDFOptions): Promise<void> => {
  try {
    options.onStart?.();
    LoadAlert(true);

    const body = {
      periodId: options.periodId,
      ...(options.warehouseId !== undefined && options.warehouseId !== null && { warehouseId: options.warehouseId })
    };

    const response = await axiosConfiguration.doPost(
      options.endpoint,
      body,
      { responseType: 'blob' }
    );

    // Crear blob y descargar
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', options.fileName);
    document.body.appendChild(link);
    link.click();
    link.parentNode?.removeChild(link);
    window.URL.revokeObjectURL(url);

    LoadAlert(false);
  } catch (error) {
    LoadAlert(false);
    console.error('Error al exportar PDF:', error);
    ToastError('Error', 'No se pudo exportar el reporte a PDF');
  } finally {
    options.onComplete?.();
  }
};

