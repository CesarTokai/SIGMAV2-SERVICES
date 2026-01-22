# 📋 Recomendaciones para el Frontend - Sistema de Marbetes

## 🎯 Resumen Ejecutivo

Este documento proporciona **recomendaciones clave** para el desarrollo del frontend del sistema de marbetes, incluyendo mejores prácticas para la importación de archivos, manejo de APIs, validaciones y experiencia de usuario.

---

## 📂 1. IMPORTACIÓN DE ARCHIVOS

### 1.1 Importación de MultiAlmacén (Excel)

#### ✅ Validaciones en el Frontend

**Antes de enviar el archivo al backend:**

```typescript
// Validar extensión del archivo
const validExtensions = ['.xlsx', '.xls'];
const fileExtension = file.name.substring(file.name.lastIndexOf('.'));

if (!validExtensions.includes(fileExtension.toLowerCase())) {
  showError('Solo se permiten archivos Excel (.xlsx o .xls)');
  return;
}

// Validar tamaño del archivo (máximo 50MB)
const maxSize = 50 * 1024 * 1024; // 50MB en bytes
if (file.size > maxSize) {
  showError('El archivo no debe superar los 50MB');
  return;
}

// Validar que el periodo esté seleccionado
if (!selectedPeriod) {
  showError('Debe seleccionar un periodo antes de importar');
  return;
}
```

#### 🔄 Indicador de Progreso

```typescript
// Mostrar progreso durante la carga
const uploadFile = async (file: File, period: string) => {
  setLoading(true);
  setProgress(0);
  
  const formData = new FormData();
  formData.append('file', file);
  formData.append('period', period);
  
  try {
    const response = await fetch('/api/multiwarehouse/import', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Error al importar el archivo');
    }
    
    const result = await response.json();
    showSuccess(`Importación exitosa: ${result.totalImported} registros`);
    
    // Recargar datos
    await reloadMultiWarehouseData();
    
  } catch (error) {
    showError(error.message);
  } finally {
    setLoading(false);
  }
};
```

#### 📝 Mensajes de Ayuda para el Usuario

```typescript
// Mostrar información sobre el formato del archivo
const ImportHelp = () => (
  <div className="help-section">
    <h4>Formato del archivo Excel requerido:</h4>
    <ul>
      <li><strong>CVE_ALM:</strong> Clave del almacén (ej: "CEDIS", "TIENDA_MTY")</li>
      <li><strong>CVE_ART:</strong> Clave del producto (ej: "PROD_001")</li>
      <li><strong>DESCR:</strong> Descripción del producto (opcional)</li>
      <li><strong>STATUS:</strong> Estado del producto ("A" = Alta, "B" = Baja)</li>
      <li><strong>EXIST:</strong> Cantidad de existencias (número decimal)</li>
    </ul>
    <p className="note">
      ⚠️ Si el almacén o producto no existe, será creado automáticamente.
    </p>
    <a href="/docs/plantilla-multialmacen.xlsx" download>
      📥 Descargar plantilla de ejemplo
    </a>
  </div>
);
```

#### ⚠️ Manejo de Errores Específicos

```typescript
// Mapear errores del backend a mensajes amigables
const handleImportError = (errorCode: string, errorMessage: string) => {
  const errorMessages: Record<string, string> = {
    'PERIOD_CLOSED': 'No se puede importar: el periodo está cerrado. Por favor, contacte al administrador.',
    'PERIOD_LOCKED': 'No se puede importar: el periodo está bloqueado.',
    'INVALID_FORMAT': 'El archivo no tiene el formato correcto. Verifique que contenga las columnas: CVE_ALM, CVE_ART, STATUS, EXIST',
    'DUPLICATE_FILE': 'Este archivo ya fue importado anteriormente. No se permiten importaciones duplicadas.',
    'FILE_TOO_LARGE': 'El archivo es demasiado grande. Máximo permitido: 50MB',
    'EMPTY_FILE': 'El archivo está vacío o no contiene registros válidos.'
  };
  
  return errorMessages[errorCode] || errorMessage;
};
```

---

## 🏷️ 2. GENERACIÓN DE MARBETES

### 2.1 API Recomendada: `generateBatchList()`

**⚠️ IMPORTANTE:** Las APIs antiguas `requestLabels()` y `generateBatch()` están **deprecadas**.

#### ✅ Implementación Correcta

