# SIGMAV2 — Inventario Completo de APIs
> Generado: 2026-03-11 | Actualizado: 2026-03-11 | Base URL: `http://localhost:8080`
> ✅ = En colección | ⚠️ = Incompleto/Inconsistente | ❌ = Falta en colección
> 
> **Estado actual:** Colección actualizada — se agregaron 19 requests al folder `🏷️ Marbetes — Flujo Completo` y se corrigieron 11 URLs incorrectas en Admin APIs.

---

## 🔐 Autenticación (`/api/sigmav2/auth` · `/api/sigmav2/users`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/users/register` | ✅ | Registrar usuario |
| 2 | POST | `/api/sigmav2/users/verify` | ✅ | Verificar con código email |
| 3 | POST | `/api/sigmav2/users/resend-verification-code` | ✅ | Reenviar código |
| 4 | POST | `/api/sigmav2/auth/login` | ✅ | Login → JWT |
| 5 | GET  | `/api/sigmav2/auth/getPage` | ✅ | Ver solicitudes de recuperación |
| 6 | GET  | `/api/sigmav2/auth/getHistory` | ✅ | Historial de solicitudes |
| 7 | POST | `/api/sigmav2/auth/createRequest` | ✅ | Crear solicitud de recuperación |
| 8 | POST | `/api/sigmav2/auth/resolveRequest` | ✅ | Aceptar/resolver solicitud |

---

## 👤 Usuario Autenticado (`/api/sigmav2/users/me`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | GET | `/api/sigmav2/users/me/security` | ✅ | Datos de seguridad propios |
| 2 | GET | `/api/sigmav2/users/me/activity` | ✅ | Actividad propia |
| 3 | GET | `/api/sigmav2/users/me/assignments` | ✅ | Asignaciones propias |
| 4 | GET | `/api/sigmav2/users/me/personal-info` | ✅ | Info personal propia |
| 5 | GET | `/api/sigmav2/users/me/complete` | ✅ | Perfil completo propio |

---

## 🔍 Consultas de Usuario

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | GET | `/api/sigmav2/users/me/complete` | ✅ | Mi información completa |
| 2 | GET | `/api/sigmav2/users/complete/email/{email}` | ✅ | Usuario completo por email |
| 3 | GET | `/api/sigmav2/users/complete/id/{id}` | ✅ | Usuario completo por ID |
| 4 | GET | `/api/sigmav2/users/search?email=` | ✅ | Buscar usuario por email (admin) |

---

## 📋 Información Personal (`/api/sigmav2/personal-information`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST   | `/api/sigmav2/personal-information` | ✅ | Crear info personal |
| 2 | PUT    | `/api/sigmav2/personal-information` | ✅ | Actualizar info personal |
| 3 | GET    | `/api/sigmav2/personal-information/user/` | ✅ | Redirigida a `/users/me/personal-info` |
| 4 | GET    | `/api/sigmav2/personal-information/user/{id}` | ✅ | Info personal por ID de usuario |
| 5 | POST   | `/api/sigmav2/personal-information/upload-image` | ✅ | Subir imagen de perfil |
| 6 | PUT    | `/api/sigmav2/personal-information/update-image` | ✅ | Actualizar imagen |
| 7 | GET    | `/api/sigmav2/personal-information/image` | ✅ | Obtener info de imagen |
| 8 | DELETE | `/api/sigmav2/personal-information/image` | ✅ | Eliminar imagen |
| 9 | POST   | `/api/sigmav2/personal-information/assign-role` | ✅ | Asignar rol (admin) |
| 10 | GET   | `/api/sigmav2/personal-information/exists` | ✅ | Verificar si existe info personal |

---

## ⚙️ Admin — Usuarios (`/api/sigmav2/admin/users`)

> Todas las URLs corregidas a `/api/sigmav2/admin/...`

