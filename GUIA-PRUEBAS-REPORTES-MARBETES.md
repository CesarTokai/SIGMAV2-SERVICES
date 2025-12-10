# Guía de Pruebas - Reportes de Marbetes

## 📋 Índice
1. [Requisitos Previos](#requisitos-previos)
2. [Configuración de Postman](#configuración-de-postman)
3. [APIs de Reportes](#apis-de-reportes)
4. [Ejemplos de Respuestas](#ejemplos-de-respuestas)
5. [Casos de Prueba](#casos-de-prueba)
6. [Solución de Problemas](#solución-de-problemas)

---

## Requisitos Previos

### 1. Datos Necesarios
Antes de probar los reportes, asegúrate de tener:

- ✅ Un periodo creado (`periodId`)
- ✅ Almacenes asignados al usuario (`warehouseId`)
- ✅ Marbetes generados e impresos
- ✅ Al menos algunos conteos registrados (C1 y/o C2)
- ✅ Token JWT válido

### 2. Verificar Datos Base
```http
GET /api/sigmav2/periods/current
Authorization: Bearer {token}
```

```http
GET /api/sigmav2/warehouses/my-warehouses
Authorization: Bearer {token}
```

---

## Configuración de Postman

### Variables de Entorno
Crea las siguientes variables en Postman:

```json
{
    "baseUrl": "http://localhost:8080",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "periodId": 16,
    "warehouseId": 369
}
```

### Headers Comunes
Para todas las peticiones:

```
Content-Type: application/json
Authorization: Bearer {{token}}
```

---

## APIs de Reportes

### 1. 📊 Reporte de Distribución de Marbetes

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/distribution
```

**Body (para un almacén específico):**
```json
{
    "periodId": {{periodId}},
    "warehouseId": {{warehouseId}}
}
```

**Body (para todos los almacenes):**
```json
{
    "periodId": {{periodId}},
    "warehouseId": null
}
```

**Respuesta Esperada:**
```json
[
    {
        "usuario": "admin@tokai.com",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "primerFolio": 1000,
        "ultimoFolio": 1050,
        "totalMarbetes": 51
    },
    {
        "usuario": "almacenista@tokai.com",
        "claveAlmacen": "ALM002",
        "nombreAlmacen": "Almacén Secundario",
        "primerFolio": 1051,
        "ultimoFolio": 1100,
        "totalMarbetes": 50
    }
]
```

---

### 2. 📋 Reporte de Listado de Marbetes

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/list
```

**Body:**
```json
{
    "periodId": {{periodId}},
    "warehouseId": {{warehouseId}}
}
```

**Respuesta Esperada:**
```json
[
    {
        "numeroMarbete": 1000,
        "claveProducto": "PROD001",
        "descripcionProducto": "Producto de Prueba",
        "unidad": "PZA",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "conteo1": 100.00,
        "conteo2": 98.00,
        "estado": "IMPRESO",
        "cancelado": false
    },
    {
        "numeroMarbete": 1001,
        "claveProducto": "PROD002",
        "descripcionProducto": "Otro Producto",
        "unidad": "KG",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "conteo1": 50.00,
        "conteo2": null,
        "estado": "IMPRESO",
        "cancelado": false
    }
]
```

---

### 3. ⏳ Reporte de Marbetes Pendientes

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/pending
```

**Body:**
```json
{
    "periodId": {{periodId}},
    "warehouseId": {{warehouseId}}
}
```

**Descripción:**
- Solo muestra marbetes sin ambos conteos (C1 o C2 faltante)
- Excluye marbetes cancelados

**Respuesta Esperada:**
```json
[
    {
        "numeroMarbete": 1001,
        "claveProducto": "PROD002",
        "descripcionProducto": "Producto Sin C2",
        "unidad": "PZA",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "conteo1": 50.00,
        "conteo2": null,
        "estado": "IMPRESO"
    },
    {
        "numeroMarbete": 1005,
        "claveProducto": "PROD006",
        "descripcionProducto": "Producto Sin Conteos",
        "unidad": "LT",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "conteo1": null,
        "conteo2": null,
        "estado": "IMPRESO"
    }
]
```

---

### 4. ⚠️ Reporte de Marbetes con Diferencias

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/with-differences
```

**Body:**
```json
{
    "periodId": {{periodId}},
    "warehouseId": {{warehouseId}}
}
```

**Descripción:**
- Solo muestra marbetes donde C1 ≠ C2
- Ambos conteos deben existir

**Respuesta Esperada:**
```json
[
    {
        "numeroMarbete": 1000,
        "claveProducto": "PROD001",
        "descripcionProducto": "Producto con Diferencia",
        "unidad": "PZA",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "conteo1": 100.00,
        "conteo2": 98.00,
        "diferencia": 2.00,
        "estado": "IMPRESO"
    }
]
```

---

### 5. ❌ Reporte de Marbetes Cancelados

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/cancelled
```

**Body:**
```json
{
    "periodId": {{periodId}},
    "warehouseId": {{warehouseId}}
}
```

**Descripción:**
- Consulta la tabla `labels_cancelled`
- Incluye información del usuario que canceló

**Respuesta Esperada:**
```json
[
    {
        "numeroMarbete": 1020,
        "claveProducto": "PROD020",
        "descripcionProducto": "Producto Cancelado",
        "unidad": "PZA",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "conteo1": 10.00,
        "conteo2": null,
        "motivoCancelacion": "Error en etiqueta",
        "canceladoAt": "2025-12-10T10:30:00",
        "canceladoPor": "admin@tokai.com"
    }
]
```

---

### 6. 📊 Reporte Comparativo

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/comparative
```

**Body:**
```json
{
    "periodId": {{periodId}},
    "warehouseId": null
}
```

**Descripción:**
- Compara existencias físicas (conteos) vs teóricas (inventory_stock)
- Agrupa por producto y almacén
- Calcula diferencias y porcentajes

**Respuesta Esperada:**
```json
[
    {
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "claveProducto": "PROD001",
        "descripcionProducto": "Producto A",
        "unidad": "PZA",
        "existenciasFisicas": 98.00,
        "existenciasTeoricas": 100.00,
        "diferencia": -2.00,
        "porcentajeDiferencia": -2.00
    },
    {
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "claveProducto": "PROD002",
        "descripcionProducto": "Producto B",
        "unidad": "KG",
        "existenciasFisicas": 105.00,
        "existenciasTeoricas": 100.00,
        "diferencia": 5.00,
        "porcentajeDiferencia": 5.00
    }
]
```

---

### 7. 🏢 Reporte de Almacén con Detalle

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/warehouse-detail
```

**Body:**
```json
{
    "periodId": {{periodId}},
    "warehouseId": {{warehouseId}}
}
```

**Descripción:**
- Muestra cada marbete individual con sus existencias
- Ordenado por almacén → producto → folio

**Respuesta Esperada:**
```json
[
    {
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "claveProducto": "PROD001",
        "descripcionProducto": "Producto A",
        "unidad": "PZA",
        "numeroMarbete": 1000,
        "cantidad": 98.00,
        "estado": "IMPRESO",
        "cancelado": false
    },
    {
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "claveProducto": "PROD002",
        "descripcionProducto": "Producto B",
        "unidad": "KG",
        "numeroMarbete": 1001,
        "cantidad": 50.00,
        "estado": "IMPRESO",
        "cancelado": false
    }
]
```

---

### 8. 📦 Reporte de Producto con Detalle

**Endpoint:**
```
POST {{baseUrl}}/api/sigmav2/labels/reports/product-detail
```

**Body:**
```json
{
    "periodId": {{periodId}},
    "warehouseId": null
}
```

**Descripción:**
- Agrupa por producto mostrando ubicaciones en diferentes almacenes
- Incluye total de existencias del producto

**Respuesta Esperada:**
```json
[
    {
        "claveProducto": "PROD001",
        "descripcionProducto": "Producto A",
        "unidad": "PZA",
        "claveAlmacen": "ALM001",
        "nombreAlmacen": "Almacén Principal",
        "numeroMarbete": 1000,
        "existencias": 98.00,
        "total": 198.00
    },
    {
        "claveProducto": "PROD001",
        "descripcionProducto": "Producto A",
        "unidad": "PZA",
        "claveAlmacen": "ALM002",
        "nombreAlmacen": "Almacén Secundario",
        "numeroMarbete": 2000,
        "existencias": 100.00,
        "total": 198.00
    }
]
```

**Nota:** El campo `total` es la suma de existencias del producto en TODOS los almacenes.

---

## Casos de Prueba

### Caso 1: Reporte con Todos los Almacenes

**Objetivo:** Obtener reporte de todos los almacenes a los que el usuario tiene acceso

**Request:**
```json
{
    "periodId": 16,
    "warehouseId": null
}
```

**Validación:**
- ✅ Debe incluir datos de múltiples almacenes
- ✅ Solo almacenes asignados al usuario (excepto ADMINISTRADOR)
- ✅ Respuesta HTTP 200

---

### Caso 2: Reporte de Almacén Específico

**Objetivo:** Obtener reporte de un almacén específico

**Request:**
```json
{
    "periodId": 16,
    "warehouseId": 369
}
```

**Validación:**
- ✅ Debe incluir solo datos del almacén 369
- ✅ Usuario debe tener acceso al almacén
- ✅ Respuesta HTTP 200

---

### Caso 3: Acceso Denegado

**Objetivo:** Verificar que se deniegue el acceso a almacenes no asignados

**Request (usuario sin acceso al almacén 999):**
```json
{
    "periodId": 16,
    "warehouseId": 999
}
```

**Validación:**
- ✅ Respuesta HTTP 403 Forbidden
- ✅ Mensaje: "No tiene acceso al almacén especificado"

---

### Caso 4: Reporte sin Datos

**Objetivo:** Verificar respuesta cuando no hay datos

**Request (periodo sin marbetes):**
```json
{
    "periodId": 99,
    "warehouseId": 369
}
```

**Validación:**
- ✅ Respuesta HTTP 200
- ✅ Array vacío `[]`

---

### Caso 5: Filtro de Marbetes Pendientes

**Objetivo:** Verificar que solo se incluyan marbetes sin ambos conteos

**Precondiciones:**
- Crear marbetes con diferentes estados de conteo:
  - Marbete A: Sin conteos
  - Marbete B: Solo C1
  - Marbete C: C1 y C2 completos
  - Marbete D: Solo C2 (caso raro)

**Validación:**
- ✅ Debe incluir solo A, B, y D
- ✅ No debe incluir C (tiene ambos conteos)

---

### Caso 6: Filtro de Marbetes con Diferencias

**Objetivo:** Verificar que solo se incluyan marbetes con C1 ≠ C2

**Precondiciones:**
- Crear marbetes:
  - Marbete A: C1=100, C2=100 (iguales)
  - Marbete B: C1=100, C2=98 (diferentes)
  - Marbete C: C1=50, C2=null (incompleto)

**Validación:**
- ✅ Debe incluir solo B
- ✅ No debe incluir A (iguales) ni C (incompleto)

---

### Caso 7: Reporte Comparativo - Cálculo de Diferencias

**Objetivo:** Verificar cálculos de diferencias y porcentajes

**Precondiciones:**
- Producto X en inventory_stock: 100 unidades
- Conteos físicos: 95 unidades

**Validación:**
- ✅ `existenciasFisicas`: 95.00
- ✅ `existenciasTeoricas`: 100.00
- ✅ `diferencia`: -5.00
- ✅ `porcentajeDiferencia`: -5.00

---

### Caso 8: Reporte de Producto - Suma de Totales

**Objetivo:** Verificar que el total sea la suma correcta

**Precondiciones:**
- Producto Y en ALM001: 50 unidades (Marbete 1000)
- Producto Y en ALM002: 75 unidades (Marbete 2000)
- Producto Y en ALM003: 25 unidades (Marbete 3000)

**Validación:**
- ✅ Cada registro debe tener `total`: 150.00
- ✅ Suma de existencias: 50 + 75 + 25 = 150

---

## Solución de Problemas

### Error 403 - Forbidden

**Problema:**
```json
{
    "error": "No tiene acceso al almacén especificado"
}
```

**Solución:**
1. Verificar que el usuario tenga asignado el almacén
2. Consultar almacenes asignados: `GET /api/sigmav2/warehouses/my-warehouses`
3. Usar `warehouseId: null` para obtener todos los almacenes permitidos

---

### Error 401 - Unauthorized

**Problema:**
```json
{
    "error": "Token inválido o expirado"
}
```

**Solución:**
1. Renovar el token haciendo login nuevamente
2. Verificar que el header `Authorization` esté presente
3. Formato correcto: `Bearer {token}` (con espacio)

---

### Reporte Vacío []

**Problema:**
El endpoint devuelve un array vacío pero debería tener datos

**Solución:**
1. Verificar que existan marbetes para el periodo:
   ```http
   GET /api/sigmav2/labels/debug/count?periodId=16&warehouseId=369
   ```

2. Verificar que los marbetes estén impresos (para reporte de distribución):
   ```http
   POST /api/sigmav2/labels/summary
   Body: {"periodId": 16, "warehouseId": 369}
   ```

3. Para reportes de conteos, verificar que existan conteos registrados

---

### Error 500 - Internal Server Error

**Problema:**
```json
{
    "error": "Error interno del servidor"
}
```

**Solución:**
1. Revisar logs del servidor para detalles
2. Verificar integridad de datos en la BD
3. Validar que las relaciones de FK existan (products, warehouses, etc.)

---

### Datos Inconsistentes en Reportes

**Problema:**
Los totales no cuadran o hay datos faltantes

**Solución:**
1. Verificar sincronización de `inventory_stock`:
   ```sql
   SELECT * FROM inventory_stock
   WHERE period_id = 16 AND warehouse_id = 369;
   ```

2. Verificar eventos de conteo:
   ```sql
   SELECT * FROM label_count_events
   WHERE folio IN (SELECT folio FROM labels WHERE period_id = 16);
   ```

3. Verificar marbetes cancelados:
   ```sql
   SELECT * FROM labels_cancelled
   WHERE period_id = 16 AND reactivado = false;
   ```

---

## Colección de Postman

Para facilitar las pruebas, importa la siguiente colección:

```json
{
    "info": {
        "name": "SIGMAV2 - Reportes de Marbetes",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "item": [
        {
            "name": "1. Distribución de Marbetes",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    },
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"periodId\": {{periodId}},\n    \"warehouseId\": {{warehouseId}}\n}"
                },
                "url": {
                    "raw": "{{baseUrl}}/api/sigmav2/labels/reports/distribution",
                    "host": ["{{baseUrl}}"],
                    "path": ["api", "sigmav2", "labels", "reports", "distribution"]
                }
            }
        },
        {
            "name": "2. Listado de Marbetes",
            "request": {
                "method": "POST",
                "header": [
                    {
                        "key": "Content-Type",
                        "value": "application/json"
                    },
                    {
                        "key": "Authorization",
                        "value": "Bearer {{token}}"
                    }
                ],
                "body": {
                    "mode": "raw",
                    "raw": "{\n    \"periodId\": {{periodId}},\n    \"warehouseId\": {{warehouseId}}\n}"
                },
                "url": {
                    "raw": "{{baseUrl}}/api/sigmav2/labels/reports/list",
                    "host": ["{{baseUrl}}"],
                    "path": ["api", "sigmav2", "labels", "reports", "list"]
                }
            }
        }
    ]
}
```

---

## Checklist de Verificación

Antes de marcar los reportes como completos, verificar:

- [ ] Todos los endpoints responden con 200 OK con datos válidos
- [ ] Los filtros por periodo funcionan correctamente
- [ ] Los filtros por almacén funcionan correctamente
- [ ] El filtro con `warehouseId: null` incluye todos los almacenes permitidos
- [ ] Las validaciones de seguridad funcionan (403 para almacenes no asignados)
- [ ] Los cálculos de totales y diferencias son correctos
- [ ] Los reportes excluyen marbetes cancelados (donde corresponde)
- [ ] Los reportes incluyen marbetes cancelados (donde corresponde)
- [ ] La ordenación de resultados es la esperada
- [ ] Los campos nulos se manejan correctamente
- [ ] El performance es aceptable con grandes volúmenes de datos

---

**Documento generado:** 10 de diciembre de 2025
**Versión:** 1.0