```typescript
interface GenerateBatchRequest {
  periodId: number;
  warehouseId: number;
  products: Array<{
    productId: number;
    quantity: number;
  }>;
}

// Generar marbetes para múltiples productos
const generateLabels = async (request: GenerateBatchRequest) => {
  try {
    const response = await fetch('/api/sigmav2/labels/generate/batch-list', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(request)
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    const result = await response.json();
    
    showSuccess(`Generados ${result.totalGenerated} marbetes`);
    
    // Mostrar resumen por producto
    result.results.forEach((item: any) => {
      console.log(`Producto ${item.productId}: ${item.generated} folios (${item.folioInicial} - ${item.folioFinal})`);
    });
    
    return result;
    
  } catch (error) {
    showError('Error al generar marbetes: ' + error.message);
    throw error;
  }
};
```

### 2.2 Validaciones Previas

```typescript
// Validar existencias antes de generar
const validateBeforeGenerate = async (
  productId: number, 
  quantity: number, 
  warehouseId: number, 
  periodId: number
) => {
  // Obtener existencias del producto
  const stock = await getInventoryStock(productId, warehouseId, periodId);
  
  if (!stock) {
    showWarning('El producto no tiene existencias registradas en el almacén');
    return false;
  }
  
  if (stock.existQty === 0) {
    const confirm = await showConfirmDialog(
      '⚠️ Este producto no tiene existencias',
      '¿Está seguro de que desea generar marbetes para un producto sin existencias?'
    );
    return confirm;
  }
  
  if (quantity > stock.existQty) {
    showWarning(
      `La cantidad solicitada (${quantity}) es mayor que las existencias (${stock.existQty})`
    );
  }
  
  return true;
};
```

### 2.3 Interfaz de Usuario Recomendada

```typescript
// Componente para selección masiva de productos
const BatchLabelGenerator = () => {
  const [selectedProducts, setSelectedProducts] = useState<Array<{
    productId: number;
    productName: string;
    stock: number;
    quantity: number;
  }>>([]);
  
  const addProduct = (product: any) => {
    setSelectedProducts([...selectedProducts, {
      productId: product.idProduct,
      productName: product.descr,
      stock: product.existQty,
      quantity: product.existQty // Sugerir cantidad = existencias
    }]);
  };
  
  const updateQuantity = (productId: number, newQuantity: number) => {
    setSelectedProducts(products => 
      products.map(p => 
        p.productId === productId 
          ? { ...p, quantity: newQuantity }
          : p
      )
    );
  };
  
  const generateAll = async () => {
    const request = {
      periodId,
      warehouseId,
      products: selectedProducts.map(p => ({
        productId: p.productId,
        quantity: p.quantity
      }))
    };
    
    await generateLabels(request);
  };
  
  return (
    <div>
      <table>
        <thead>
          <tr>
            <th>Producto</th>
            <th>Existencias</th>
            <th>Cantidad a Generar</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {selectedProducts.map(product => (
            <tr key={product.productId}>
              <td>{product.productName}</td>
              <td>{product.stock}</td>
              <td>
                <input 
                  type="number" 
                  value={product.quantity}
                  onChange={e => updateQuantity(product.productId, Number(e.target.value))}
                  min="0"
                  max={product.stock}
                />
              </td>
              <td>
                <button onClick={() => removeProduct(product.productId)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      
      <button 
        onClick={generateAll}
        disabled={selectedProducts.length === 0}
      >
        Generar {selectedProducts.length} producto(s)
      </button>
    </div>
  );
};
```

---

## 🖨️ 3. IMPRESIÓN DE MARBETES

### 3.1 API Simplificada (Sin Folios)

**✅ RECOMENDADO:** Impresión automática de todos los pendientes

```typescript
// Impresión automática de todos los marbetes pendientes
const printAllPendingLabels = async (periodId: number, warehouseId: number) => {
  try {
    const response = await fetch('/api/sigmav2/labels/print', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        periodId,
        warehouseId
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      
      if (error.message.includes('No hay marbetes pendientes')) {
        showWarning('No hay marbetes pendientes de impresión');
        return null;
      }
      
      throw new Error(error.message);
    }
    
    // Descargar PDF
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `marbetes_P${periodId}_A${warehouseId}_${Date.now()}.pdf`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
    
    showSuccess('Marbetes impresos correctamente');
    
  } catch (error) {
    showError('Error al imprimir: ' + error.message);
  }
};
```

### 3.2 Consultar Pendientes Antes de Imprimir

