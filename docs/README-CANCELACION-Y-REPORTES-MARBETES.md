# Documentación de APIs de Cancelación y Reportes de Marbetes

## Fecha de Implementación
8 de Diciembre de 2025

## Descripción General
Este documento describe las APIs implementadas para la cancelación de marbetes y la generación de 8 reportes específicos del módulo de marbetes.

---

## 1. API de Cancelación de Marbetes

### Endpoint: Cancelar Marbete
**URL:** `POST /api/sigmav2/labels/cancel`

**Descripción:** Permite cancelar un folio de marbete desde la interfaz de conteo. Todos los usuarios con los roles adecuados pueden efectuar esta operación.

**Roles Permitidos:**
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

**Request Body:**
```json
{
  "folio": 1001,
  "periodId": 1,
  "warehouseId": 2,
  "motivoCancelacion": "Error en conteo físico"
}
```

**Response:** `200 OK` (sin body)

**Proceso:**
1. Seleccionar el periodo
2. Ingresar el número de folio del marbete
3. Se mostrarán los datos del folio (almacén, producto, descripción, estado, conteos)
4. Marcar la casilla "Cancelado"
5. El folio queda cancelado inmediatamente

**Validaciones:**
- El folio debe existir
- El folio debe pertenecer al periodo y almacén especificado
- El folio no debe estar ya cancelado
- El usuario debe tener acceso al almacén

**Efectos:**
- Cambia el estado del marbete a `CANCELADO` en la tabla `labels`
- Registra la cancelación en la tabla `labels_cancelled` con:
  - Folio cancelado
  - Usuario que canceló
  - Fecha de cancelación
  - Motivo de cancelación
  - Existencias al momento de cancelar

---

## 2. APIs de Reportes

Todos los reportes comparten el mismo filtro base y estructura de autorización.

### Filtro Base (ReportFilterDTO)
```json
{
  "periodId": 1,
  "warehouseId": 2  // Opcional: null para todos los almacenes
}
```

### Roles Permitidos (Todos los Reportes)
- ADMINISTRADOR
- AUXILIAR
- ALMACENISTA
- AUXILIAR_DE_CONTEO

---

### 2.1 Reporte de Distribución de Marbetes

**URL:** `POST /api/sigmav2/labels/reports/distribution`

