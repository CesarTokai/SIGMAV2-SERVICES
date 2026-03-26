# 📊 APIs para Consultar Historial de Folios por Email

## ✅ APIs EXISTENTES que registran y consultan historial

### 1️⃣ **FOLIO REQUEST HISTORY** (Mi Solicitud de Folios)
Registra **QUIÉN solicitó cuántos folios** (email + folio range + cantidad)

```
GET /folio-request-history/mi-historial
Authorization: Bearer {TOKEN}

Response:
[
  {
    "id": 1,
    "userId": 42,
    "email": "cgonzalez@tokai.com.mx",
    "folioStart": 1,
    "folioEnd": 100,
    "quantity": 100,
    "periodId": 1,
    "warehouseId": 5,
    "fullName": "Cesar Gonzalez",
    "role": "AUXILIAR",
    "createdAt": "2026-03-26T10:30:00"
  }
]
```

---

### 2️⃣ **ADMIN - Ver Historial por Email de Otro Usuario**

```
GET /folio-request-history/usuario/{email}
Authorization: Bearer {TOKEN}
Role: ADMINISTRADOR

Example:
GET /folio-request-history/usuario/cgonzalez@tokai.com.mx

Response:
[
  {
    "id": 1,
    "email": "cgonzalez@tokai.com.mx",
    "folioStart": 1,
    "folioEnd": 100,
    "quantity": 100,
    "periodId": 1,
    "warehouseId": 5,
    "createdAt": "2026-03-26T10:30:00"
  }
]
```

---

### 3️⃣ **QR SCAN AUDIT** (Historial de Escaneos de QR + Conteos)
Registra **QUIÉN escaneó qué folio y cuándo** (email + folio + timestamp)

```
GET /labels/qr-scan-audit/mi-historial
Authorization: Bearer {TOKEN}

Response:
[
  {
    "id": 1,
    "userId": 42,
    "email": "cgonzalez@tokai.com.mx",
    "folio": 42,
    "periodId": 1,
    "warehouseId": 5,
    "countType": "C1",
    "countValue": 100.00,
    "deviceInfo": "iPhone 12",
    "ipAddress": "192.168.1.100",
    "status": "EXITOSO",
    "createdAt": "2026-03-26T12:30:00"
  },
  {
    "id": 2,
    "userId": 42,
    "email": "cgonzalez@tokai.com.mx",
    "folio": 42,
    "periodId": 1,
    "warehouseId": 5,
    "countType": "C2",
    "countValue": 98.00,
    "status": "EXITOSO",
    "createdAt": "2026-03-26T12:35:00"
  }
]
```

---

### 4️⃣ **ADMIN - Resumen de Folios por Usuario + Almacén**

```
GET /api/sigmav2/admin/users/summary/warehouses-folios?page=0&size=20
Authorization: Bearer {TOKEN}
Role: ADMINISTRADOR

Response:
[
  {
    "email": "cgonzalez@tokai.com.mx",
    "fullName": "Cesar Gonzalez",
    "warehouseCode": "ALM005",
    "warehouseName": "Almacén Central",
    "firstFolio": 1,
    "lastFolio": 100,
    "quantity": 100,
    "generatedAt": "2026-03-26T10:30:00"
  },
  {
    "email": "cgonzalez@tokai.com.mx",
    "fullName": "Cesar Gonzalez",
    "warehouseCode": "ALM007",
    "warehouseName": "Almacén Sucursal",
    "firstFolio": 101,
    "lastFolio": 150,
    "quantity": 50,
    "generatedAt": "2026-03-26T14:15:00"
  }
]
```

---

### 5️⃣ **ACTIVITY LOG** (Historial de Actividades General)
Registra **TODAS las acciones** de un usuario (login, logout, cambios, etc.)

```
POST /api/sigmav2/users/admin/activity-log/by-email
Authorization: Bearer {TOKEN}
Role: ADMINISTRADOR

Body:
{
  "email": "cgonzalez@tokai.com.mx",
  "page": 0,
  "size": 20
}

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 42,
      "action": "LOGIN",
      "resource": "AUTH",
      "details": "Usuario autenticado",
      "ipAddress": "192.168.1.100",
      "userAgent": "Chrome/120",
      "createdAt": "2026-03-26T10:00:00"
    },
    {
      "id": 2,
      "userId": 42,
      "action": "GENERATE_BATCH",
      "resource": "LABELS",
      "details": "Se generaron 100 folios",
      "ipAddress": "192.168.1.100",
      "createdAt": "2026-03-26T10:30:00"
    }
  ]
}
```

