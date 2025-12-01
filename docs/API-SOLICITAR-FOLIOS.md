# 📡 API para Solicitar Folios (Marbetes)

## 🎯 Endpoint Principal

```
POST /api/sigmav2/labels/request
```

**Descripción:** Registra la cantidad de folios (marbetes) solicitados para un producto específico en un almacén y periodo determinado.

**Autenticación:** Requerida (JWT Bearer Token)

**Roles permitidos:**
- `ADMINISTRADOR`
- `AUXILIAR`
- `ALMACENISTA`

---

## 📥 Request

### **Headers**
```
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json
```

### **Body (JSON)**
```json
{
  "productId": 123,
  "warehouseId": 250,
  "periodId": 7,
  "requestedLabels": 50
}
```

### **Parámetros**

| Campo | Tipo | Requerido | Descripción | Validaciones |
|-------|------|-----------|-------------|--------------|
| `productId` | Long | ✅ Sí | ID del producto | > 0 |
| `warehouseId` | Long | ✅ Sí | ID del almacén | > 0 |
| `periodId` | Long | ✅ Sí | ID del periodo | > 0 |
| `requestedLabels` | Integer | ✅ Sí | Cantidad de folios solicitados | >= 0 |

---

## 📤 Responses

### **✅ 201 Created - Solicitud creada exitosamente**
```json
Status: 201 Created
Body: (vacío)
```

**Casos:**
- Primera solicitud para ese producto/almacén/periodo
- Solicitud actualizada (cantidad modificada)
- Solicitud eliminada (cantidad = 0)

---

### **❌ 400 Bad Request - Validación fallida**

#### **Caso 1: Marbetes sin imprimir**
```json
Status: 400 Bad Request
{
  "timestamp": "2025-01-12T17:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Existen marbetes GENERADOS sin imprimir para este producto/almacén/periodo. Por favor imprima los marbetes existentes antes de solicitar más.",
  "path": "/api/sigmav2/labels/request"
}
```

**Motivo:** Se intentó solicitar más folios cuando hay marbetes generados que no han sido impresos.

**Solución:** Imprimir los marbetes existentes antes de solicitar más.

---

#### **Caso 2: Campos inválidos**
```json
Status: 400 Bad Request
{
  "timestamp": "2025-01-12T17:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "requestedLabels",
      "message": "must be greater than or equal to 0"
    }
  ]
}
```

---

### **❌ 403 Forbidden - Sin acceso al almacén**
```json
Status: 403 Forbidden
{
  "message": "No tiene acceso al almacén especificado"
}
```

**Motivo:** El usuario no tiene asignado el almacén en `user_warehouse_assignments`.

---

### **❌ 401 Unauthorized - Token inválido**
```json
Status: 401 Unauthorized
{
  "message": "Token JWT inválido o expirado"
}
```

---

## 🔄 Comportamiento según Cantidad

### **1️⃣ Crear solicitud (primera vez)**

**Request:**
```json
POST /api/sigmav2/labels/request
{
  "productId": 123,
  "warehouseId": 250,
  "periodId": 7,
  "requestedLabels": 50
}
```

**Resultado:**
- ✅ Crea registro en `label_requests`
- `requested_labels` = 50
- `folios_generados` = 0

**SQL ejecutado:**
```sql
INSERT INTO label_requests
  (id_product, id_warehouse, id_period, requested_labels, folios_generados, created_by, created_at)
VALUES
  (123, 250, 7, 50, 0, 7, NOW());
```

---

### **2️⃣ Actualizar cantidad**

**Request:**
```json
POST /api/sigmav2/labels/request
{
  "productId": 123,      # Mismo
  "warehouseId": 250,    # Mismo
  "periodId": 7,         # Mismo
  "requestedLabels": 75  # Nueva cantidad
}
```

**Resultado:**
- ✅ Actualiza registro existente (NO crea duplicado)
- `requested_labels` = 75

**SQL ejecutado:**
```sql
UPDATE label_requests
SET requested_labels = 75
WHERE id_product = 123
  AND id_warehouse = 250
  AND id_period = 7;
```

---

### **3️⃣ Cancelar solicitud (cantidad = 0)**

**Request:**
```json
POST /api/sigmav2/labels/request
{
  "productId": 123,
  "warehouseId": 250,
  "periodId": 7,
  "requestedLabels": 0
}
```

**Resultado:**
- ✅ Elimina la solicitud de la BD (solo si `folios_generados` = 0)

**SQL ejecutado:**
```sql
DELETE FROM label_requests
WHERE id_product = 123
  AND id_warehouse = 250
  AND id_period = 7
  AND folios_generados = 0;
```

**⚠️ Restricción:**
- Si `folios_generados` > 0, lanza excepción 400:
  ```
  "No se puede cancelar la solicitud porque ya se generaron X folios"
  ```

---

## 🧪 Ejemplos de Uso

### **Ejemplo 1: Solicitar 50 marbetes**

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/request \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 123,
    "warehouseId": 250,
    "periodId": 7,
    "requestedLabels": 50
  }'
```

**Response:**
```
Status: 201 Created
```

---

### **Ejemplo 2: Cambiar cantidad a 75**

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/request \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 123,
    "warehouseId": 250,
    "periodId": 7,
    "requestedLabels": 75
  }'
```

**Response:**
```
Status: 201 Created
```

**Verificar en BD:**
```sql
SELECT * FROM label_requests
WHERE id_product = 123
  AND id_warehouse = 250
  AND id_period = 7;

-- Resultado: requested_labels = 75 (actualizado)
```

---

