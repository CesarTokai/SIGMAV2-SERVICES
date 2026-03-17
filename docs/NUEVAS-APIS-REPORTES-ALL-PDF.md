# 📊 APIs de Reportes - SIGMAV2 (ACTUALIZADO)

## 🆕 Nuevas APIs Agregadas

Se han agregado **2 nuevos endpoints PDF** para generar reportes de **TODOS** los productos y **TODOS** los almacenes:

1. ✅ `POST /api/sigmav2/labels/reports/product-detail/all/pdf` — Detalle de TODOS los productos
2. ✅ `POST /api/sigmav2/labels/reports/comparative/all/pdf` — Comparativo de TODOS los almacenes

---

## 📋 Tabla Completa de APIs de Reportes

| # | Tipo | Endpoint | Descripción |
|---|------|----------|-------------|
| 1 | JSON | `/reports/distribution` | Distribución de marbetes |
| 2 | JSON | `/reports/list` | Listado completo |
| 3 | JSON | `/reports/pending` | Marbetes pendientes |
| 4 | JSON | `/reports/with-differences` | Con diferencias (C1 ≠ C2) |
| 5 | JSON | `/reports/cancelled` | Marbetes cancelados |
| 6 | JSON | `/reports/comparative` | Teórico vs Físico |
| 7 | JSON | `/reports/warehouse-detail` | Detalle por almacén |
| 8 | JSON | `/reports/product-detail` | Detalle por producto |
| 9 | PDF | `/reports/distribution/pdf` | Distribución (PDF) |
| 10 | PDF | `/reports/list/pdf` | Listado (PDF) |
| 11 | PDF | `/reports/pending/pdf` | Pendientes (PDF) |
| 12 | PDF | `/reports/with-differences/pdf` | Diferencias (PDF) |
| 13 | PDF | `/reports/cancelled/pdf` | Cancelados (PDF) |
| 14 | PDF | `/reports/comparative/pdf` | Comparativo (PDF) |
| 15 | PDF | `/reports/warehouse-detail/pdf` | Detalle almacén (PDF) |
| 16 | PDF | `/reports/warehouse-detail/all/pdf` | Todos almacenes (PDF) |
| 17 | PDF | `/reports/product-detail/pdf` | Detalle producto (PDF) |
| 18 | PDF | `/reports/product-detail/all/pdf` | **[NUEVO]** Todos productos (PDF) |
| 19 | PDF | `/reports/comparative/pdf` | Comparativo (PDF) |
| 20 | PDF | `/reports/comparative/all/pdf` | **[NUEVO]** Todos almacenes (PDF) |
| 21 | TXT | `/generate-file` | Archivo de existencias |

**Total: 20 APIs de reportes** (+ archivo TXT)

---

## 🆕 NUEVOS ENDPOINTS

### 1️⃣ **Detalle de TODOS los Productos (PDF)**
```
POST /api/sigmav2/labels/reports/product-detail/all/pdf
```

**Descripción:** Genera un PDF con el detalle de inventario para TODOS los productos del período, independientemente del almacén.

**Request:**
```json
{
  "periodId": 1
}
```

**Response:** `application/pdf` (archivo descargable)

**Diferencia vs `/product-detail/pdf`:**
- `/product-detail/pdf` — Requiere `warehouseId`, muestra productos de UN almacén
- `/product-detail/all/pdf` — No requiere `warehouseId`, muestra TODOS los productos

**Roles permitidos:** ADMINISTRADOR, AUXILIAR, ALMACENISTA, AUXILIAR_DE_CONTEO

**Ejemplo cURL:**
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/reports/product-detail/all/pdf \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"periodId": 1}' \
  --output productos_todos.pdf