| # | Método | Endpoint correcto | Estado | Notas |
|---|--------|-------------------|--------|-------|
| 1  | POST | `/api/sigmav2/auth/login` | ✅ | Login admin |
| 2  | GET  | `/api/sigmav2/admin/users?page=&size=&sortBy=&sortDir=` | ✅ | Listar usuarios paginado |
| 3  | GET  | `/api/sigmav2/admin/users?page=&size=&email=&verified=&status=` | ✅ | Buscar con filtros |
| 4  | GET  | `/api/sigmav2/admin/users/{id}` | ✅ | Obtener usuario por ID |
| 5  | POST | `/api/sigmav2/users/register` | ✅ | Crear usuario (admin) |
| 6  | PUT  | `/api/sigmav2/admin/users/{id}` | ✅ | Actualizar usuario |
| 7  | DELETE | `/api/sigmav2/admin/users/{id}` | ✅ | Eliminar usuario |
| 8  | POST | `/api/sigmav2/admin/users/{id}/toggle-status` | ✅ | Activar/desactivar |
| 9  | POST | `/api/sigmav2/admin/users/{id}/force-verify` | ✅ | Verificar forzoso |
| 10 | POST | `/api/sigmav2/admin/users/{id}/reset-attempts` | ✅ | Resetear intentos |
| 11 | POST | `/api/sigmav2/admin/users/{id}/resend-verification` | ✅ | Reenviar verificación |
| 12 | DELETE | `/api/sigmav2/admin/users/cleanup-unverified?daysOld=7` | ✅ | Limpiar no verificados (7d) |
| 13 | DELETE | `/api/sigmav2/admin/users/cleanup-unverified?daysOld=30` | ✅ | Limpiar no verificados (30d) |
| 14 | GET  | `/api/sigmav2/admin/users/statistics` | ✅ | Estadísticas generales |
| 15 | POST | `/api/sigmav2/admin/users/bulk-action` (ACTIVATE) | ✅ | Activar múltiples |
| 16 | POST | `/api/sigmav2/admin/users/bulk-action` (DEACTIVATE) | ✅ | Desactivar múltiples |
| 17 | POST | `/api/sigmav2/admin/users/bulk-action` (DELETE) | ✅ | Eliminar múltiples |
| 18 | POST | `/api/sigmav2/admin/users/bulk-action` (CHANGE_ROLE) | ✅ | Cambiar rol masivamente |
| 19 | POST | `/api/sigmav2/admin/users/bulk-action` (RESET_ATTEMPTS) | ✅ | Resetear intentos masivamente |
| 20 | POST | `/api/sigmav2/admin/users/bulk-action` (RESEND_VERIFICATION) | ✅ | Reenviar verificación masivamente |
| 21 | POST | `/api/sigmav2/admin/users/bulk-action` (FORCE_VERIFY) | ✅ | Verificar múltiples |

---

## 🔒 Admin — Seguridad (`/api/sigmav2/users/admin`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/users/admin/activity-summary/by-email` | ✅ | Resumen de actividad |
| 2 | POST | `/api/sigmav2/users/admin/activity/by-email` | ✅ | Actividad detallada |
| 3 | POST | `/api/sigmav2/users/admin/assignments/by-email` | ✅ | Almacenes por email |
| 4 | POST | `/api/sigmav2/users/admin/personal-info/by-email` | ✅ | Info personal por email |
| 5 | POST | `/api/sigmav2/users/admin/activity-log/by-email` | ✅ | Historial completo paginado |
| 6 | GET  | `/api/sigmav2/admin/users/with-warehouses` | ✅ | Ver almacenes asignados |

---

## 🏭 Almacenes (`/api/sigmav2/warehouses`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1  | GET    | `/api/sigmav2/warehouses?page=&size=&sortBy=&sortDir=&search=` | ✅ | Listar/Buscar |
| 2  | GET    | `/api/sigmav2/warehouses/{id}` | ✅ | Obtener por ID |
| 3  | POST   | `/api/sigmav2/warehouses` | ✅ | Crear almacén (admin) |
| 4  | PUT    | `/api/sigmav2/warehouses/{id}` | ✅ | Actualizar almacén |
| 5  | DELETE | `/api/sigmav2/warehouses/{id}` | ✅ | Eliminar almacén |
| 6  | GET    | `/api/sigmav2/warehouses/my-warehouses` | ✅ | Mis almacenes asignados |
| 7  | GET    | `/api/sigmav2/warehouses/users/{userId}` | ✅ | Almacenes de un usuario |
| 8  | POST   | `/api/sigmav2/warehouses/users/{userId}/assign` | ✅ | Asignar almacenes |
| 9  | DELETE | `/api/sigmav2/warehouses/users/{userId}/warehouses/{warehouseId}` | ✅ | Revocar almacén |
| 10 | GET    | `/api/sigmav2/warehouses/users-with-assignments` | ✅ | Almacenes con asignaciones |

---