```typescript
// Obtener cantidad de marbetes pendientes
const getPendingCount = async (
  periodId: number, 
  warehouseId: number, 
  productId?: number
) => {
  try {
    const response = await fetch('/api/sigmav2/labels/pending-print/count', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        periodId,
        warehouseId,
        productId
      })
    });
    
    if (!response.ok) {
      throw new Error('Error al obtener conteo de pendientes');
    }
    
    const result = await response.json();
    return result.count;
    
  } catch (error) {
    console.error(error);
    return 0;
  }
};

// Mostrar confirmación antes de imprimir
const confirmAndPrint = async (periodId: number, warehouseId: number) => {
  const pendingCount = await getPendingCount(periodId, warehouseId);
  
  if (pendingCount === 0) {
    showInfo('No hay marbetes pendientes de impresión');
    return;
  }
  
  const confirm = await showConfirmDialog(
    'Confirmar Impresión',
    `¿Desea imprimir ${pendingCount} marbete(s) pendiente(s)?`
  );
  
  if (confirm) {
    await printAllPendingLabels(periodId, warehouseId);
  }
};
```

### 3.3 Reimpresión de Folios Específicos

```typescript
// Reimprimir folios dañados o perdidos
const reprintSpecificFolios = async (
  periodId: number,
  warehouseId: number,
  folios: number[]
) => {
  if (folios.length === 0) {
    showError('Debe seleccionar al menos un folio');
    return;
  }
  
  const confirm = await showConfirmDialog(
    'Confirmar Reimpresión',
    `¿Desea reimprimir ${folios.length} folio(s)?\nFolios: ${folios.join(', ')}`
  );
  
  if (!confirm) return;
  
  try {
    const response = await fetch('/api/sigmav2/labels/print', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        periodId,
        warehouseId,
        folios,
        forceReprint: true  // ⚠️ OBLIGATORIO para reimpresión
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    // Descargar PDF
    const blob = await response.blob();
    downloadPDF(blob, `reimpresion_${folios.join('_')}.pdf`);
    
    showSuccess('Folios reimpresos correctamente');
    
  } catch (error) {
    showError('Error al reimprimir: ' + error.message);
  }
};
```

---

## 📊 4. CONSULTA Y LISTADO DE MARBETES

### 4.1 API de Resumen con Paginación

```typescript
interface LabelSummaryParams {
  periodId: number;
  warehouseId: number;
  page?: number;
  size?: number;
  searchText?: string;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

// Obtener resumen de marbetes con paginación
const getLabelSummary = async (params: LabelSummaryParams) => {
  const queryParams = new URLSearchParams({
    periodId: String(params.periodId),
    warehouseId: String(params.warehouseId),
    page: String(params.page || 0),
    size: String(params.size || 50),
    ...(params.searchText && { searchText: params.searchText }),
    ...(params.sortBy && { sortBy: params.sortBy }),
    ...(params.sortDirection && { sortDirection: params.sortDirection })
  });
  
  try {
    const response = await fetch(
      `/api/sigmav2/labels/summary?${queryParams}`,
      {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      }
    );
    
    if (!response.ok) {
      throw new Error('Error al obtener resumen de marbetes');
    }
    
    return await response.json();
    
  } catch (error) {
    showError(error.message);
    return [];
  }
};
```

### 4.2 Tabla con Búsqueda y Ordenamiento

