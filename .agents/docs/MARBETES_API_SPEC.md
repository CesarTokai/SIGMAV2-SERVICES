# 📡 Especificación de API para Sistema de Marbetes

## Estructura de Respuestas Paginadas

Todas las respuestas de listados deben seguir este formato:

```json
{
  "content": [...],           // Array de datos
  "totalPages": 10,           // Total de páginas
  "totalElements": 245,       // Total de registros
  "size": 100,                // Tamaño de página
  "number": 0                 // Página actual (0-indexed)
}
```

---

## 1. Submódulo: Consulta y Captura

### 1.1 Listar Marbetes

**Endpoint**: `POST /marbetes/list`

**Request Body**:
```json
{
  "periodId": 1,
  "warehouseId": 3,
  "search": "producto A",      // Opcional
  "page": 0,
  "pageSize": 100
}
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "requestedFolios": 50,
      "existingFolios": 45,
      "productCode": "PROD-001",
      "productName": "Producto A",
      "warehouseKey": "ALM-01",
      "warehouseName": "Almacén Principal",
      "status": "Activo",
      "stock": 150
    }
  ],
  "totalPages": 5,
  "totalElements": 450,
  "size": 100,
  "number": 0
}
```

**Mapeo en Frontend**:
- `requestedFolios` → `foliosSolicitados`
- `existingFolios` → `foliosExistentes`
- `productCode` → `claveProducto`
- `productName` → `producto`
- `warehouseKey` → `claveAlmacen`
- `warehouseName` → `almacen`
- `status` → `estado`
- `stock` → `existencias`

---

### 1.2 Generar Marbetes

**Endpoint**: `POST /marbetes/generate`

**Request Body**:
```json
{
  "periodId": 1,
  "warehouseId": 3
}
```

**Response** (200 OK):
```json
{
  "message": "Marbetes generados exitosamente",
  "generated": 45,
  "period": "2025-01",
  "warehouse": "Almacén Principal"
}
```

**Errores Posibles**:
- 400 Bad Request: "Ya existen marbetes generados para este período y almacén"
- 404 Not Found: "Período o almacén no encontrado"
- 500 Internal Server Error: Error en la generación

---

## 2. Submódulo: Impresión de Marbetes

### 2.1 Listar Marbetes Generados

**Endpoint**: `POST /marbetes/generated`

**Request Body**:
```json
{
  "periodId": 1,
  "warehouseId": 3,
  "search": "producto A",      // Opcional
  "page": 0,
  "pageSize": 100
}
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 123,
      "folio": 1001,
      "productCode": "PROD-001",
      "productName": "Producto A",
      "warehouseKey": "ALM-01",
      "warehouseName": "Almacén Principal",
      "stock": 150,
      "printed": true,
      "printedDate": "2025-11-27T10:30:00"
    },
    {
      "id": 124,
      "folio": 1002,
      "productCode": "PROD-002",
      "productName": "Producto B",
      "warehouseKey": "ALM-01",
      "warehouseName": "Almacén Principal",
      "stock": 75,
      "printed": false,
      "printedDate": null
    }
  ],
  "totalPages": 3,
  "totalElements": 245,
  "size": 100,
  "number": 0
}
```

**Campos Importantes**:
- `printed`: boolean - indica si ya fue impreso
- `printedDate`: string | null - fecha y hora de impresión

---

### 2.2 Imprimir Marbetes

**Endpoint**: `POST /marbetes/print`

**Request Body**:
```json
{
  "periodId": 1,
  "warehouseId": 3,
  "folios": [1001, 1002, 1003, 1004, 1005]
}
```

**Opciones de Respuesta**:

#### Opción A: Devolver URL del PDF
```json
{
  "message": "Marbetes impresos correctamente",
  "pdfUrl": "https://api.ejemplo.com/files/marbetes-1234.pdf",
  "printed": 5
}
```

#### Opción B: Devolver el PDF directamente
- Content-Type: `application/pdf`
- Body: Binary PDF data
- Headers:
  - `Content-Disposition: attachment; filename="marbetes-1001-1005.pdf"`

**Comportamiento del Frontend**:
- Si recibe `pdfUrl`: Abre en nueva pestaña con `window.open()`
- Si recibe Blob: Crea objeto URL y abre en nueva pestaña

