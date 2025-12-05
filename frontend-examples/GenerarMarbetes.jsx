// ============================================
// COMPONENTE REACT: GENERAR MARBETES
// ============================================

import React, { useState, useEffect } from 'react';
import MarbetesService from '../services/marbetes-service';
import Swal from 'sweetalert2'; // npm install sweetalert2

const GenerarMarbetes = () => {
  const [periodos, setPeriodos] = useState([]);
  const [almacenes, setAlmacenes] = useState([]);
  const [productos, setProductos] = useState([]);

  const [selectedPeriodo, setSelectedPeriodo] = useState(null);
  const [selectedAlmacen, setSelectedAlmacen] = useState(null);
  const [loading, setLoading] = useState(false);

  // Cargar datos iniciales
  useEffect(() => {
    cargarPeriodos();
    cargarAlmacenes();
  }, []);

  useEffect(() => {
    if (selectedPeriodo && selectedAlmacen) {
      cargarResumen();
    }
  }, [selectedPeriodo, selectedAlmacen]);

  const cargarPeriodos = async () => {
    // Implementar según tu API de periodos
    // Ejemplo:
    setPeriodos([
      { id: 1, nombre: 'Periodo 2025' },
      { id: 2, nombre: 'Periodo 2024' }
    ]);
  };

  const cargarAlmacenes = async () => {
    // Implementar según tu API de almacenes
    setAlmacenes([
      { id: 15, nombre: 'Almacén 15', clave: '15' }
    ]);
  };

  const cargarResumen = async () => {
    setLoading(true);
    const result = await MarbetesService.consultarResumen(
      selectedPeriodo,
      selectedAlmacen,
      0,
      100
    );

    if (result.success) {
      setProductos(result.data);
    } else {
      Swal.fire('Error', result.error, 'error');
    }
    setLoading(false);
  };

  const handleSolicitarFolios = async (productId) => {
    const { value: cantidad } = await Swal.fire({
      title: 'Solicitar Folios',
      input: 'number',
      inputLabel: '¿Cuántos folios necesitas?',
      inputPlaceholder: 'Ingresa la cantidad',
      showCancelButton: true,
      inputValidator: (value) => {
        if (!value || value <= 0) {
          return 'Ingresa una cantidad válida';
        }
      }
    });

    if (cantidad) {
      setLoading(true);
      const result = await MarbetesService.solicitarFolios(
        productId,
        selectedAlmacen,
        selectedPeriodo,
        parseInt(cantidad)
      );

      if (result.success) {
        await Swal.fire({
          icon: 'success',
          title: '✅ Folios Solicitados',
          text: `Se solicitaron ${cantidad} folios correctamente`,
          timer: 2000
        });
        cargarResumen();
      } else {
        Swal.fire('Error', result.error, 'error');
      }
      setLoading(false);
    }
  };

  const handleGenerarMarbetes = async (productId, foliosSolicitados) => {
    // Confirmación
    const confirmacion = await Swal.fire({
      title: '¿Generar Marbetes?',
      text: `Se generarán ${foliosSolicitados} marbete(s)`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, generar',
      cancelButtonText: 'Cancelar'
    });

    if (!confirmacion.isConfirmed) return;

    setLoading(true);

    // LLAMADA A LA API CON VALIDACIÓN DE EXISTENCIAS
    const result = await MarbetesService.generarMarbetes(
      productId,
      selectedAlmacen,
      selectedPeriodo,
      foliosSolicitados
    );

    if (result.success) {
      const { data } = result;

      // MODAL CON INFORMACIÓN DETALLADA (NUEVO)
      if (data.sinExistencias > 0) {
        // Hay marbetes sin existencias
        await Swal.fire({
          icon: 'warning',
          title: '⚠️ Generación con Advertencias',
          html: `
            <div style="text-align: left; padding: 15px;">
              <p><strong>📊 Resumen de Generación:</strong></p>
              <hr>
              <p>🔢 <strong>Total generados:</strong> ${data.totalGenerados}</p>
              <p style="color: green;">✅ <strong>Con existencias:</strong> ${data.conExistencias}</p>
              <p style="color: red;">❌ <strong>Sin existencias (cancelados):</strong> ${data.sinExistencias}</p>
              <p>📋 <strong>Rango de folios:</strong> ${data.primerFolio} - ${data.ultimoFolio}</p>
              <hr>
              <p style="font-size: 12px; color: #666;">
                Los marbetes sin existencias están en estado CANCELADO.<br>
                Puedes consultarlos y actualizar existencias en la sección
                <strong>"Marbetes Cancelados"</strong>.
              </p>
            </div>
          `,
          confirmButtonText: 'Entendido',
          width: 600
        });
      } else {
        // Todos los marbetes se generaron correctamente
        await Swal.fire({
          icon: 'success',
          title: '✅ Generación Exitosa',
          html: `
            <div style="text-align: left; padding: 15px;">
              <p><strong>Se generaron ${data.totalGenerados} marbete(s) correctamente</strong></p>
              <p>📋 <strong>Rango de folios:</strong> ${data.primerFolio} - ${data.ultimoFolio}</p>
              <p style="font-size: 12px; color: #666; margin-top: 10px;">
                Los marbetes están listos para impresión
              </p>
            </div>
          `,
          timer: 3000
        });
      }

      cargarResumen(); // Recargar lista
    } else {
      Swal.fire('Error', result.error, 'error');
    }

    setLoading(false);
  };

  return (
    <div className="container-fluid">
      <h2>Generar Marbetes</h2>

      {/* Filtros */}
      <div className="row mb-4">
        <div className="col-md-6">
          <label>Periodo</label>
          <select
            className="form-control"
            value={selectedPeriodo || ''}
            onChange={(e) => setSelectedPeriodo(parseInt(e.target.value))}
          >
            <option value="">Seleccione periodo</option>
            {periodos.map(p => (
              <option key={p.id} value={p.id}>{p.nombre}</option>
            ))}
          </select>
        </div>

        <div className="col-md-6">
          <label>Almacén</label>
          <select
            className="form-control"
            value={selectedAlmacen || ''}
            onChange={(e) => setSelectedAlmacen(parseInt(e.target.value))}
          >
            <option value="">Seleccione almacén</option>
            {almacenes.map(a => (
              <option key={a.id} value={a.id}>{a.nombre}</option>
            ))}
          </select>
        </div>
      </div>

      {/* Tabla de Productos */}
      {selectedPeriodo && selectedAlmacen && (
        <div className="table-responsive">
          <table className="table table-striped table-hover">
            <thead className="thead-dark">
              <tr>
                <th>Clave</th>
                <th>Producto</th>
                <th>Existencias</th>
                <th>Folios Solicitados</th>
                <th>Folios Generados</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" className="text-center">
                    <div className="spinner-border" role="status">
                      <span className="sr-only">Cargando...</span>
                    </div>
                  </td>
                </tr>
              ) : productos.length === 0 ? (
                <tr>
                  <td colSpan="6" className="text-center">
                    No hay productos disponibles
                  </td>
                </tr>
              ) : (
                productos.map(producto => (
                  <tr key={producto.productId}>
                    <td>{producto.claveProducto}</td>
                    <td>{producto.nombreProducto}</td>
                    <td>
                      <span className={producto.existencias > 0 ? 'text-success' : 'text-danger'}>
                        {producto.existencias}
                      </span>
                    </td>
                    <td>{producto.foliosSolicitados}</td>
                    <td>{producto.foliosExistentes}</td>
                    <td>
                      {producto.foliosSolicitados === 0 ? (
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => handleSolicitarFolios(producto.productId)}
                          disabled={loading}
                        >
                          📝 Solicitar Folios
                        </button>
                      ) : producto.foliosExistentes < producto.foliosSolicitados ? (
                        <button
                          className="btn btn-sm btn-success"
                          onClick={() => handleGenerarMarbetes(
                            producto.productId,
                            producto.foliosSolicitados - producto.foliosExistentes
                          )}
                          disabled={loading}
                        >
                          🔨 Generar Marbetes
                        </button>
                      ) : (
                        <span className="badge badge-success">✅ Completado</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default GenerarMarbetes;

