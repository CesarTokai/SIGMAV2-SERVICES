# APIs Consumidas - Módulo Almacenista

## 📡 Endpoints Implementados

Este documento detalla todos los endpoints de API consumidos por el módulo de **Gestión de Marbetes para Almacenista**.

---

## 🔹 1. Consulta y Captura (ConsultaCaptura.vue)

### Cargar Períodos
```typescript
GET /periods?page=0&size=100
```
**Descripción:** Obtiene la lista de períodos disponibles para selección.

**Respuesta:**
```json
{
  "content": [
    {
      "id": 1,
      "date": "2024-01-01",
      "comments": "Período Enero 2024",
      "state": "ACTIVO"
    }
  ]
}
```

---

### Cargar Almacenes
```typescript
GET /warehouses?page=0&size=100&sortBy=warehouseKey&sortDir=asc&search=false
```
**Descripción:** Obtiene la lista de almacenes asignados al usuario.

**Respuesta:**
```json
{
  "data": [
    {
      "id": 1,
      "warehouseKey": "A01",
      "nameWarehouse": "Almacén Central",
      "deleted": false
    }
  ]
}
```

---

### Cargar Marbetes (Consulta de Productos)
```typescript
POST /labels/list
```
**Body:**
```json
{
  "periodId": 1,
  "warehouseId": 1,
  "page": 0,
  "size": 100,
  "sortBy": "claveProducto",
  "sortDir": "ASC",
  "search": "",
  "folioSearch": "",
  "productSearch": ""
}
```

**Respuesta:**
```json
{
  "content": [
    {
      "productId": 123,
      "claveProducto": "P001",
      "producto": "Producto A",
      "claveAlmacen": "A01",
      "nombreAlmacen": "Almacén Central",
      "existencias": 100,
      "foliosSolicitados": 0,
      "foliosExistentes": 0,
      "estado": "PENDIENTE"
    }
  ],
  "totalPages": 1,
  "totalElements": 50
}
```

---

### Solicitar Folios
```typescript
POST /labels/request
```
**Body:**
```json
{
  "productId": 123,
  "warehouseId": 1,
  "periodId": 1,
  "labelsToGenerate": 5
}
```

**Respuesta:**
```json
{
  "message": "Solicitud guardada exitosamente",
  "productId": 123,
  "labelsRequested": 5
}
```

---

### Generar Marbetes (Batch)
```typescript
POST /labels/generate/batch
```
**Body:**
```json
{
  "warehouseId": 1,
  "periodId": 1,
  "products": [
    {
      "productId": 123,
      "labelsToGenerate": 5
    },
    {
      "productId": 456,
      "labelsToGenerate": 3
    }
  ]
}
```

**Respuesta:**
```json
{
  "totalGenerated": 8,
  "generadosConExistencias": 5,
  "generadosSinExistencias": 3,
  "primerFolio": 1001,
  "ultimoFolio": 1008,
  "mensaje": "Marbetes generados exitosamente"
}
```

---

### Eliminar Marbete
```typescript
DELETE /labels/{id}
```
**Parámetros:** ID del marbete a eliminar

**Respuesta:**
```json
{
  "message": "Marbete eliminado exitosamente"
}
```

---

## 🔹 2. Impresión de Marbetes (ImpresionMarbetes.vue)

### Buscar Marbetes por Folio/Rango
```typescript
POST /labels/summary
```
**Body (Folio Individual):**
```json
{
  "periodId": 1,
  "warehouseId": 1,
  "folioInicio": 1001
}
```

**Body (Rango de Folios):**
```json
{
  "periodId": 1,
  "warehouseId": 1,
  "folioInicio": 1001,
  "folioFin": 1010
}
```

**Respuesta:**
```json
{
  "content": [
    {
      "folio": 1001,
      "claveProducto": "P001",
      "nombreProducto": "Producto A",
      "claveAlmacen": "A01",
      "nombreAlmacen": "Almacén Central",
      "existencias": 100,
      "estado": "IMPRESO",
      "c1": null,
      "c2": null
    }
  ]
}
```

---

### Contar Marbetes Pendientes de Imprimir
```typescript
POST /labels/pending-print-count
```
**Body:**
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta:**
```json
{
  "count": 25,
  "message": "Hay 25 marbetes pendientes de imprimir"
}
```

---

### Imprimir Marbetes
```typescript
POST /labels/print
```
**Body:**
```json
{
  "periodId": 1,
  "warehouseId": 1,
  "folios": [1001, 1002, 1003]
}
```