**Errores Posibles**:
- 400 Bad Request: "Algunos folios no existen o no pertenecen al período/almacén"
- 404 Not Found: "No se encontraron marbetes para imprimir"

---

## 3. Submódulo: Conteo de Marbetes

### 3.1 Listar Marbetes para Conteo

**Endpoint**: `POST /labels/for-count`

**Request Body**:
```json
{
  "periodId": 1,
  "warehouseId": 3,
  "search": "producto A",      // Opcional
  "page": 0,
  "pageSize": 50
}
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 123,
      "folio": 1001,
      "productCode": "PROD-001",
      "productName": "Producto A",
      "warehouseKey": "ALM-01",
      "warehouseName": "Almacén Principal",
      "expectedStock": 150,
      "count1": 148,
      "count2": 150,
      "difference": 2,
      "status": "Completo",
      "cancelled": false
    },
    {
      "id": 124,
      "folio": 1002,
      "productCode": "PROD-002",
      "productName": "Producto B",
      "warehouseKey": "ALM-01",
      "warehouseName": "Almacén Principal",
      "expectedStock": 75,
      "count1": null,
      "count2": null,
      "difference": null,
      "status": "Pendiente",
      "cancelled": false
    },
    {
      "id": 125,
      "folio": 1003,
      "productCode": "PROD-003",
      "productName": "Producto C",
      "warehouseKey": "ALM-01",
      "warehouseName": "Almacén Principal",
      "expectedStock": 200,
      "count1": 200,
      "count2": null,
      "difference": null,
      "status": "Parcial",
      "cancelled": false
    },
    {
      "id": 126,
      "folio": 1004,
      "productCode": "PROD-004",
      "productName": "Producto D (Cancelado)",
      "warehouseKey": "ALM-01",
      "warehouseName": "Almacén Principal",
      "expectedStock": 50,
      "count1": null,
      "count2": null,
      "difference": null,
      "status": "Cancelado",
      "cancelled": true
    }
  ],
  "totalPages": 2,
  "totalElements": 100,
  "size": 50,
  "number": 0
}
```

**Estados Posibles**:
- `"Pendiente"`: Sin conteos
- `"Parcial"`: Solo un conteo capturado
- `"Completo"`: Ambos conteos capturados
- `"Cancelado"`: Marbete cancelado

**Campos Importantes**:
- `count1`, `count2`: pueden ser `null` si no se han capturado
- `difference`: diferencia absoluta entre conteos, `null` si no ambos están capturados
- `cancelled`: boolean, indica si el marbete fue cancelado

---

### 3.2 Guardar Conteo

**Endpoint**: `POST /marbetes/save-count`

**Request Body**:
```json
{
  "marbeteId": 124,
  "folio": 1002,
  "count1": 74,
  "count2": 75,
  "difference": 1
}
```

**Response** (200 OK):
```json
{
  "message": "Conteo guardado correctamente",
  "marbeteId": 124,
  "folio": 1002,
  "count1": 74,
  "count2": 75,
  "difference": 1,
  "status": "Completo"
}
```

**Validaciones en Backend**:
- Verificar que el marbete no esté cancelado
- Calcular y almacenar la diferencia: `|count1 - count2|`
- Actualizar el estado del marbete
- Registrar fecha/hora de captura

**Errores Posibles**:
- 400 Bad Request: "El marbete está cancelado, no se puede capturar conteo"
- 404 Not Found: "Marbete no encontrado"

---

### 3.3 Cancelar Marbete

**Endpoint**: `POST /marbetes/cancel`

**Request Body**:
```json
{
  "marbeteId": 126,
  "folio": 1004
}
```

**Response** (200 OK):
```json
{
  "message": "Marbete cancelado correctamente",
  "marbeteId": 126,
  "folio": 1004,
  "cancelledAt": "2025-11-27T11:45:00"
}
```

**Validaciones en Backend**:
- Marcar el marbete como cancelado
- Registrar fecha/hora de cancelación
- No permitir captura de conteo después de cancelar
- **Acción irreversible**: No se puede "descancelar"

**Errores Posibles**:
- 400 Bad Request: "El marbete ya está cancelado"
- 404 Not Found: "Marbete no encontrado"

---

## 4. Endpoints Auxiliares (Compartidos)