## 📦 Inventario (`/api/sigmav2/inventory`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/inventory/import` | ✅ | Importar Excel (multipart: periodId, file) |
| 2 | POST | `/api/sigmav2/inventory/import/preview` | ✅ | Preview de importación |
| 3 | GET  | `/api/sigmav2/inventory/query` | ✅ | Consultar con filtros |
| 4 | GET  | `/api/sigmav2/inventory/query/import?jobId=` | ✅ | Estado de importación |
| 5 | GET  | `/api/sigmav2/inventory/stock?productId=&warehouseId=` | ✅ | Stock actual |
| 6 | GET  | `/api/sigmav2/inventory/snapshots?period=` | ✅ | Snapshots por periodo |
| 7 | GET  | `/api/sigmav2/inventory/period-report?periodId=&size=&page=&search=&sort=` | ✅ | Reporte de periodo |
| 8 | GET  | `/api/sigmav2/inventory/all-periods` | ✅ | Todos los periodos |
| 9 | GET  | `/api/sigmav2/inventory/debug/period/{id}` | ✅ | Debug de periodo |

---

## 🏬 Multi-Almacén (`/api/sigmav2/multi-warehouse`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | GET  | `/api/sigmav2/multi-warehouse/existences?page=&size=&period_id=` | ✅ | Consultar existencias |
| 2 | POST | `/api/sigmav2/multi-warehouse/import` | ✅ | Importar Excel multi-almacén |
| 3 | POST | `/api/sigmav2/multi-warehouse/import/preview` | ✅ | Preview de importación |
| 4 | GET  | `/api/sigmav2/multi-warehouse/stock` | ✅ | Stock por producto/almacén/periodo |
| 5 | GET  | `/api/sigmav2/multiwarehouse/summary` | ✅ | Resumen por almacén |

---

## 📅 Periodos (`/api/sigmav2/periods`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/periods` | ✅ | Crear periodo |
| 2 | GET  | `/api/sigmav2/periods/{id}` | ✅ | Obtener por ID |
| 3 | GET  | `/api/sigmav2/periods?page=&size=` | ✅ | Listar paginado |
| 4 | POST | `/api/sigmav2/periods/update-comments` | ✅ | Actualizar comentarios |
| 5 | POST | `/api/sigmav2/periods/delete` | ✅ | Eliminar periodo |
| 6 | POST | `/api/sigmav2/periods/close` | ✅ | Cerrar periodo |
| 7 | POST | `/api/sigmav2/periods/open` | ✅ | Abrir periodo |
| 8 | POST | `/api/sigmav2/periods/{id}/{status}` | ✅ | Cambiar estado (ej. CLOSED) |

---

## 🏷️ Marbetes — Fase 1: Generación (`/api/sigmav2/labels`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/labels/request` | ✅ | Solicitar folios por producto |
| 2 | POST | `/api/sigmav2/labels/generate` | ✅ | Generar marbetes (1 solicitud) |
| 3 | POST | `/api/sigmav2/labels/generate/batch` | ✅ | Generar lote masivo (lista de productos) |
| 4 | POST | `/api/sigmav2/labels/generate-and-print` | ✅ | Generar + imprimir en un paso |

---

## 🏷️ Marbetes — Fase 2: Verificación y Consulta

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/labels/pending-print-count` | ✅ | Contar pendientes de impresión |
| 2 | GET  | `/api/sigmav2/labels/debug/count?periodId=&warehouseId=` | ✅ | Total de marbetes (diagnóstico) |
| 3 | POST | `/api/sigmav2/labels/summary` | ✅ | Resumen por producto |
| 4 | GET  | `/api/sigmav2/labels/status?folio=&periodId=&warehouseId=` | ✅ | Estado de un marbete |
| 5 | GET  | `/api/sigmav2/labels/by-folio/{folio}` | ✅ | Buscar marbete por folio (GET) |
| 6 | GET  | `/api/sigmav2/labels/product/{productId}?periodId=&warehouseId=` | ✅ | Marbetes de un producto |
| 7 | GET  | `/api/sigmav2/labels/cancelled?periodId=&warehouseId=` | ✅ | Marbetes cancelados (GET) |

---

## 🏷️ Marbetes — Fase 3: Impresión

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/labels/print` | ✅ | Imprimir marbetes → PDF |
| 2 | POST | `/api/sigmav2/labels/for-extraordinary-reprint/list` | ✅ | Lista para reimpresión extraordinaria |
| 3 | POST | `/api/sigmav2/labels/extraordinary-reprint` | ✅ | Reimpresión extraordinaria → PDF |

---