```typescript
const LabelSummaryTable = () => {
  const [data, setData] = useState([]);
  const [page, setPage] = useState(0);
  const [searchText, setSearchText] = useState('');
  const [sortBy, setSortBy] = useState('claveProducto');
  const [sortDirection, setSortDirection] = useState<'ASC' | 'DESC'>('ASC');
  
  // Cargar datos
  useEffect(() => {
    loadData();
  }, [page, searchText, sortBy, sortDirection]);
  
  const loadData = async () => {
    const result = await getLabelSummary({
      periodId: selectedPeriod,
      warehouseId: selectedWarehouse,
      page,
      size: 50,
      searchText,
      sortBy,
      sortDirection
    });
    setData(result);
  };
  
  // Manejar cambio de ordenamiento
  const handleSort = (column: string) => {
    if (sortBy === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC');
    } else {
      setSortBy(column);
      setSortDirection('ASC');
    }
  };
  
  return (
    <div>
      {/* Búsqueda */}
      <input
        type="text"
        placeholder="Buscar por producto, almacén, estado..."
        value={searchText}
        onChange={(e) => {
          setSearchText(e.target.value);
          setPage(0); // Reiniciar a página 0
        }}
      />
      
      {/* Tabla */}
      <table>
        <thead>
          <tr>
            <th onClick={() => handleSort('claveProducto')}>
              Clave Producto {sortBy === 'claveProducto' && (sortDirection === 'ASC' ? '↑' : '↓')}
            </th>
            <th onClick={() => handleSort('nombreProducto')}>
              Producto {sortBy === 'nombreProducto' && (sortDirection === 'ASC' ? '↑' : '↓')}
            </th>
            <th onClick={() => handleSort('foliosExistentes')}>
              Folios {sortBy === 'foliosExistentes' && (sortDirection === 'ASC' ? '↑' : '↓')}
            </th>
            <th onClick={() => handleSort('existencias')}>
              Existencias {sortBy === 'existencias' && (sortDirection === 'ASC' ? '↑' : '↓')}
            </th>
            <th onClick={() => handleSort('estado')}>
              Estado {sortBy === 'estado' && (sortDirection === 'ASC' ? '↑' : '↓')}
            </th>
            <th>Impreso</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {data.map(item => (
            <tr key={item.productId}>
              <td>{item.claveProducto}</td>
              <td>{item.nombreProducto}</td>
              <td>
                {item.foliosExistentes > 0 ? (
                  <span>
                    {item.foliosExistentes} 
                    <small>({item.primerFolio} - {item.ultimoFolio})</small>
                  </span>
                ) : (
                  <span className="no-folios">Sin folios</span>
                )}
              </td>
              <td>{item.existencias}</td>
              <td>
                <span className={`badge badge-${item.estado}`}>
                  {item.estado}
                </span>
              </td>
              <td>
                {item.impreso ? (
                  <span className="printed">✓ {item.fechaImpresion}</span>
                ) : (
                  <span className="pending">Pendiente</span>
                )}
              </td>
              <td>
                <button onClick={() => viewDetails(item.productId)}>
                  Ver detalles
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      
      {/* Paginación */}
      <div className="pagination">
        <button 
          onClick={() => setPage(page - 1)} 
          disabled={page === 0}
        >
          Anterior
        </button>
        <span>Página {page + 1}</span>
        <button 
          onClick={() => setPage(page + 1)}
          disabled={data.length < 50}
        >
          Siguiente
        </button>
      </div>
    </div>
  );
};
```

---

## 🔢 5. REGISTRO DE CONTEOS (C1 / C2)

### 5.1 Validaciones Críticas

```typescript
// Validar folio antes de registrar conteo
const validateFolioForCount = async (
  folio: number,
  periodId: number,
  warehouseId: number
) => {
  try {
    // Obtener información del marbete
    const label = await getLabelByFolio(folio);
    
    if (!label) {
      throw new Error(`El folio ${folio} no existe en el sistema`);
    }
    
    // Validar periodo
    if (label.periodId !== periodId) {
      throw new Error(
        `El folio ${folio} pertenece a un periodo diferente. ` +
        `Verifique que está trabajando en el periodo correcto.`
      );
    }
    
    // Validar almacén
    if (label.warehouseId !== warehouseId) {
      throw new Error(
        `El folio ${folio} pertenece a otro almacén. ` +
        `Verifique que está en el almacén correcto.`
      );
    }
    
    // Validar estado
    if (label.estado === 'CANCELADO') {
      throw new Error(
        `No se puede registrar conteo: el folio ${folio} está CANCELADO`
      );
    }
    
    if (label.estado !== 'IMPRESO') {
      throw new Error(
        `No se puede registrar conteo: el folio ${folio} no está IMPRESO. ` +
        `Estado actual: ${label.estado}`
      );
    }
    
    return true;
    
  } catch (error) {
    showError(error.message);
    return false;
  }
};
```

### 5.2 Registro de C1

```typescript
// Registrar primer conteo
const registerC1 = async (
  folio: number,
  countedValue: number,
  periodId: number,
  warehouseId: number
) => {
  // Validar folio
  const isValid = await validateFolioForCount(folio, periodId, warehouseId);
  if (!isValid) return;
  
  try {
    const response = await fetch('/api/sigmav2/labels/count/c1', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        folio,
        countedValue,
        periodId,
        warehouseId
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      
      // Manejar errores específicos
      if (error.message.includes('ya fue registrado')) {
        showError(`El conteo C1 ya existe para el folio ${folio}`);
      } else if (error.message.includes('ya existe un conteo C2')) {
        showError(
          `No se puede registrar C1 porque ya existe C2 para el folio ${folio}. ` +
          `La secuencia de conteo está rota.`
        );
      } else {
        throw new Error(error.message);
      }
      return;
    }
    
    showSuccess(`Conteo C1 registrado correctamente para folio ${folio}`);
    
    // Recargar datos
    await reloadCountData();
    
  } catch (error) {
    showError('Error al registrar C1: ' + error.message);
  }
};
```

### 5.3 Registro de C2

