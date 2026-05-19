# ✅ APIs REALES QUE EXISTEN - Historial de Folios por Email

## 🔍 Análisis Honesto

Después de revisar el código, **NO TODAS las APIs que documenté existen realmente**. Aquí están las que SÍ funcionan:

---

## ✅ APIs QUE REALMENTE EXISTEN

### 1️⃣ **Historial de Actividades de Usuario (Por Email)**
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
  "userId": 42,
  "email": "cgonzalez@tokai.com.mx",
  "totalActivities": 5,
  "currentPage": 0,
  "totalPages": 1,
  "activities": [
    {
      "logId": 1,
      "actionType": "LOGIN",
      "actionDetails": "...",
      "timestamp": "2026-03-26T10:00:00",
      "ipAddress": "192.168.1.100"
    }
  ]
}
```

✅ **Registra:** Acciones del usuario, IP, timestamp, tipo de acción

---

### 2️⃣ **Info de Usuario por Email**
```
GET /api/sigmav2/users/{email}
Authorization: Bearer {TOKEN}

Example:
GET /api/sigmav2/users/cgonzalez@tokai.com.mx

Response:
{
  "success": true,
  "data": {
    "id": 42,
    "email": "cgonzalez@tokai.com.mx",
    "firstName": "Cesar",
    "lastName": "Gonzalez",
    "role": "AUXILIAR",
    ...
  }
}
```

✅ **Registra:** Email, información del usuario

---

### 3️⃣ **Info de un Folio Específico (Por Folio)**
```
GET /api/sigmav2/labels/by-folio/{folio}
Authorization: Bearer {TOKEN}

Example:
GET /api/sigmav2/labels/by-folio/42

Response:
{
  "success": true,
  "folio": 42,
  "estado": "IMPRESO",
  "claveProducto": "GM17MEXB8",
  "usuarioId": 42,
  "usuarioEmail": "cgonzalez@tokai.com.mx",
  "createdAt": "2026-03-26T10:30:00",
  "updatedAt": "2026-03-26T14:15:00"
}
```

✅ **Registra:** Quién creó el folio, email, timestamp, estado

---

### 4️⃣ **Resumen de Usuarios + Almacenes + Folios (Admin)**
```
GET /api/sigmav2/admin/users/summary/warehouses-folios?page=0&size=20
Authorization: Bearer {TOKEN}
Role: ADMINISTRADOR

Response: (ACTUALMENTE VACÍO - Ver problema abajo)
{
  "data": [],
  "success": true,
  "totalPages": 0,
  "pageSize": 20,
  "totalElements": 0
}
```

⚠️ **PROBLEMA:** Este endpoint EXISTE pero retorna data vacía. Razón: **No hay datos en la tabla de labels que cumplan con los criterios**.

---

## ❌ APIs QUE NO EXISTEN (Que yo documenté pero NO implementé)

Estos endpoints que mencioné **NO están implementados**:

```
❌ GET /folio-request-history/mi-historial
❌ GET /folio-request-history/usuario/{email}
❌ GET /labels/qr-scan-audit/mi-historial
❌ POST /api/sigmav2/users/admin/activity-log/by-email (EXISTE pero es POST no GET)
```

---

## 🎯 Las APIs REALES que FUNCIONAN para consultar historial:

| API | Método | Endpoint | Email | Folio | Timestamp | Status |
|-----|--------|----------|-------|-------|-----------|--------|
| Actividades | POST | `/users/admin/activity-log/by-email` | ✅ | ❌ | ✅ | ✅ |
| Info Usuario | GET | `/users/{email}` | ✅ | ❌ | ❌ | ✅ |
| Info Folio | GET | `/labels/by-folio/{folio}` | ✅ | ✅ | ✅ | ✅ |
| Resumen Admin | GET | `/admin/users/summary/warehouses-folios` | ✅ | ✅ | ✅ | VACÍO |

---

## 🔧 Por qué `/admin/users/summary/warehouses-folios` retorna VACÍO

El endpoint existe pero la lógica **busca datos en una tabla o vista que no tiene registros**.

**Posibles razones:**
1. No hay labels generados
2. No hay relación entre labels y usuarios
3. La query está buscando en la tabla equivocada

---

## 💡 SOLUCIÓN: Lo que REALMENTE necesitas

Si quieres un **historial completo de quién registró qué folio**:

### **Usa estas 2 APIs juntas:**

**Paso 1:** Obtener info del folio
```bash
GET /api/sigmav2/labels/by-folio/42
→ Retorna: email de quien lo creó, timestamp
```

**Paso 2:** Ver todas las actividades de ese usuario
```bash
POST /api/sigmav2/users/admin/activity-log/by-email
Body: { "email": "cgonzalez@tokai.com.mx" }
→ Retorna: todas las acciones que hizo (incluyendo generación de folio)
```

---

## ✨ Resumen Honesto

| Lo que dijiste | Realidad |
|---|---|
| "Hay 6 APIs" | Hay 3 APIs funcionales + 1 con datos vacíos |
| "GET folio-request-history/mi-historial" | ❌ NO EXISTE |
| "GET qr-scan-audit/mi-historial" | ❌ NO EXISTE |
| "admin/users/warehouses-folios" | ✅ EXISTE pero VACÍO |
| "Consultar por email" | ✅ SÍ FUNCIONA con 2 APIs diferentes |

---

**Mi error:** Documenté APIs teóricas sin verificar que estuvieran realmente implementadas.

**Próximo paso:** ¿Quieres que implemente las APIs que faltan?