## 🏷️ Marbetes — Fase 4: Conteo Físico

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | GET  | `/api/sigmav2/labels/for-count?folio=&periodId=&warehouseId=` | ✅ | Buscar marbete para contar (GET) |
| 2 | POST | `/api/sigmav2/labels/for-count` | ✅ | Buscar marbete para contar (POST) |
| 3 | POST | `/api/sigmav2/labels/for-count/list` | ✅ | Lista de marbetes para contar |
| 4 | POST | `/api/sigmav2/labels/counts/c1` | ✅ | Registrar primer conteo (C1) |
| 5 | POST | `/api/sigmav2/labels/counts/c2` | ✅ | Registrar segundo conteo (C2) |

---

## 🏷️ Marbetes — Fase 5: Corrección y Cancelación

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | PUT  | `/api/sigmav2/labels/counts/c1` | ✅ | Actualizar C1 |
| 2 | PUT  | `/api/sigmav2/labels/counts/c2` | ✅ | Actualizar C2 |
| 3 | POST | `/api/sigmav2/labels/cancel` | ✅ | Cancelar marbete |
| 4 | PUT  | `/api/sigmav2/labels/cancelled/update-stock` | ✅ | Actualizar stock de marbete cancelado |

---

## 🏷️ Marbetes — Fase 6: Exportación

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/labels/generate-file` | ✅ | Generar archivo TXT de existencias |

---

## 📊 Reportes — JSON (`/api/sigmav2/labels/reports`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/labels/reports/distribution` | ✅ | Distribución por estado |
| 2 | POST | `/api/sigmav2/labels/reports/list` | ✅ | Listado de marbetes |
| 3 | POST | `/api/sigmav2/labels/reports/pending` | ✅ | Marbetes pendientes |
| 4 | POST | `/api/sigmav2/labels/reports/with-differences` | ✅ | Con diferencias |
| 5 | POST | `/api/sigmav2/labels/reports/cancelled` | ✅ | Cancelados |
| 6 | POST | `/api/sigmav2/labels/reports/comparative` | ✅ | Comparativo |
| 7 | POST | `/api/sigmav2/labels/reports/warehouse-detail` | ✅ | Detalle por almacén |
| 8 | POST | `/api/sigmav2/labels/reports/product-detail` | ✅ | Detalle por producto |

---

## 📄 Reportes — PDF (`/api/sigmav2/labels/reports/.../pdf`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | POST | `/api/sigmav2/labels/reports/distribution/pdf` | ✅ | PDF distribución |
| 2 | POST | `/api/sigmav2/labels/reports/list/pdf` | ✅ | PDF listado |
| 3 | POST | `/api/sigmav2/labels/reports/pending/pdf` | ✅ | PDF pendientes |
| 4 | POST | `/api/sigmav2/labels/reports/with-differences/pdf` | ✅ | PDF diferencias |
| 5 | POST | `/api/sigmav2/labels/reports/cancelled/pdf` | ✅ | PDF cancelados |
| 6 | POST | `/api/sigmav2/labels/reports/comparative/pdf` | ✅ | PDF comparativo |
| 7 | POST | `/api/sigmav2/labels/reports/warehouse-detail/pdf` | ✅ | PDF detalle almacén |
| 8 | POST | `/api/sigmav2/labels/reports/warehouse-detail/all/pdf` | ✅ | PDF todos los almacenes |
| 9 | POST | `/api/sigmav2/labels/reports/product-detail/pdf` | ✅ | PDF detalle producto |

---

## 🔧 Debug — Jasper (`/api/debug/jasper`)

| # | Método | Endpoint | Estado | Notas |
|---|--------|----------|--------|-------|
| 1 | GET | `/api/debug/jasper/check-file` | ✅ | Verificar archivo Jasper |
| 2 | GET | `/api/debug/jasper/parse-xml` | ✅ | Parsear XML |
| 3 | GET | `/api/debug/jasper/compile` | ✅ | Compilar template |
| 4 | GET | `/api/debug/jasper/test-cache` | ✅ | Test de caché |

---

## ✅ Todas las APIs están en la colección

Todas las APIs del código han sido agregadas a `SIGMAV2-COLLECTION.yaml`.

**Nuevo folder agregado:** `🏷️ Marbetes — Flujo Completo` con 19 requests:
- `POST /labels/request`
- `POST /labels/generate`
- `POST /labels/generate/batch`
- `POST /labels/generate-and-print`
- `POST /labels/pending-print-count`
- `GET  /labels/debug/count`
- `GET  /labels/by-folio/{folio}`
- `GET  /labels/product/{productId}`
- `GET  /labels/cancelled`
- `GET  /labels/for-count` (variante GET)
- `POST /labels/counts/c1`
- `POST /labels/counts/c2`
- `PUT  /labels/counts/c1`
- `PUT  /labels/counts/c2`
- `POST /labels/cancel`
- `PUT  /labels/cancelled/update-stock`
- `POST /labels/for-extraordinary-reprint/list`
- `POST /labels/extraordinary-reprint`
- `POST /labels/generate-file`