```typescript
// Registrar segundo conteo
const registerC2 = async (
  folio: number,
  countedValue: number,
  periodId: number,
  warehouseId: number
) => {
  // Validar folio
  const isValid = await validateFolioForCount(folio, periodId, warehouseId);
  if (!isValid) return;
  
  try {
    const response = await fetch('/api/sigmav2/labels/count/c2', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        folio,
        countedValue,
        periodId,
        warehouseId
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      
      if (error.message.includes('ya fue registrado')) {
        showError(`El conteo C2 ya existe para el folio ${folio}`);
      } else if (error.message.includes('debe registrar C1 primero')) {
        showError(
          `Debe registrar C1 antes de registrar C2 para el folio ${folio}`
        );
      } else {
        throw new Error(error.message);
      }
      return;
    }
    
    showSuccess(`Conteo C2 registrado correctamente para folio ${folio}`);
    
    // Recargar datos
    await reloadCountData();
    
  } catch (error) {
    showError('Error al registrar C2: ' + error.message);
  }
};
```

### 5.4 Interfaz de Registro de Conteos

```typescript
const CountRegistrationForm = () => {
  const [folio, setFolio] = useState('');
  const [countValue, setCountValue] = useState('');
  const [labelInfo, setLabelInfo] = useState(null);
  
  // Buscar información del folio
  const searchFolio = async () => {
    if (!folio) return;
    
    try {
      const info = await getLabelByFolio(Number(folio));
      setLabelInfo(info);
      
      if (info.estado === 'CANCELADO') {
        showWarning('Este folio está CANCELADO');
      } else if (info.estado !== 'IMPRESO') {
        showWarning('Este folio no está IMPRESO');
      }
      
    } catch (error) {
      showError('Folio no encontrado');
      setLabelInfo(null);
    }
  };
  
  // Registrar conteo
  const submitCount = async (countType: 'C1' | 'C2') => {
    if (!folio || !countValue) {
      showError('Complete todos los campos');
      return;
    }
    
    const registerFn = countType === 'C1' ? registerC1 : registerC2;
    await registerFn(
      Number(folio),
      Number(countValue),
      selectedPeriod,
      selectedWarehouse
    );
    
    // Limpiar formulario
    setFolio('');
    setCountValue('');
    setLabelInfo(null);
  };
  
  return (
    <div className="count-form">
      <div className="form-group">
        <label>Folio</label>
        <input
          type="number"
          value={folio}
          onChange={(e) => setFolio(e.target.value)}
          onBlur={searchFolio}
          placeholder="Ingrese el folio"
        />
      </div>
      
      {labelInfo && (
        <div className="label-info">
          <p><strong>Producto:</strong> {labelInfo.productName}</p>
          <p><strong>Estado:</strong> 
            <span className={`badge badge-${labelInfo.estado}`}>
              {labelInfo.estado}
            </span>
          </p>
          {labelInfo.c1Value !== null && (
            <p><strong>C1:</strong> {labelInfo.c1Value}</p>
          )}
          {labelInfo.c2Value !== null && (
            <p><strong>C2:</strong> {labelInfo.c2Value}</p>
          )}
        </div>
      )}
      
      <div className="form-group">
        <label>Cantidad Contada</label>
        <input
          type="number"
          value={countValue}
          onChange={(e) => setCountValue(e.target.value)}
          placeholder="Ingrese la cantidad"
          min="0"
          step="0.01"
        />
      </div>
      
      <div className="button-group">
        <button 
          onClick={() => submitCount('C1')}
          disabled={!labelInfo || labelInfo.estado !== 'IMPRESO'}
        >
          Registrar C1
        </button>
        <button 
          onClick={() => submitCount('C2')}
          disabled={!labelInfo || labelInfo.estado !== 'IMPRESO'}
        >
          Registrar C2
        </button>
      </div>
    </div>
  );
};
```

---

## ❌ 6. CANCELACIÓN DE MARBETES

### 6.1 Validaciones Importantes

```typescript
// Validar si se puede cancelar un marbete
const canCancelLabel = (label: any): { canCancel: boolean; reason?: string } => {
  // No se puede cancelar si ya está cancelado
  if (label.estado === 'CANCELADO') {
    return {
      canCancel: false,
      reason: 'El marbete ya está cancelado'
    };
  }
  
  // No se puede cancelar si tiene conteos
  if (label.c1Value !== null || label.c2Value !== null) {
    return {
      canCancel: false,
      reason: 'No se puede cancelar un marbete que tiene conteos registrados'
    };
  }
  
  // Si está generado pero no impreso, se puede cancelar
  if (label.estado === 'GENERADO' || label.estado === 'IMPRESO') {
    return { canCancel: true };
  }
  
  return {
    canCancel: false,
    reason: 'Estado inválido para cancelación'
  };
};
```

