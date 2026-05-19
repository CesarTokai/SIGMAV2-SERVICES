# 📋 APIs para Probar - Módulo de Conteo y Cancelación de Marbetes

## 🔍 Respuesta a tus Preguntas

### 1. **¿Por qué los reportes se quedan volátiles?**

Los reportes actualmente se generan como **JSON dinámico** y se retornan directamente al cliente. No se guardan como PDFs en el servidor. Esto es por diseño, ya que:

- **Ventaja**: Los datos siempre están actualizados
- **Desventaja**: No hay histórico de reportes generados

**Solución implementada**: Los reportes JSON pueden ser exportados a PDF/Excel desde el frontend según necesites.

### 2. **¿Por qué no me muestra los registros para ingresar los conteos?**

El problema era que **no existía un endpoint específico** para consultar un marbete individual en la interfaz de conteo.

**Solución implementada**: He creado el endpoint `GET /api/sigmav2/labels/for-count` que devuelve toda la información necesaria para la interfaz de conteo.

---

## 🆕 Nueva API Implementada

### **1. Obtener Marbete para Interfaz de Conteo**

**Endpoint**: `GET /api/sigmav2/labels/for-count`

**Descripción**: Obtiene la información completa de un marbete para mostrarlo en la interfaz de conteo.

**Parámetros** (Query):
```json
{
  "folio": 10001,
  "periodId": 1,
  "warehouseId": 1
}
```

**Ejemplo de uso**:
```bash
GET http://localhost:8080/api/sigmav2/labels/for-count?folio=10001&periodId=1&warehouseId=1
Authorization: Bearer {tu_token_jwt}
```

**Respuesta exitosa** (200 OK):
```json
{
  "folio": 10001,
  "periodId": 1,
  "warehouseId": 1,
  "claveAlmacen": "ALM01",
  "nombreAlmacen": "Almacén Principal",
  "claveProducto": "PROD001",
  "descripcionProducto": "Producto de Ejemplo",
  "unidadMedida": "PZ",
  "cancelado": false,
  "conteo1": 100.00,
  "conteo2": 98.00,
  "diferencia": -2.00,
  "estado": "IMPRESO",
  "impreso": true,
  "mensaje": "Ambos conteos ya están registrados"
}
```

**Casos de uso**:
- ✅ Marbete sin conteos: `"mensaje": "Listo para registrar el primer conteo"`
- ✅ Solo con C1: `"mensaje": "Primer conteo registrado, falta el segundo conteo"`
- ✅ Con ambos conteos: `"mensaje": "Ambos conteos ya están registrados"`
- ❌ Cancelado: `"mensaje": "Este marbete está CANCELADO y no puede ser usado para conteo"`

---

## 🆕 **NUEVO: Listar Todos los Marbetes para Conteo** ⭐

### **1.1 Listar Marbetes Disponibles para Conteo**

**Endpoint**: `POST /api/sigmav2/labels/for-count/list`

**Descripción**: Obtiene una lista completa de TODOS los marbetes IMPRESOS disponibles para conteo en un periodo y almacén específico. **Este es el endpoint que necesitas para ver los marbetes que ya fueron impresos.**

**Body** (JSON):
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Ejemplo de uso**:
```bash
POST http://localhost:8080/api/sigmav2/labels/for-count/list
Authorization: Bearer {tu_token_jwt}
Content-Type: application/json

Body:
{
  "periodId": 16,
  "warehouseId": 369
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "folio": 10001,
    "periodId": 1,
    "warehouseId": 1,
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "claveProducto": "PROD001",
    "descripcionProducto": "Producto de Ejemplo",
    "unidadMedida": "PZ",
    "cancelado": false,
    "conteo1": null,
    "conteo2": null,
    "diferencia": null,
    "estado": "IMPRESO",
    "impreso": true,
    "mensaje": "Pendiente C1"
  },
  {
    "folio": 10002,
    "periodId": 1,
    "warehouseId": 1,
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "claveProducto": "PROD002",
    "descripcionProducto": "Otro Producto",
    "unidadMedida": "KG",
    "cancelado": false,
    "conteo1": 50.00,
    "conteo2": null,
    "diferencia": null,
    "estado": "IMPRESO",
    "impreso": true,
    "mensaje": "Pendiente C2"
  },
  {
    "folio": 10003,
    "periodId": 1,
    "warehouseId": 1,
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "claveProducto": "PROD003",
    "descripcionProducto": "Producto Completo",
    "unidadMedida": "PZ",
    "cancelado": false,
    "conteo1": 100.00,
    "conteo2": 98.00,
    "diferencia": -2.00,
    "estado": "IMPRESO",
    "impreso": true,
    "mensaje": "Completo"
  }
]
```