---

## ✅ Inconsistencias en Admin corregidas

Los siguientes 11 endpoints fueron corregidos de `/api/admin/...` a `/api/sigmav2/admin/...`:

```
✅ Desactivar Múltiples Usuarios      → /api/sigmav2/admin/users/bulk-action
✅ Eliminar Múltiples Usuarios        → /api/sigmav2/admin/users/bulk-action
✅ Obtener Usuario por ID             → /api/sigmav2/admin/users/16
✅ Resetear Intentos de Login         → /api/sigmav2/admin/users/10/reset-attempts
✅ Limpiar No Verificados (7 días)    → /api/sigmav2/admin/users/cleanup-unverified
✅ Cambiar Rol Masivamente            → /api/sigmav2/admin/users/bulk-action
✅ Eliminar Usuario                   → /api/sigmav2/admin/users/15
✅ Reenviar Verificación Masivamente  → /api/sigmav2/admin/users/bulk-action
✅ Reenviar Código de Verificación    → /api/sigmav2/admin/users/10/resend-verification
✅ Resetear Intentos Masivamente      → /api/sigmav2/admin/users/bulk-action
✅ Limpiar No Verificados (30 días)   → /api/sigmav2/admin/users/cleanup-unverified
✅ Actualizar Usuario                 → /api/sigmav2/admin/users/10
✅ Resetear password                  → /api/sigmav2/auth/createRequest
✅ assignments (espacio al final)     → /api/sigmav2/users/me/assignments
✅ assignments/by-email (espacio)     → /api/sigmav2/users/admin/assignments/by-email
✅ personal-information/user/ (sin ID)→ /api/sigmav2/users/me/personal-info
```

---

## 📝 Bodies de Referencia para APIs Faltantes

### `POST /api/sigmav2/labels/request`
```json
{ "productId": 123, "warehouseId": 14, "periodId": 1, "requestedLabels": 10 }
```

### `POST /api/sigmav2/labels/generate`
```json
{ "requestId": 456, "periodId": 1, "warehouseId": 14 }
```

### `POST /api/sigmav2/labels/generate/batch`
```json
{
  "periodId": 1, "warehouseId": 14,
  "products": [
    { "productId": 123, "requestedLabels": 10 },
    { "productId": 124, "requestedLabels": 5 }
  ]
}
```

### `POST /api/sigmav2/labels/generate-and-print`
```json
{
  "periodId": 1, "warehouseId": 14,
  "products": [{ "productId": 123, "requestedLabels": 10 }]
}
```

### `POST /api/sigmav2/labels/pending-print-count`
```json
{ "periodId": 1, "warehouseId": 14 }
```

### `GET /api/sigmav2/labels/debug/count`
```
?periodId=1&warehouseId=14
```

### `POST /api/sigmav2/labels/counts/c1`
```json
{ "folio": 1001, "countedValue": 95 }
```

### `POST /api/sigmav2/labels/counts/c2`
```json
{ "folio": 1001, "countedValue": 93 }
```

### `PUT /api/sigmav2/labels/counts/c1`
```json
{ "folio": 1001, "countedValue": 96, "observaciones": "Corrección de captura" }
```

### `PUT /api/sigmav2/labels/counts/c2`
```json
{ "folio": 1001, "countedValue": 94, "observaciones": "Ajuste por reconteo" }
```

### `POST /api/sigmav2/labels/cancel`
```json
{ "folio": 1001, "reason": "Producto no encontrado", "existenciasActuales": 0 }
```

### `PUT /api/sigmav2/labels/cancelled/update-stock`
```json
{ "folio": 1001, "existenciasActuales": 5 }
```

### `POST /api/sigmav2/labels/generate-file`
```json
{ "periodId": 1 }
```

### `POST /api/sigmav2/labels/for-extraordinary-reprint/list`
```json
{ "periodId": 1, "warehouseId": 14 }
```

### `POST /api/sigmav2/labels/extraordinary-reprint`
```json
{ "periodId": 1, "warehouseId": 14, "folios": [1001, 1002], "forceReprint": true }
```