### 6.2 Cancelación de Marbete

```typescript
// Cancelar marbete
const cancelLabel = async (
  folio: number,
  reason: string,
  periodId: number,
  warehouseId: number
) => {
  // Obtener información del marbete
  const label = await getLabelByFolio(folio);
  
  if (!label) {
    showError(`Folio ${folio} no encontrado`);
    return;
  }
  
  // Validar si se puede cancelar
  const validation = canCancelLabel(label);
  if (!validation.canCancel) {
    showError(validation.reason!);
    return;
  }
  
  // Confirmar cancelación
  const confirm = await showConfirmDialog(
    'Confirmar Cancelación',
    `¿Está seguro de que desea cancelar el folio ${folio}?\n\n` +
    `Producto: ${label.productName}\n` +
    `Motivo: ${reason}\n\n` +
    `Esta acción no se puede deshacer.`
  );
  
  if (!confirm) return;
  
  try {
    const response = await fetch('/api/sigmav2/labels/cancel', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        folio,
        reason,
        periodId,
        warehouseId
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    showSuccess(`Folio ${folio} cancelado correctamente`);
    
    // Recargar datos
    await reloadLabelData();
    
  } catch (error) {
    showError('Error al cancelar: ' + error.message);
  }
};
```

---

## 🎨 7. MEJORES PRÁCTICAS DE UX/UI

### 7.1 Feedback Visual

```typescript
// Indicadores de estado visuales
const LabelStatusBadge = ({ estado }: { estado: string }) => {
  const statusConfig = {
    'GENERADO': { color: 'blue', icon: '📝', text: 'Generado' },
    'IMPRESO': { color: 'green', icon: '🖨️', text: 'Impreso' },
    'CANCELADO': { color: 'red', icon: '❌', text: 'Cancelado' }
  };
  
  const config = statusConfig[estado] || { color: 'gray', icon: '❓', text: estado };
  
  return (
    <span className={`badge badge-${config.color}`}>
      {config.icon} {config.text}
    </span>
  );
};
```

### 7.2 Manejo de Errores Amigable

```typescript
// Mostrar errores de forma amigable
const showUserFriendlyError = (error: any) => {
  const errorMap: Record<string, string> = {
    'LabelNotFoundException': 'El folio no fue encontrado en el sistema',
    'InvalidLabelStateException': 'El marbete no está en el estado correcto para esta operación',
    'PermissionDeniedException': 'No tiene permisos para realizar esta acción',
    'DuplicateCountException': 'Este conteo ya fue registrado anteriormente',
    'CountSequenceException': 'Error en la secuencia de conteos. Verifique el orden.',
    'LabelAlreadyCancelledException': 'Este marbete ya está cancelado',
    'PeriodClosedException': 'No se pueden realizar cambios: el periodo está cerrado',
    'CatalogNotLoadedException': 'Debe importar los catálogos antes de continuar'
  };
  
  const errorType = error.error || error.type || 'Error';
  const friendlyMessage = errorMap[errorType] || error.message;
  
  // Mostrar toast o modal según la gravedad
  if (errorType.includes('Permission') || errorType.includes('Closed')) {
    showModal('Error', friendlyMessage, 'error');
  } else {
    showToast(friendlyMessage, 'error');
  }
};
```

### 7.3 Confirmaciones Críticas

```typescript
// Diálogo de confirmación reutilizable
const showConfirmDialog = (
  title: string,
  message: string,
  type: 'info' | 'warning' | 'danger' = 'info'
): Promise<boolean> => {
  return new Promise((resolve) => {
    // Implementar modal de confirmación
    const modal = createModal({
      title,
      message,
      type,
      buttons: [
        {
          text: 'Cancelar',
          style: 'secondary',
          onClick: () => {
            modal.close();
            resolve(false);
          }
        },
        {
          text: 'Confirmar',
          style: type === 'danger' ? 'danger' : 'primary',
          onClick: () => {
            modal.close();
            resolve(true);
          }
        }
      ]
    });
    
    modal.show();
  });
};
```

---

## 🔐 8. SEGURIDAD Y PERMISOS

### 8.1 Control de Acceso por Rol