### 4.1 Listar Períodos

**Endpoint**: `GET /periods?page=0&size=100`

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "date": "2025-01-31T00:00:00",
      "comments": "Inventario Enero 2025",
      "state": "Activo"
    },
    {
      "id": 2,
      "date": "2024-12-31T00:00:00",
      "comments": "Inventario Diciembre 2024",
      "state": "Cerrado"
    }
  ],
  "totalPages": 1,
  "totalElements": 2,
  "size": 100,
  "number": 0
}
```

---

### 4.2 Listar Almacenes

**Endpoint**: `GET /warehouses?page=0&size=100`

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "clave": "ALM-01",
      "nombre": "Almacén Principal",
      "activo": true
    },
    {
      "id": 2,
      "clave": "ALM-02",
      "nombre": "Almacén Secundario",
      "activo": true
    },
    {
      "id": 3,
      "clave": "ALM-03",
      "nombre": "Almacén Norte",
      "activo": false
    }
  ],
  "totalPages": 1,
  "totalElements": 3,
  "size": 100,
  "number": 0
}
```

**Consideraciones**:
- Para usuarios con rol "ALMACENISTA", el backend debe filtrar y devolver solo el almacén asignado a ese usuario
- Los demás roles reciben todos los almacenes activos

---

## 5. Códigos de Estado HTTP

### Exitosos:
- **200 OK**: Operación exitosa
- **201 Created**: Recurso creado (si aplica)

### Errores del Cliente:
- **400 Bad Request**: Datos inválidos, validaciones fallidas
- **401 Unauthorized**: No autenticado
- **403 Forbidden**: No tiene permisos para la operación
- **404 Not Found**: Recurso no encontrado

### Errores del Servidor:
- **500 Internal Server Error**: Error inesperado del servidor

---

## 6. Formato de Fechas

Todas las fechas deben seguir el formato **ISO 8601**:
- `2025-11-27T10:30:00` (sin zona horaria)
- `2025-11-27T10:30:00Z` (UTC)
- `2025-11-27T10:30:00-06:00` (con offset)

El frontend las formateará según la configuración regional (es-MX).

---

## 7. Manejo de Errores

Formato estándar de error:

```json
{
  "message": "Descripción del error",
  "code": "ERROR_CODE",
  "details": {
    "field": "periodId",
    "reason": "Período no encontrado"
  }
}
```

El frontend mostrará el mensaje al usuario mediante SweetAlert2.

---

## 8. Consideraciones de Seguridad

1. **Autenticación**: Todas las rutas requieren token JWT válido
2. **Autorización por Rol**: Validar permisos según rol del usuario
3. **Contexto de Usuario**: 
   - Almacenistas solo acceden a su almacén asignado
   - Validar que el usuario tenga acceso al período y almacén solicitados
4. **Validación de Datos**: Sanitizar y validar todos los inputs
5. **Logging**: Registrar todas las operaciones de impresión y conteo

---

## 9. Consideraciones de Rendimiento

1. **Paginación**: Implementar paginación en todas las consultas de listados
2. **Índices en BD**: Crear índices en campos de búsqueda frecuente
3. **Caché**: Considerar caché para listados de períodos y almacenes
4. **PDF Asíncrono**: Para grandes volúmenes, considerar generación asíncrona de PDFs

---

## 10. Pruebas Recomendadas

### Casos de Prueba:

1. **Consulta y Captura**:
   - Generar marbetes por primera vez
   - Intentar regenerar marbetes (debe fallar)
   - Buscar marbetes con diferentes filtros

2. **Impresión**:
   - Imprimir rango completo (normal)
   - Reimprimir folios específicos (extraordinaria)
   - Intentar imprimir folios inexistentes (debe fallar)
   - Verificar que actualiza estado "impreso"

3. **Conteo**:
   - Capturar conteo completo (count1 + count2)
   - Guardar solo count1, luego count2 (actualización)
   - Cancelar marbete no utilizado
   - Intentar capturar conteo en marbete cancelado (debe fallar)
   - Verificar cálculo correcto de diferencia

---

**Nota**: Este documento es una guía completa para el desarrollo del backend. Todos los nombres de campos son sugerencias; el frontend está preparado para mapear diferentes nombres mediante el código de transformación en cada componente.

