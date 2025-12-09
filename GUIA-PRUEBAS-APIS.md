# 🚀 APIs de Cancelación y Reportes de Marbetes - Guía de Pruebas

## 📋 Requisitos Previos

1. **Servidor corriendo**: El servidor debe estar ejecutándose en `http://localhost:8080`
2. **Token JWT**: Necesitas obtener un token de autenticación primero

---

## 🔐 Paso 1: Obtener Token JWT

### Login
**URL**: `POST http://localhost:8080/api/sigmav2/auth/login`

**Body**:
```json
{
  "email": "tu_email@example.com",
  "password": "tu_password"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Nota**: Copia el token del response para usarlo en las siguientes llamadas.

---

## 📝 Headers para Todas las APIs

Todas las siguientes APIs requieren estos headers:

```
Content-Type: application/json
Authorization: Bearer TU_TOKEN_JWT_AQUI
```

---

## 1️⃣ Cancelar Marbete

**URL**: `POST http://localhost:8080/api/sigmav2/labels/cancel`

**Body**:
```json
{
  "folio": 1001,
  "periodId": 1,
  "warehouseId": 2,
  "motivoCancelacion": "Error en conteo físico"
}
```

**Response Exitoso**: `200 OK` (sin body)

**Ejemplo Postman/cURL**:
```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/cancel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TU_TOKEN" \
  -d '{
    "folio": 1001,
    "periodId": 1,
    "warehouseId": 2,
    "motivoCancelacion": "Error en conteo físico"
  }'
```

---

## 2️⃣ Reporte de Distribución de Marbetes

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/distribution`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": null
}
```

**Nota**: `warehouseId` puede ser `null` para todos los almacenes o un ID específico (ej: `2`)

**Response**:
```json
[
  {
    "usuario": "juan.perez@example.com",
    "claveAlmacen": "A01",
    "nombreAlmacen": "Almacén Principal",
    "primerFolio": 1001,
    "ultimoFolio": 1050,
    "totalMarbetes": 50
  }
]
```

---

## 3️⃣ Reporte de Listado de Marbetes

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/list`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response**:
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

---

## 4️⃣ Reporte de Marbetes Pendientes

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/pending`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response**:
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

---

## 5️⃣ Reporte de Marbetes con Diferencias

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/with-differences`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response**:
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

---

## 6️⃣ Reporte de Marbetes Cancelados

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/cancelled`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response**:
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
    "canceladoPor": "juan.perez@example.com"
  }
]
```

---

## 7️⃣ Reporte Comparativo

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/comparative`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response**:
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
    "porcentajeDiferencia": -5.0000
  }
]
```

---

## 8️⃣ Reporte de Almacén con Detalle

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/warehouse-detail`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": 2
}
```

**Response**:
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

---

## 9️⃣ Reporte de Producto con Detalle

**URL**: `POST http://localhost:8080/api/sigmav2/labels/reports/product-detail`

**Body**:
```json
{
  "periodId": 1,
  "warehouseId": null
}
```

**Nota**: Usa `null` en `warehouseId` para ver el producto en todos los almacenes

**Response**:
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

---

## 🧪 Colección Postman

### Importar en Postman

1. Crea una nueva colección llamada "Marbetes - Cancelación y Reportes"
2. Agrega una variable de entorno `base_url` = `http://localhost:8080`
3. Agrega una variable de entorno `token` = `tu_token_jwt`
4. Crea los 9 requests con las URLs y bodies de arriba

### Variables de Entorno Postman
```json
{
  "base_url": "http://localhost:8080",
  "token": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "periodId": 1,
  "warehouseId": 2
}
```

Luego usa `{{base_url}}`, `{{token}}`, etc. en tus requests.

---

## 💡 Tips para Pruebas

### 1. Orden de Pruebas Recomendado
```
1. Login → Obtener token
2. Reporte de Listado → Ver qué marbetes existen
3. Reporte de Pendientes → Ver cuáles tienen conteos incompletos
4. Reporte con Diferencias → Ver cuáles tienen discrepancias
5. Cancelar Marbete → Probar cancelación
6. Reporte de Cancelados → Verificar que se canceló
7. Reporte Comparativo → Ver diferencias físico vs teórico
8. Reporte de Distribución → Ver distribución por almacén
9. Reportes de Detalle → Ver información detallada
```

### 2. Datos de Prueba
- **periodId**: Usa el ID del periodo activo en tu base de datos
- **warehouseId**: Usa un ID de almacén que exista
- **folio**: Usa un folio de marbete que exista y no esté cancelado

### 3. Verificar Datos en Base de Datos
```sql
-- Ver periodos disponibles
SELECT * FROM periods;

-- Ver almacenes disponibles
SELECT * FROM warehouse;

-- Ver marbetes generados
SELECT * FROM labels WHERE id_period = 1 LIMIT 10;

-- Ver conteos
SELECT * FROM label_count_events WHERE folio IN (SELECT folio FROM labels WHERE id_period = 1) LIMIT 10;
```

---

## ⚠️ Posibles Errores

### Error 401 - Unauthorized
```json
{
  "error": "Unauthorized"
}
```
**Solución**: Verifica que el token JWT sea válido y esté en el header `Authorization: Bearer TOKEN`

### Error 403 - Forbidden
```json
{
  "error": "Forbidden"
}
```
**Solución**: El usuario no tiene permisos. Verifica que tenga uno de los roles permitidos.

### Error 404 - Not Found
```json
{
  "error": "Marbete no encontrado"
}
```
**Solución**: El folio/periodo/almacén no existe. Verifica los IDs en la base de datos.

### Error 400 - Bad Request
```json
{
  "error": "Validation failed",
  "details": ["El folio es obligatorio"]
}
```
**Solución**: Revisa que el body tenga todos los campos requeridos.

---

## 📱 Ejemplo con JavaScript (Fetch)

```javascript
// Login
const loginResponse = await fetch('http://localhost:8080/api/sigmav2/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'tu_email@example.com',
    password: 'tu_password'
  })
});
const { token } = await loginResponse.json();

// Cancelar Marbete
const cancelResponse = await fetch('http://localhost:8080/api/sigmav2/labels/cancel', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    folio: 1001,
    periodId: 1,
    warehouseId: 2,
    motivoCancelacion: 'Error en conteo'
  })
});

// Obtener Reporte de Distribución
const reportResponse = await fetch('http://localhost:8080/api/sigmav2/labels/reports/distribution', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify({
    periodId: 1,
    warehouseId: null
  })
});
const reportData = await reportResponse.json();
console.log(reportData);
```

---

## ✅ Checklist de Pruebas

- [ ] ✅ Login exitoso y token obtenido
- [ ] ✅ API 1: Cancelar marbete - Response 200
- [ ] ✅ API 2: Distribución - Response con datos
- [ ] ✅ API 3: Listado - Response con datos
- [ ] ✅ API 4: Pendientes - Response con datos
- [ ] ✅ API 5: Con diferencias - Response con datos
- [ ] ✅ API 6: Cancelados - Response incluye el cancelado
- [ ] ✅ API 7: Comparativo - Response con cálculos correctos
- [ ] ✅ API 8: Almacén detalle - Response con desglose
- [ ] ✅ API 9: Producto detalle - Response con totales
- [ ] ✅ Verificar en BD que los datos se guardaron

---

**¡Listo para probar! 🚀**

Si tienes algún problema, revisa:
1. El servidor está corriendo
2. El token es válido
3. Los IDs (periodId, warehouseId, folio) existen en la BD
4. Los headers están correctos