```typescript
// Verificar permisos según el rol
const checkPermission = (action: string, userRole: string): boolean => {
  const permissions: Record<string, string[]> = {
    'import_multiwarehouse': ['ADMINISTRADOR'],
    'generate_labels': ['ADMINISTRADOR', 'ALMACENISTA', 'AUXILIAR'],
    'print_labels': ['ADMINISTRADOR', 'ALMACENISTA', 'AUXILIAR'],
    'register_c1': ['ADMINISTRADOR', 'ALMACENISTA', 'AUXILIAR', 'AUXILIAR_DE_CONTEO'],
    'register_c2': ['ADMINISTRADOR', 'ALMACENISTA', 'AUXILIAR', 'AUXILIAR_DE_CONTEO'],
    'cancel_label': ['ADMINISTRADOR', 'ALMACENISTA'],
    'view_all_warehouses': ['ADMINISTRADOR', 'AUXILIAR']
  };
  
  const allowedRoles = permissions[action] || [];
  return allowedRoles.includes(userRole.toUpperCase());
};

// Ocultar/deshabilitar elementos según permisos
const SecureButton = ({ 
  action, 
  userRole, 
  children, 
  ...props 
}: any) => {
  const hasPermission = checkPermission(action, userRole);
  
  if (!hasPermission) {
    return (
      <button 
        {...props} 
        disabled 
        title="No tiene permisos para esta acción"
      >
        {children} 🔒
      </button>
    );
  }
  
  return <button {...props}>{children}</button>;
};
```

### 8.2 Validaci��n de Token

```typescript
// Interceptor para renovar token automáticamente
const fetchWithAuth = async (url: string, options: RequestInit = {}) => {
  // Agregar token
  const headers = {
    ...options.headers,
    'Authorization': `Bearer ${getToken()}`
  };
  
  const response = await fetch(url, {
    ...options,
    headers
  });
  
  // Si el token expiró, intentar renovar
  if (response.status === 401) {
    const refreshed = await refreshToken();
    
    if (refreshed) {
      // Reintentar con nuevo token
      return fetch(url, {
        ...options,
        headers: {
          ...options.headers,
          'Authorization': `Bearer ${getToken()}`
        }
      });
    } else {
      // Redirigir al login
      redirectToLogin();
    }
  }
  
  return response;
};
```

---

## 📱 9. RESPONSIVE DESIGN

### 9.1 Adaptación Móvil

```typescript
// Componente adaptativo para móvil
const ResponsiveLabelTable = ({ data }: { data: any[] }) => {
  const isMobile = useMediaQuery('(max-width: 768px)');
  
  if (isMobile) {
    // Vista de cards para móvil
    return (
      <div className="label-cards">
        {data.map(item => (
          <div key={item.productId} className="label-card">
            <div className="card-header">
              <h3>{item.nombreProducto}</h3>
              <LabelStatusBadge estado={item.estado} />
            </div>
            <div className="card-body">
              <div className="card-row">
                <span className="label">Clave:</span>
                <span className="value">{item.claveProducto}</span>
              </div>
              <div className="card-row">
                <span className="label">Folios:</span>
                <span className="value">{item.foliosExistentes}</span>
              </div>
              <div className="card-row">
                <span className="label">Existencias:</span>
                <span className="value">{item.existencias}</span>
              </div>
            </div>
            <div className="card-footer">
              <button onClick={() => viewDetails(item.productId)}>
                Ver detalles
              </button>
            </div>
          </div>
        ))}
      </div>
    );
  }
  
  // Vista de tabla para desktop
  return (
    <table className="label-table">
      {/* Tabla normal */}
    </table>
  );
};
```

---

## 🧪 10. TESTING

### 10.1 Tests Unitarios

```typescript
// Test de validación de folios
describe('Validación de Folios', () => {
  test('Debe validar folio correcto', async () => {
    const isValid = await validateFolioForCount(1001, 16, 369);
    expect(isValid).toBe(true);
  });
  
  test('Debe rechazar folio cancelado', async () => {
    const label = { estado: 'CANCELADO' };
    const validation = canCancelLabel(label);
    expect(validation.canCancel).toBe(false);
    expect(validation.reason).toContain('ya está cancelado');
  });
  
  test('Debe rechazar folio con conteos', async () => {
    const label = { estado: 'IMPRESO', c1Value: 10, c2Value: null };
    const validation = canCancelLabel(label);
    expect(validation.canCancel).toBe(false);
    expect(validation.reason).toContain('tiene conteos');
  });
});
```

### 10.2 Tests de Integración