**Descripción:** Presenta la distribución de folios por almacén basado en marbetes impresos, mostrando el usuario que realizó la captura y generación.

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": null  // null = todos los almacenes
}
```

**Response:**
```json
[
  {
    "usuario": "Juan Pérez",
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "primerFolio": 1001,
    "ultimoFolio": 1050,
    "totalMarbetes": 50
  }
]
```

**Columnas:**
- Usuario que generó los marbetes
- Clave del almacén
- Nombre del almacén
- Primer folio asignado
- Último folio asignado
- Total de marbetes generados

---

### 2.2 Reporte de Listado de Marbetes

**URL:** `POST /api/sigmav2/labels/reports/list`

**Descripción:** Listado completo de todos los marbetes generados por almacén.

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response:**
```json
[
  {
    "numeroMarbete": 1001,
    "claveProducto": "P001",
    "descripcionProducto": "Producto Ejemplo",
    "unidad": "PZA",
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": 100.00,
    "conteo2": 100.00,
    "estado": "IMPRESO",
    "cancelado": false
  }
]
```

**Columnas:**
- Número de marbete (folio)
- Clave del producto
- Descripción del producto
- Unidad de medida
- Clave del almacén
- Nombre del almacén
- Primer conteo
- Segundo conteo
- Estado (GENERADO, IMPRESO, CANCELADO)
- Indicador si está cancelado

---

### 2.3 Reporte de Marbetes Pendientes

**URL:** `POST /api/sigmav2/labels/reports/pending`

**Descripción:** Muestra solo los marbetes que aún no tienen aplicados ambos conteos (C1 y C2).

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response:**
```json
[
  {
    "numeroMarbete": 1002,
    "claveProducto": "P002",
    "descripcionProducto": "Producto Pendiente",
    "unidad": "KG",
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": 50.00,
    "conteo2": null,
    "estado": "IMPRESO"
  }
]
```

**Criterio de Filtrado:**
- Marbetes no cancelados
- Falta conteo1 O falta conteo2

---

### 2.4 Reporte de Marbetes con Diferencias

**URL:** `POST /api/sigmav2/labels/reports/with-differences`

**Descripción:** Presenta los marbetes que exhiben diferencias entre sus conteos (C1 ≠ C2) donde ya se efectuaron ambos conteos.

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response:**
```json
[
  {
    "numeroMarbete": 1003,
    "claveProducto": "P003",
    "descripcionProducto": "Producto con Diferencia",
    "unidad": "LT",
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": 100.00,
    "conteo2": 95.00,
    "diferencia": 5.00,
    "estado": "IMPRESO"
  }
]
```

**Criterio de Filtrado:**
- Marbetes no cancelados
- Conteo1 existe Y conteo2 existe
- Conteo1 ≠ Conteo2

**Columnas Adicionales:**
- Diferencia (valor absoluto de C1 - C2)

---

### 2.5 Reporte de Marbetes Cancelados

**URL:** `POST /api/sigmav2/labels/reports/cancelled`

**Descripción:** Listado de marbetes que fueron cancelados.

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response:**
```json
[
  {
    "numeroMarbete": 1004,
    "claveProducto": "P004",
    "descripcionProducto": "Producto Cancelado",
    "unidad": "PZA",
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": 10.00,
    "conteo2": null,
    "motivoCancelacion": "Error en conteo físico",
    "canceladoAt": "2025-12-08T10:30:00",
    "canceladoPor": "Juan Pérez"
  }
]
```

**Columnas Adicionales:**
- Motivo de cancelación
- Fecha y hora de cancelación
- Usuario que canceló

---

### 2.6 Reporte Comparativo

**URL:** `POST /api/sigmav2/labels/reports/comparative`

**Descripción:** Presenta las diferencias entre lo que en teoría debe existir (existencias teóricas del sistema) contra lo que físicamente se contó.

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response:**
```json
[
  {
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "claveProducto": "P001",
    "descripcionProducto": "Producto Ejemplo",
    "unidad": "PZA",
    "existenciasFisicas": 95.00,
    "existenciasTeoricas": 100.00,
    "diferencia": -5.00,
    "porcentajeDiferencia": -5.00
  }
]
```

**Columnas:**
- Clave del almacén
- Nombre del almacén
- Clave del producto
- Descripción del producto
- Unidad de medida
- Existencias físicas (suma de conteos de marbetes)
- Existencias teóricas (de `inventory_stock`)
- Diferencia (físicas - teóricas)
- Porcentaje de diferencia

**Cálculos:**
- **Existencias Físicas:** Suma de conteo2 (o conteo1 si no hay conteo2) de todos los marbetes del producto
- **Existencias Teóricas:** Valor de `exist_qty` en `inventory_stock`
- **Diferencia:** Existencias Físicas - Existencias Teóricas
- **Porcentaje:** (Diferencia / Existencias Teóricas) × 100

---

### 2.7 Reporte de Almacén con Detalle

**URL:** `POST /api/sigmav2/labels/reports/warehouse-detail`

**Descripción:** Desglose completo del inventario físico por cada almacén, mostrando cada marbete generado con su cantidad de existencias físicas.

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response:**
```json
[
  {
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "claveProducto": "P001",
    "descripcionProducto": "Producto Ejemplo",
    "unidad": "PZA",
    "numeroMarbete": 1001,
    "cantidad": 100.00,
    "estado": "IMPRESO",
    "cancelado": false
  }
]
```

**Columnas:**
- Clave del almacén
- Nombre del almacén
- Clave del producto
- Descripción del producto
- Unidad de medida
- Número de marbete
- Cantidad (conteo2 o conteo1)
- Estado del marbete
- Indicador si está cancelado

**Orden:** Por almacén → producto → número de marbete

---

### 2.8 Reporte de Producto con Detalle

**URL:** `POST /api/sigmav2/labels/reports/product-detail`

**Descripción:** Desglose del inventario físico por producto, mostrando información detallada incluyendo ubicación en almacén, folio de marbete, cantidad y total acumulado.

**Request Body:**
```json
{
  "periodId": 1,
  "warehouseId": null  // null para ver producto en todos los almacenes
}
```

**Response:**
```json
[
  {
    "claveProducto": "P001",
    "descripcionProducto": "Producto Ejemplo",
    "unidad": "PZA",
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "numeroMarbete": 1001,
    "existencias": 100.00,
    "total": 250.00
  }
]
```

**Columnas:**
- Clave del producto
- Descripción del producto
- Unidad de medida
- Clave del almacén (ubicación)
- Nombre del almacén
- Número de marbete
- Existencias (cantidad en este marbete)
- Total (suma de existencias del producto en todos los almacenes)

**Orden:** Por producto → almacén → número de marbete

**Nota:** El campo `total` es el mismo para todas las filas del mismo producto, representando la suma total en todos los almacenes.

---

## Ejemplos de Uso con cURL

### Cancelar un Marbete
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/cancel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "folio": 1001,
    "periodId": 1,
    "warehouseId": 2,
    "motivoCancelacion": "Error en conteo"
  }'
```