**Respuesta:**
```json
{
  "message": "Marbetes marcados como impresos",
  "totalImpreso": 3,
  "folios": [1001, 1002, 1003]
}
```

---

## 🔹 3. Conteo de Marbetes (ConteoMarbetes.vue)

### Buscar Marbete por Folio para Conteo
```typescript
POST /labels/for-count
```
**Body:**
```json
{
  "folio": 1001,
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta:**
```json
{
  "id": 1,
  "folio": 1001,
  "claveProducto": "P001",
  "descripcionProducto": "Producto A",
  "claveAlmacen": "A01",
  "nombreAlmacen": "Almacén Central",
  "existenciasEsperadas": 100,
  "conteo1": null,
  "conteo2": null,
  "diferencia": null,
  "estado": "IMPRESO",
  "cancelado": false
}
```

---

### Registrar Primer Conteo (C1)
```typescript
POST /labels/counts/c1
```
**Body:**
```json
{
  "folio": 1001,
  "countedValue": 98
}
```

**Respuesta:**
```json
{
  "message": "Primer conteo registrado exitosamente",
  "folio": 1001,
  "c1Value": 98
}
```

---

### Registrar Segundo Conteo (C2)
```typescript
POST /labels/counts/c2
```
**Body:**
```json
{
  "folio": 1001,
  "countedValue": 97
}
```

**Respuesta:**
```json
{
  "message": "Segundo conteo registrado exitosamente",
  "folio": 1001,
  "c1Value": 98,
  "c2Value": 97,
  "diferencia": -1
}
```

---

### Actualizar Segundo Conteo (C2)
```typescript
PUT /labels/counts/c2
```
**Body:**
```json
{
  "folio": 1001,
  "countedValue": 98
}
```

**Respuesta:**
```json
{
  "message": "Segundo conteo actualizado exitosamente",
  "folio": 1001,
  "c2Value": 98,
  "diferencia": 0
}
```

---

## 🔹 4. Cancelación de Marbetes (CancelacionMarbetes.vue)

### Listar Marbetes Cancelados
```typescript
POST /labels/reports/cancelled
```
**Body:**
```json
{
  "periodId": 1,
  "warehouseId": 1,
  "page": 0,
  "size": 100,
  "sortBy": "folio",
  "sortDir": "ASC"
}
```

**Respuesta:**
```json
{
  "content": [
    {
      "folio": 1005,
      "claveProducto": "P002",
      "producto": "Producto B",
      "claveAlmacen": "A01",
      "almacen": "Almacén Central",
      "existencias": 50,
      "estado": "CANCELADO",
      "canceladoAt": "2024-01-15T10:30:00",
      "canceladoPor": "usuario@example.com",
      "motivo": "Error en captura"
    }
  ],
  "totalPages": 1,
  "totalElements": 10
}
```

---

### Cancelar Marbete
```typescript
POST /labels/cancel
```
**Body:**
```json
{
  "folio": 1001,
  "periodId": 1,
  "warehouseId": 1,
  "motivo": "Error en generación"
}
```

**Respuesta:**
```json
{
  "message": "Marbete cancelado exitosamente",
  "folio": 1001,
  "estado": "CANCELADO"
}
```

---

## 🔹 5. Registro de Conteos (RegistroConteos.vue)

Este componente usa el componente reutilizable `@/components/RegistroConteos.vue` que ya tiene implementados los siguientes endpoints:

### Buscar Folio
```typescript
GET /labels/folio/{folio}?periodId={periodId}&warehouseId={warehouseId}
```

### Registrar C1
```typescript
POST /labels/count/c1
```
**Body:**
```json
{
  "folio": 1001,
  "countedValue": 98,
  "periodId": 1,
  "warehouseId": 1
}
```

### Registrar C2
```typescript
POST /api/sigmav2/labels/counts/c2
```
**Body:**
```json
{
  "folio": 1001,
  "countedValue": 97,
  "periodId": 1,
  "warehouseId": 1
}
```

---

## 📊 Resumen de Endpoints por Funcionalidad

### Consulta y Captura (6 endpoints)
- ✅ GET `/periods` - Cargar períodos
- ✅ GET `/warehouses` - Cargar almacenes
- ✅ POST `/labels/list` - Listar productos/marbetes
- ✅ POST `/labels/request` - Solicitar folios
- ✅ POST `/labels/generate/batch` - Generar marbetes
- ✅ DELETE `/labels/{id}` - Eliminar marbete

### Impresión (4 endpoints)
- ✅ GET `/periods` - Cargar períodos
- ✅ GET `/warehouses` - Cargar almacenes
- ✅ POST `/labels/summary` - Buscar marbetes
- ✅ POST `/labels/pending-print-count` - Contar pendientes
- ✅ POST `/labels/print` - Imprimir marbetes

### Conteo (6 endpoints)
- ✅ GET `/periods` - Cargar períodos
- ✅ GET `/warehouses` - Cargar almacenes
- ✅ POST `/labels/for-count` - Buscar folio para conteo
- ✅ POST `/labels/counts/c1` - Registrar C1
- ✅ POST `/labels/counts/c2` - Registrar C2
- ✅ PUT `/labels/counts/c2` - Actualizar C2

### Cancelación (4 endpoints)
- ✅ GET `/periods` - Cargar períodos
- ✅ GET `/warehouses` - Cargar almacenes
- ✅ POST `/labels/reports/cancelled` - Listar cancelados
- ✅ POST `/labels/cancel` - Cancelar marbete

---

## 🔒 Validaciones de Seguridad Implementadas

### Frontend (Todas las pantallas)
- ✅ Validación de período seleccionado
- ✅ Validación de almacén seleccionado
- ✅ Validación de campos numéricos (no negativos)
- ✅ Validación de rangos de folios
- ✅ Confirmación antes de acciones destructivas
- ✅ Loading states para evitar double-submit
- ✅ Mensajes de error descriptivos

### Backend (Esperado)
- ✅ Validación de rol ALMACENISTA
- ✅ Validación de `user_warehouse_assignments`
- ✅ Validación de período activo/cerrado
- ✅ Validación de estados de marbetes
- ✅ Validación de existencias en inventario
- ✅ Permisos CRUD según rol

---

## 🎯 Códigos de Error Manejados

El sistema maneja los siguientes códigos de error específicos:

| Código | Mensaje | Acción |
|--------|---------|--------|
| `PERIOD_CLOSED` | Período cerrado | Contactar administrador |
| `PERIOD_LOCKED` | Período bloqueado | Sin modificaciones |
| `NO_STOCK` | Sin existencias | Validar inventario |
| `INVALID_STATE` | Estado inválido | Verificar flujo |
| `LABEL_NOT_FOUND` | Folio no encontrado | Validar número |
| `DUPLICATE_COUNT` | Conteo duplicado | Ya registrado |
| `COUNT_SEQUENCE_ERROR` | Error de secuencia | Registrar C1 primero |
| `LABEL_ALREADY_CANCELLED` | Ya cancelado | No repetir |
| `CATALOG_NOT_LOADED` | Catálogo no importado | Importar primero |
| `INVALID_QUANTITY` | Cantidad inválida | Validar input |
| `WAREHOUSE_NOT_FOUND` | Almacén no existe | Validar selección |
| `PRODUCT_NOT_FOUND` | Producto no existe | Validar catálogo |
| `PERMISSION_DENIED` | Sin permisos | Contactar admin |

---

## ✅ Estado de Implementación

| Funcionalidad | APIs Consumidas | Estado |
|--------------|-----------------|--------|
| Consulta y Captura | 6/6 | ✅ Completo |
| Impresión | 5/5 | ✅ Completo |
| Conteo | 6/6 | ✅ Completo |
| Cancelación | 4/4 | ✅ Completo |
| Registro Detallado | 3/3 | ✅ Completo |
| **TOTAL** | **24/24** | **✅ 100%** |

---

## 📝 Notas Técnicas

### Configuración de Axios
Todos los endpoints se consumen a través de `@/config/axiosConfig.ts` que maneja:
- ✅ Autenticación con JWT
- ✅ Interceptores de request/response
- ✅ Timeout y retry logic
- ✅ Manejo centralizado de errores
- ✅ Headers comunes (Content-Type, Authorization)

### Helpers Utilizados
- `ToastSuccess()` - Notificaciones de éxito
- `ToastError()` - Notificaciones de error
- `LoadAlert()` - Indicador de carga global
- `Swal.fire()` - Modales de confirmación
- `handleAPIError()` - Parser de errores de API

### Store de Período
El módulo utiliza `@/store/periodoStore.ts` para:
- ✅ Persistir período seleccionado entre pantallas
- ✅ Evitar recargar período en cada navegación
- ✅ Sincronizar período entre componentes

---

**Fecha de documentación:** 2026-01-30  
**Versión de API:** v2  
**Estado:** ✅ Todos los endpoints documentados y funcionando