```typescript
// Test de flujo completo
describe('Flujo de Generación e Impresión', () => {
  test('Debe generar e imprimir marbetes', async () => {
    // 1. Generar marbetes
    const generateResult = await generateLabels({
      periodId: 16,
      warehouseId: 369,
      products: [{ productId: 123, quantity: 10 }]
    });
    
    expect(generateResult.totalGenerated).toBe(10);
    
    // 2. Verificar pendientes
    const pendingCount = await getPendingCount(16, 369);
    expect(pendingCount).toBeGreaterThanOrEqual(10);
    
    // 3. Imprimir
    const printResult = await printAllPendingLabels(16, 369);
    expect(printResult).not.toBeNull();
    
    // 4. Verificar que no haya pendientes
    const pendingAfter = await getPendingCount(16, 369);
    expect(pendingAfter).toBeLessThan(pendingCount);
  });
});
```

---

## 📚 11. DOCUMENTACIÓN PARA EL USUARIO

### 11.1 Tooltips Contextuales

```typescript
// Tooltips informativos
const TooltipHelp = ({ text }: { text: string }) => (
  <span className="tooltip-icon" title={text}>
    ❓
  </span>
);

// Uso
<label>
  Cantidad de Marbetes
  <TooltipHelp text="Ingrese la cantidad de marbetes a generar. Se recomienda generar la misma cantidad que las existencias del producto." />
</label>
```

### 11.2 Mensajes de Ayuda Inline

```typescript
// Mensajes de ayuda contextuales
const InlineHelp = () => (
  <div className="inline-help">
    <p className="help-text">
      💡 <strong>Tip:</strong> Los marbetes se numeran automáticamente en forma consecutiva. 
      No es necesario especificar los números de folio.
    </p>
  </div>
);
```

---

## 🔄 12. SINCRONIZACIÓN Y ACTUALIZACIÓN

### 12.1 Polling para Datos en Tiempo Real

```typescript
// Actualizar datos automáticamente
const useAutoRefresh = (fetchFn: () => Promise<any>, intervalMs: number = 30000) => {
  useEffect(() => {
    fetchFn();
    
    const interval = setInterval(() => {
      fetchFn();
    }, intervalMs);
    
    return () => clearInterval(interval);
  }, [fetchFn, intervalMs]);
};

// Uso
const Dashboard = () => {
  const [pendingCount, setPendingCount] = useState(0);
  
  const fetchPendingCount = useCallback(async () => {
    const count = await getPendingCount(periodId, warehouseId);
    setPendingCount(count);
  }, [periodId, warehouseId]);
  
  // Actualizar cada 30 segundos
  useAutoRefresh(fetchPendingCount, 30000);
  
  return (
    <div>
      <h2>Marbetes Pendientes: {pendingCount}</h2>
    </div>
  );
};
```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

### Frontend - Importación
- [ ] Validar extensión y tamaño de archivos
- [ ] Mostrar plantilla de ejemplo descargable
- [ ] Implementar barra de progreso durante carga
- [ ] Manejar errores específicos (periodo cerrado, formato inválido, etc.)
- [ ] Mostrar resumen de importación (registros creados/actualizados)

### Frontend - Generación de Marbetes
- [ ] Usar API `generateBatchList()` (no las deprecadas)
- [ ] Validar existencias antes de generar
- [ ] Permitir selección múltiple de productos
- [ ] Mostrar resumen de generación (folios asignados)
- [ ] Confirmar operación antes de ejecutar

### Frontend - Impresión
- [ ] Implementar impresión automática (sin folios)
- [ ] Consultar cantidad de pendientes antes de imprimir
- [ ] Permitir reimpresión con `forceReprint: true`
- [ ] Descargar PDF automáticamente
- [ ] Mostrar confirmación de impresión exitosa

### Frontend - Conteos
- [ ] Validar folio antes de registrar conteo
- [ ] Mostrar información del producto al ingresar folio
- [ ] Prevenir registro de C2 sin C1
- [ ] Prevenir duplicados de conteos
- [ ] Manejar errores de secuencia

### Frontend - Cancelación
- [ ] Validar que el marbete no tenga conteos
- [ ] Solicitar motivo de cancelación
- [ ] Confirmar acción crítica
- [ ] Actualizar vista después de cancelar

### Frontend - UX General
- [ ] Implementar feedback visual (loading, success, error)
- [ ] Agregar tooltips y ayuda contextual
- [ ] Diseño responsive para móviles
- [ ] Control de permisos por rol
- [ ] Actualización automática de datos

---

## 📞 SOPORTE

Para dudas o problemas, consulte la documentación adicional:

- `/docs/GUIA-FRONTEND-NUEVA-API-IMPRESION.md`
- `/docs/EJEMPLOS-USO-API-IMPRESION.md`
- `/docs/FORMATO-EXCEL-MULTIALMACEN.md`
- `/docs/GUIA-COMPLETA-APIS-MARBETES.md`

---

**Última actualización:** 2026-01-22
**Versión del documento:** 1.0