**Estados del mensaje**:
- ✅ **"Pendiente C1"**: Marbete listo para el primer conteo
- ✅ **"Pendiente C2"**: Ya tiene C1, falta el segundo conteo
- ✅ **"Completo"**: Ambos conteos registrados

**Ventajas de este endpoint**:
- ✅ Muestra TODOS los marbetes impresos de una vez
- ✅ Indica el estado de cada marbete (Pendiente C1, Pendiente C2, Completo)
- ✅ Ordenados por folio para fácil navegación
- ✅ Filtra automáticamente solo los marbetes en estado IMPRESO
- ✅ Excluye los marbetes cancelados

---

## 📝 APIs de Conteo (Ya existentes)

### **2. Registrar Primer Conteo (C1)**

**Endpoint**: `POST /api/sigmav2/labels/counts/c1`

**Descripción**: Registra el primer conteo de un marbete.

**Body**:
```json
{
  "folio": 10001,
  "countedValue": 100.50
}
```

**Roles permitidos**: ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO

**Respuesta exitosa** (200 OK):
```json
{
  "idCountEvent": 1,
  "folio": 10001,
  "userId": 5,
  "countNumber": 1,
  "countedValue": 100.50,
  "role": "ALMACENISTA",
  "createdAt": "2025-12-09T09:30:00",
  "isSecondCount": false
}
```

---

### **3. Registrar Segundo Conteo (C2)**

**Endpoint**: `POST /api/sigmav2/labels/counts/c2`

**Descripción**: Registra el segundo conteo de un marbete (solo AUXILIAR_DE_CONTEO).

**Body**:
```json
{
  "folio": 10001,
  "countedValue": 98.00
}
```

**Roles permitidos**: AUXILIAR_DE_CONTEO (únicamente)

**Respuesta exitosa** (200 OK):
```json
{
  "idCountEvent": 2,
  "folio": 10001,
  "userId": 7,
  "countNumber": 2,
  "countedValue": 98.00,
  "role": "AUXILIAR_DE_CONTEO",
  "createdAt": "2025-12-09T10:15:00",
  "isSecondCount": true
}
```

---

## 🚫 API de Cancelación

### **4. Cancelar Marbete**

**Endpoint**: `POST /api/sigmav2/labels/cancel`

**Descripción**: Cancela un folio de marbete desde la interfaz de conteo.

**Body**:
```json
{
  "folio": 10001,
  "periodId": 1,
  "warehouseId": 1,
  "motivoCancelacion": "Error en impresión del código de barras"
}
```

**Roles permitidos**: ADMINISTRADOR, AUXILIAR, ALMACENISTA, AUXILIAR_DE_CONTEO

**Respuesta exitosa** (200 OK):
```
(vacío - solo status 200)
```

---

## 📊 APIs de Reportes

### **5. Reporte de Listado de Marbetes**

**Endpoint**: `POST /api/sigmav2/labels/reports/list`