### Generar Reporte de Distribución
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/reports/distribution \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "periodId": 1,
    "warehouseId": null
  }'
```

### Generar Reporte Comparativo
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/reports/comparative \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "periodId": 1,
    "warehouseId": 2
  }'
```

---

## Reglas de Negocio Implementadas

### Cancelación de Marbetes
1. **Todos los usuarios** con roles adecuados pueden cancelar marbetes
2. **Validación de permisos:** El usuario debe tener acceso al almacén
3. **No duplicar cancelaciones:** No se puede cancelar un marbete ya cancelado
4. **Trazabilidad completa:** Se registra quién, cuándo y por qué se canceló
5. **Preservar información:** Se guardan las existencias al momento de la cancelación

### Reportes
1. **Control de acceso:** Solo usuarios autorizados pueden generar reportes
2. **Filtrado por almacén:** Permite ver un almacén específico o todos
3. **Filtrado por periodo:** Obligatorio para todos los reportes
4. **Exclusión de cancelados:** La mayoría de los reportes excluyen marbetes cancelados (excepto el reporte de cancelados)
5. **Ordenamiento lógico:** Cada reporte se ordena de forma lógica para facilitar su lectura

### Cálculos en Reportes
1. **Existencias físicas:** Se prefiere conteo2 sobre conteo1
2. **Diferencias:** Siempre valor absoluto para facilitar visualización
3. **Totales:** Se calculan dinámicamente al momento de generar el reporte
4. **Porcentajes:** Con 4 decimales de precisión

---

## Notas Técnicas

### Arquitectura
- **Patrón:** Arquitectura Hexagonal (Clean Architecture)
- **Capa de Aplicación:** DTOs y Service Interface
- **Capa de Dominio:** Modelos y Excepciones
- **Capa de Infraestructura:** Repositorios JPA
- **Capa de Adaptadores:** Controladores REST

### Transacciones
- **Cancelación:** Transaccional (`@Transactional`)
- **Reportes:** Solo lectura (`@Transactional(readOnly = true)`)

### Performance
- Los reportes cargan datos en memoria
- Para grandes volúmenes considerar implementar paginación
- Se recomienda usar caché para reportes frecuentes

### Extensibilidad
- Estructura preparada para agregar exportación a PDF usando JasperReports
- DTOs pueden extenderse sin romper compatibilidad
- Fácil agregar nuevos filtros en `ReportFilterDTO`

---

## Próximas Mejoras Sugeridas

1. **Exportación a PDF:** Implementar plantillas JasperReports para cada reporte
2. **Exportación a Excel:** Agregar opción de descarga en formato XLSX
3. **Paginación:** Implementar paginación para reportes grandes
4. **Cache:** Agregar cache con TTL para reportes frecuentes
5. **Filtros Avanzados:** Agregar filtros por fecha, rango de folios, etc.
6. **Gráficas:** Agregar endpoints para datos de gráficas del dashboard
7. **Notificaciones:** Notificar cuando se cancela un marbete
8. **Auditoría:** Registrar todas las consultas de reportes

---

## Soporte y Contacto

Para reportar problemas o solicitar mejoras, contactar al equipo de desarrollo.

**Versión del Documento:** 1.0
**Última Actualización:** 8 de Diciembre de 2025