```

---

### 2️⃣ **Comparativo de TODOS los Almacenes (PDF)**
```
POST /api/sigmav2/labels/reports/comparative/all/pdf
```

**Descripción:** Genera un PDF con el reporte comparativo (teórico vs físico) para TODOS los almacenes del período.

**Request:**
```json
{
  "periodId": 1
}
```

**Response:** `application/pdf` (archivo descargable)

**Diferencia vs `/comparative/pdf`:**
- `/comparative/pdf` — Requiere `warehouseId`, muestra comparativo de UN almacén
- `/comparative/all/pdf` — No requiere `warehouseId`, muestra TODOS los almacenes

**Roles permitidos:** ADMINISTRADOR, AUXILIAR, ALMACENISTA, AUXILIAR_DE_CONTEO

**Ejemplo cURL:**
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/reports/comparative/all/pdf \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"periodId": 1}' \
  --output comparativo_todos_almacenes.pdf
```

---

## 📝 Estructura de Requests

### Para reportes `/all/pdf`
```json
{
  "periodId": 1  // OBLIGATORIO — sin warehouseId
}
```

### Para reportes regulares con almacén específico
```json
{
  "periodId": 1,
  "warehouseId": 1  // OBLIGATORIO
}
```

---

## 🔐 Autenticación Requerida

```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

---

## 📊 Comparativa de Endpoints

| Endpoint | Sin Almacén | Con Almacén | Tipo |
|----------|-----------|-----------|------|
| `/reports/product-detail/pdf` | ❌ | ✅ Sí | PDF |
| `/reports/product-detail/all/pdf` | ✅ **[NUEVO]** | ❌ No | PDF |
| `/reports/comparative/pdf` | ❌ | ✅ Sí | PDF |
| `/reports/comparative/all/pdf` | ✅ **[NUEVO]** | ❌ No | PDF |
| `/reports/warehouse-detail/pdf` | ❌ | ✅ Sí | PDF |
| `/reports/warehouse-detail/all/pdf` | ✅ | ❌ No | PDF |

---

## 🎯 Casos de Uso

### Caso 1: Reporte de un producto específico en un almacén
```bash
POST /api/sigmav2/labels/reports/product-detail/pdf
{
  "periodId": 1,
  "warehouseId": 1,
  "productId": 101  # Opcional: filtrar por producto
}
```

### Caso 2: Reporte de TODOS los productos del período
```bash
POST /api/sigmav2/labels/reports/product-detail/all/pdf
{
  "periodId": 1
}
```

### Caso 3: Comparativo de un almacén
```bash
POST /api/sigmav2/labels/reports/comparative/pdf
{
  "periodId": 1,
  "warehouseId": 1
}
```

### Caso 4: Comparativo de TODOS los almacenes
```bash
POST /api/sigmav2/labels/reports/comparative/all/pdf
{
  "periodId": 1
}
```

---

## ✅ Validaciones y Errores

Si no se encuentran datos, ambos endpoints retornan:

```json
{
  "success": false,
  "message": "No se encontraron registros de detalle de todos los productos para el periodo 1 en todos los almacenes.",
  "periodId": 1,
  "warehouseId": "todos"
}
```

HTTP Status: `404 NOT FOUND`

---

## 📁 Cambios Realizados

**Archivo modificado:** 
- `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/adapter/controller/LabelsController.java`

**Métodos agregados:**
1. `getAllProductsDetailReportPdf()` — `/reports/product-detail/all/pdf`
2. `getAllComparativeReportPdf()` — `/reports/comparative/all/pdf`

**Patrón utilizado:** Similar a `getAllWarehousesDetailReportPdf()` existente

---

## 🔄 Detalles Técnicos

Ambos endpoints:
1. ✅ Extraen el `userId` y `userRole` del token JWT
2. ✅ Validan acceso del usuario (a través de `warehouseAccessService`)
3. ✅ Crean un `ReportFilterDTO` con `warehouseId = null` (todos los almacenes)
4. ✅ Llaman al servicio de reportes (`LabelReportService`)
5. ✅ Generan PDF con `JasperReportPdfService`
6. ✅ Retornan archivo PDF descargable con nombre único (timestamp)

---

## 📅 Versión

- **Versión:** 1.0.1
- **Fecha:** 2026-03-13
- **Estado:** ✅ Implementado
- **Base URL:** `http://localhost:8080/api/sigmav2/labels`