**Descripción**: Lista todos los marbetes generados con sus conteos.

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "numeroMarbete": 10001,
    "claveProducto": "PROD001",
    "descripcionProducto": "Producto Ejemplo",
    "unidadMedida": "PZ",
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": 100.00,
    "conteo2": 98.00,
    "estado": "IMPRESO",
    "cancelado": false
  }
]
```

---

### **6. Reporte de Marbetes Pendientes**

**Endpoint**: `POST /api/sigmav2/labels/reports/pending`

**Descripción**: Muestra solo los marbetes que faltan conteos (C1 o C2).

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "numeroMarbete": 10002,
    "claveProducto": "PROD002",
    "descripcionProducto": "Producto Sin Conteo",
    "unidadMedida": "PZ",
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": 50.00,
    "conteo2": null,
    "estado": "IMPRESO"
  }
]
```

---

### **7. Reporte de Marbetes con Diferencias**

**Endpoint**: `POST /api/sigmav2/labels/reports/with-differences`

**Descripción**: Muestra marbetes donde C1 ≠ C2.

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "numeroMarbete": 10001,
    "claveProducto": "PROD001",
    "descripcionProducto": "Producto con Diferencia",
    "unidadMedida": "PZ",
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": 100.00,
    "conteo2": 98.00,
    "diferencia": 2.00,
    "estado": "IMPRESO"
  }
]
```

---

### **8. Reporte de Marbetes Cancelados**

**Endpoint**: `POST /api/sigmav2/labels/reports/cancelled`

**Descripción**: Lista de todos los marbetes cancelados.

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "numeroMarbete": 10050,
    "claveProducto": "PROD005",
    "descripcionProducto": "Producto Cancelado",
    "unidadMedida": "PZ",
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "conteo1": null,
    "conteo2": null,
    "motivoCancelacion": "Error en impresión",
    "canceladoAt": "2025-12-09T08:30:00",
    "canceladoPor": "usuario@example.com"
  }
]
```

---

### **9. Reporte Comparativo (Físico vs Teórico)**

**Endpoint**: `POST /api/sigmav2/labels/reports/comparative`

**Descripción**: Compara existencias físicas contadas vs existencias teóricas del sistema.

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "claveProducto": "PROD001",
    "descripcionProducto": "Producto Ejemplo",
    "unidadMedida": "PZ",
    "existenciasFisicas": 98.00,
    "existenciasTeoricas": 100.00,
    "diferencia": -2.00,
    "porcentajeDiferencia": -2.00
  }
]
```

---

### **10. Reporte de Distribución de Marbetes**

**Endpoint**: `POST /api/sigmav2/labels/reports/distribution`

**Descripción**: Muestra la distribución de folios por almacén y usuario.

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": null
}
```

**Nota**: Si `warehouseId` es `null`, muestra todos los almacenes.

**Respuesta exitosa** (200 OK):
```json
[
  {
    "usuario": "admin@example.com",
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "primerFolio": 10001,
    "ultimoFolio": 10100,
    "totalFolios": 100
  }
]
```

---

### **11. Reporte de Almacén con Detalle**

**Endpoint**: `POST /api/sigmav2/labels/reports/warehouse-detail`

**Descripción**: Desglose de inventario físico por almacén, mostrando cada marbete.

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 1
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "claveProducto": "PROD001",
    "descripcionProducto": "Producto Ejemplo",
    "unidadMedida": "PZ",
    "numeroMarbete": 10001,
    "cantidad": 98.00,
    "estado": "IMPRESO",
    "cancelado": false
  }
]
```

---

### **12. Reporte de Producto con Detalle**

**Endpoint**: `POST /api/sigmav2/labels/reports/product-detail`

**Descripción**: Desglose de inventario físico por producto, mostrando ubicaciones.

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": null
}
```

**Respuesta exitosa** (200 OK):
```json
[
  {
    "claveProducto": "PROD001",
    "descripcionProducto": "Producto Ejemplo",
    "unidadMedida": "PZ",
    "claveAlmacen": "ALM01",
    "nombreAlmacen": "Almacén Principal",
    "numeroMarbete": 10001,
    "existencias": 98.00,
    "total": 248.00
  },
  {
    "claveProducto": "PROD001",
    "descripcionProducto": "Producto Ejemplo",
    "unidadMedida": "PZ",
    "claveAlmacen": "ALM02",
    "nombreAlmacen": "Almacén Secundario",
    "numeroMarbete": 10101,
    "existencias": 150.00,
    "total": 248.00
  }
]
```