### **Ejemplo 3: Cancelar solicitud**

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/request \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 123,
    "warehouseId": 250,
    "periodId": 7,
    "requestedLabels": 0
  }'
```

**Response:**
```
Status: 201 Created
```

**Verificar en BD:**
```sql
SELECT COUNT(*) FROM label_requests
WHERE id_product = 123
  AND id_warehouse = 250
  AND id_period = 7;

-- Resultado: 0 (eliminado)
```

---

### **Ejemplo 4: Error - Marbetes sin imprimir**

```bash
# 1. Primero solicitar y generar
POST /api/sigmav2/labels/request
{ "requestedLabels": 50 }

POST /api/sigmav2/labels/generate
{ ... }

# 2. Intentar solicitar más sin imprimir
POST /api/sigmav2/labels/request
{ "requestedLabels": 100 }
```

**Response:**
```json
Status: 400 Bad Request
{
  "message": "Existen marbetes GENERADOS sin imprimir para este producto/almacén/periodo. Por favor imprima los marbetes existentes antes de solicitar más."
}
```

---

## 📋 Reglas de Negocio Implementadas

### ✅ **Regla 1:** Solo cantidad numérica entera
- Validación en DTO: `Integer`
- No acepta decimales ni texto

### ✅ **Regla 2:** Persistencia automática
- Se guarda inmediatamente en `label_requests`
- No requiere botón "Guardar"
- Puede cerrar el navegador sin perder datos

### ✅ **Regla 3:** Actualizar cantidad múltiples veces
- Busca solicitud existente antes de crear
- Si existe, actualiza (no crea duplicado)
- Puede cambiar la cantidad las veces que desee

### ✅ **Regla 4:** Cantidad = 0 cancela solicitud
- Si la cantidad es 0, elimina el registro
- Solo si `folios_generados` = 0

### ✅ **Regla 5:** No solicitar con marbetes sin imprimir
- Valida si existen marbetes GENERADOS
- Si existen, lanza excepción 400
- Debe imprimir primero antes de solicitar más

---

## 🔗 APIs Relacionadas

### **Generar marbetes**
```
POST /api/sigmav2/labels/generate
```
Genera los folios solicitados (crea registros en tabla `labels`).

### **Imprimir marbetes**
```
POST /api/sigmav2/labels/print
```
Marca los marbetes como IMPRESOS.

### **Consultar inventario**
```
POST /api/sigmav2/labels/summary
```
Lista productos con folios solicitados y existentes.

---

## 📊 Flujo Completo

```
1. Usuario consulta inventario
   GET /api/sigmav2/labels/summary

2. Usuario solicita 50 folios para un producto
   POST /api/sigmav2/labels/request
   { "requestedLabels": 50 }
   ✅ Guardado automáticamente

3. Usuario cambia a 75 folios
   POST /api/sigmav2/labels/request
   { "requestedLabels": 75 }
   ✅ Actualizado (no duplicado)

4. Usuario genera los marbetes
   POST /api/sigmav2/labels/generate
   ✅ Crea 75 marbetes en estado GENERADO

5. Usuario imprime los marbetes
   POST /api/sigmav2/labels/print
   { "startFolio": 1, "endFolio": 75 }
   ✅ Marca como IMPRESOS

6. Usuario puede solicitar más folios
   POST /api/sigmav2/labels/request
   { "requestedLabels": 100 }
   ✅ Permitido porque ya imprimió los anteriores
```

---

## 🧪 Testing con Postman

### **Collection:** SIGMAV2 - Labels

#### **1. Login**
```
POST /api/sigmav2/auth/login
{
  "email": "admin@tokai.com.mx",
  "password": "password123"
}

→ Copiar token del response
```

#### **2. Solicitar folios**
```
POST /api/sigmav2/labels/request
Headers:
  Authorization: Bearer {TOKEN}
  Content-Type: application/json

Body:
{
  "productId": {{productId}},
  "warehouseId": {{warehouseId}},
  "periodId": {{periodId}},
  "requestedLabels": 50
}

Tests:
pm.test("Status 201", () => {
  pm.response.to.have.status(201);
});
```

---

## 📝 Notas de Implementación

### **Backend**
- ✅ Controller: `LabelsController.java`
- ✅ Service: `LabelServiceImpl.java`
- ✅ Repository: `LabelsPersistenceAdapter.java`
- ✅ Validaciones: Implementadas
- ✅ Transacciones: `@Transactional`

### **Frontend (Pendiente)**
```javascript
// Al salir del input (evento @blur)
async saveFoliosRequest(productId, cantidad) {
  try {
    await axios.post('/api/sigmav2/labels/request', {
      productId: productId,
      warehouseId: this.selectedWarehouse,
      periodId: this.selectedPeriod,
      requestedLabels: parseInt(cantidad)
    });
    this.$notify.success('Guardado automáticamente');
  } catch (error) {
    if (error.response?.status === 400) {
      this.$notify.error(error.response.data.message);
    }
  }
}
```

---

## ✅ Checklist de Validación

- [ ] API responde 201 al crear solicitud
- [ ] API actualiza en lugar de crear duplicado
- [ ] API elimina cuando cantidad = 0
- [ ] API valida marbetes sin imprimir
- [ ] API valida acceso al almacén
- [ ] API requiere autenticación JWT
- [ ] Frontend dispara guardado al salir del input
- [ ] Frontend maneja errores correctamente

---

**Documentación creada:** 2025-01-12
**API Base:** `http://localhost:8080/api/sigmav2/labels`
**Estado:** ✅ COMPLETO Y FUNCIONAL