---

### 6️⃣ **LABEL BY FOLIO** (Información de un Folio Específico)
Consulta quién y cuándo se registró un folio específico

```
GET /api/sigmav2/labels/by-folio/{folio}
Authorization: Bearer {TOKEN}

Example:
GET /api/sigmav2/labels/by-folio/42

Response:
{
  "folio": 42,
  "estado": "IMPRESO",
  "claveProducto": "GM17MEXB8",
  "descripcionProducto": "Producto XYZ",
  "claveAlmacen": "ALM005",
  "nombreAlmacen": "Almacén Central",
  "usuarioId": 42,
  "usuarioEmail": "cgonzalez@tokai.com.mx",
  "createdAt": "2026-03-26T10:30:00",
  "updatedAt": "2026-03-26T14:15:00"
}
```

---

## 📋 TABLA COMPARATIVA

| API | Información | Email | Folio | Timestamp | Uso |
|-----|-------------|-------|-------|-----------|-----|
| **folio-request-history/mi-historial** | Solicitudes de folios | ✅ | ✅ (Range) | ✅ | Ver mis solicitudes |
| **folio-request-history/usuario/{email}** | Solicitudes por usuario | ✅ | ✅ (Range) | ✅ | Admin: Ver solicitudes de otro |
| **qr-scan-audit/mi-historial** | Escaneos QR + Conteos | ✅ | ✅ | ✅ | Ver mis escaneos |
| **admin/users/warehouses-folios** | Resumen folios por usuario | ✅ | ✅ (Range) | ✅ | Admin: Resumen general |
| **activity-log/by-email** | Todas las actividades | ✅ | ❌ | ✅ | Admin: Auditoría completa |
| **labels/by-folio/{folio}** | Info de un folio | ✅ | ✅ | ✅ | Ver detalles de un folio |

---

## 🎯 CASOS DE USO

### **Caso 1: "¿Quién registró el folio 42?"**
```bash
GET /api/sigmav2/labels/by-folio/42
→ Retorna: email del usuario, timestamp, estado
```

### **Caso 2: "¿Qué folios registró cgonzalez@tokai.com.mx?"**
```bash
GET /folio-request-history/usuario/cgonzalez@tokai.com.mx
→ Retorna: todos los rangos de folios que solicitó + timestamps
```

### **Caso 3: "¿Quién escaneó el folio 42 con QR?"**
```bash
GET /labels/qr-scan-audit/mi-historial
→ Retorna: email, folio, timestamp, tipo conteo (C1/C2)
```

### **Caso 4: "Admin quiere ver TODAS las acciones de un usuario"**
```bash
POST /api/sigmav2/users/admin/activity-log/by-email
Body: { "email": "cgonzalez@tokai.com.mx" }
→ Retorna: login, logout, cambios, generaciones, escaneos
```

---

## 🚀 EJEMPLO DE FLUJO COMPLETO

**Escenario:** Quiero ver el historial completo del folio 42 registrado por cgonzalez@tokai.com.mx

### Paso 1: Obtener info del folio
```bash
curl -X GET "http://localhost:8080/api/sigmav2/labels/by-folio/42" \
  -H "Authorization: Bearer TOKEN"

Response:
{
  "folio": 42,
  "usuarioEmail": "cgonzalez@tokai.com.mx",
  "createdAt": "2026-03-26T10:30:00"
}
```

### Paso 2: Ver historial de solicitudes del usuario
```bash
curl -X GET "http://localhost:8080/folio-request-history/usuario/cgonzalez@tokai.com.mx" \
  -H "Authorization: Bearer TOKEN"

Response: Todos los rangos que solicitó (incluyendo el 42)
```

### Paso 3: Ver escaneos de ese folio
```bash
curl -X GET "http://localhost:8080/labels/qr-scan-audit/mi-historial" \
  -H "Authorization: Bearer TOKEN"

Response: Si escaneó el folio 42 con QR, aparecerá aquí
```

---

## 💾 TABLAS DE BASE DE DATOS INVOLUCRADAS

| Tabla | Almacena | Campos Clave |
|-------|----------|--------------|
| `folio_request_history` | Solicitudes de folios | email, folio_start, folio_end, created_at |
| `qr_scan_audit` | Escaneos de QR | user_id, email, folio, count_type, created_at |
| `user_activity_log` | Actividades generales | user_id, email, action, created_at |
| `labels` | Información de folios | folio, user_id, created_at, updated_at |

---

**Conclusión:** ✅ SÍ EXISTEN las APIs para consultar historial de folios por email. Están implementadas y listas para usar.