---

## 🔐 Headers Requeridos

Todas las APIs requieren autenticación JWT:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

---

## ✅ Flujo Completo de Uso

### **Escenario: Registrar Conteo de Marbetes**

1. **Listar TODOS los marbetes disponibles para conteo** (⭐ NUEVO):
   ```
   POST /api/sigmav2/labels/for-count/list
   Body: { "periodId": 1, "warehouseId": 1 }
   ```
   **Respuesta**: Lista completa de marbetes impresos con su estado actual

2. **Seleccionar un marbete de la lista y registrar C1**:
   ```
   POST /api/sigmav2/labels/counts/c1
   Body: { "folio": 10001, "countedValue": 100.50 }
   ```

3. **Registrar C2** (solo AUXILIAR_DE_CONTEO):
   ```
   POST /api/sigmav2/labels/counts/c2
   Body: { "folio": 10001, "countedValue": 98.00 }
   ```

4. **Si necesitas información detallada de un marbete específico**:
   ```
   GET /api/sigmav2/labels/for-count?folio=10001&periodId=1&warehouseId=1
   ```

5. **Si hay error, cancelar el marbete**:
   ```
   POST /api/sigmav2/labels/cancel
   Body: { "folio": 10001, "periodId": 1, "warehouseId": 1, "motivoCancelacion": "Error en conteo" }
   ```

6. **Generar reporte de diferencias**:
   ```
   POST /api/sigmav2/labels/reports/with-differences
   Body: { "periodId": 1, "warehouseId": 1 }
   ```

---

## 🎯 Resumen de Soluciones Implementadas

✅ **NUEVO: Endpoint para listar marbetes**: `POST /api/sigmav2/labels/for-count/list` (⭐ **ESTE RESUELVE TU PROBLEMA**)
✅ **Endpoint para marbete individual**: `GET /api/sigmav2/labels/for-count`
✅ **Los reportes funcionan correctamente** (devuelven JSON dinámico)
✅ **Cancelación de marbetes funcional**
✅ **8 tipos diferentes de reportes disponibles**
✅ **Validaciones de reglas de negocio implementadas**

---

## 🚨 IMPORTANTE: Respuesta a tu Problema

**TU PREGUNTA**: "Ya lo hice pero no me muestra los registros del periodo y almacén de ese periodo que ya están impresos esos marbetes"

**SOLUCIÓN**: Usa el NUEVO endpoint que acabo de crear:

```bash
POST http://localhost:8080/api/sigmav2/labels/for-count/list
Content-Type: application/json
Authorization: Bearer {tu_token}

Body:
{
  "periodId": 16,
  "warehouseId": 369
}
```

Este endpoint:
- ✅ **Lista TODOS los marbetes IMPRESOS** del periodo y almacén
- ✅ **Muestra el estado actual de cada marbete** (Pendiente C1, Pendiente C2, Completo)
- ✅ **Incluye toda la información necesaria** para la interfaz de conteo
- ✅ **Solo muestra marbetes en estado IMPRESO** (listos para conteo)
- ✅ **Excluye los marbetes cancelados**

### **Pasos para usar**:

1. **Asegúrate de tener el servidor corriendo**:
   ```
   .\mvnw.cmd spring-boot:run
   ```

2. **Llama al endpoint con tu token JWT**:
   ```
   GET http://localhost:8080/api/sigmav2/labels/for-count/list?periodId=1&warehouseId=1
   Authorization: Bearer {tu_token}
   ```

3. **Recibirás la lista completa de marbetes** listos para conteo

---

## 🚀 URLs Base

- **Desarrollo**: `http://localhost:8080`
- **Producción**: Configura según tu servidor

Todos los endpoints comienzan con: `/api/sigmav2/labels/`

