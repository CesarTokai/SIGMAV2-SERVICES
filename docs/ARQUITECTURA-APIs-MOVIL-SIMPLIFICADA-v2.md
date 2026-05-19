# 🔌 ARQUITECTURA APIS MÓVIL - VERSIÓN SIMPLIFICADA

**Fecha:** 24 de Marzo 2026  
**Revisión:** v2 (Optimizada, sin redundancias)  
**Basis:** Feedback del usuario - Eliminar endpoints duplicados

---

## 📋 PROBLEMA CON v1

❌ **Endpoint 2.1 (validar QR)** era **REDUNDANTE**
- Endpoints 1.6/1.7 ya retornan TODO: estado, C1, C2, cantidad teórica
- No necesitaba un paso intermedio de validación

❌ **Endpoint 2.5 (lista pendientes)** NO es crítico
- Dashboard secundario, puede agregarse después
- El flujo principal es: Escanear → Registrar → Siguiente

---

## ✅ ARQUITECTURA v2: SIMPLIFICADA (4 ENDPOINTS)

### FLUJO MÓVIL FINAL:

```
1. POST /api/sigmav2/auth/login                        ✅ REUTILIZADO
2. GET  /api/sigmav2/warehouses                        ✅ REUTILIZADO
3. GET  /api/sigmav2/periods/active                    ✅ REUTILIZADO
4. GET  /api/sigmav2/labels/by-folio/{folio}           ✅ REUTILIZADO (CLAVE)
5. POST /api/sigmav2/labels/scan/count                 🆕 NUEVO (metadata dispositivo)
6. [Volver a paso 4 para siguiente marbete]
```

**Total:** 3 existentes + 1 nuevo = 4 endpoints. **LIMPIO.**

---

## 🔐 ENDPOINTS DESGLOSADOS

### FASE 1: AUTENTICACIÓN

#### 1️⃣ LOGIN
```http
POST /api/sigmav2/auth/login
Content-Type: application/json

{
  "email": "juan@tokai.mx",
  "password": "contraseña123"
}

Response 200 OK:
{
  "token": "eyJhbGciOiJIUzI1NiI...",
  "user": {
    "id": 42,
    "email": "juan@tokai.mx",
    "name": "Juan Pérez",
    "role": "AUXILIAR_DE_CONTEO"
  },
  "expiresIn": 86400
}
```

---

### FASE 2: CONFIGURACIÓN INICIAL

#### 2️⃣ OBTENER ALMACENES ASIGNADOS
```http
GET /api/sigmav2/warehouses
Authorization: Bearer {token}

Response 200 OK:
[
  {
    "id": 369,
    "nombre": "ALM_01",
    "descripcion": "Almacén Principal"
  },
  {
    "id": 370,
    "nombre": "ALM_02"
  }
]

Flutter: Mostrar dropdown. Usuario selecciona ALM_01
```

#### 3️⃣ OBTENER PERÍODO ACTIVO
```http
GET /api/sigmav2/periods/active
Authorization: Bearer {token}

Response 200 OK:
{
  "id": 16,
  "nombre": "Diciembre 2025",
  "estado": "ACTIVO"
}

Flutter: Confirmar que existe período activo
```

---

### FASE 3: CONTEO (LOOP)

```
[SCANNER ABIERTO]
    ↓
[Usuario escanea QR o ingresa folio manual: "42"]
    ↓
GET /api/sigmav2/labels/by-folio/42 ← ⭐ CLAVE
    ↓
[Backend retorna TODO lo que necesita Flutter]
    ↓
[Flutter muestra: producto, teórico, C1?, C2?]
    ↓
[Usuario ingresa cantidad: 95]
    ↓
POST /api/sigmav2/labels/scan/count ← NUEVO
    ↓
[Backend registra + metadata dispositivo]
    ↓
[Flutter: "✓ Registrado"]
    ↓
[¿Siguiente? → Volver a Scanner]
```

---

## 🎯 ENDPOINT EXISTENTE (REUTILIZADO COMO CLAVE)

### GET /api/sigmav2/labels/by-folio/{folio}

**Ubicación en backend:** `LabelController.java` (ya existe)

**Uso actual:** Desktop (reportes)

**Uso NUEVO:** Móvil (después de escanear QR)

**Response que necesitamos que incluya:**

```http
GET /api/sigmav2/labels/by-folio/42?warehouseId=369&periodId=16
Authorization: Bearer {token}

Response 200 OK:
{
  "folio": 42,
  "productId": 123,
  "productName": "Laptop Dell Inspiron 15",
  "warehouseId": 369,
  "periodId": 16,
  "estado": "IMPRESO",
  "impresoAt": "2026-03-20T10:00:00Z",
  
  // ⭐ AGREGAR ESTOS CAMPOS (si no los tiene):
  "theoreticalQuantity": 100,
  "c1": {
    "registered": true,
    "quantity": 95,
    "registeredAt": "2026-03-23T14:35:22Z",
    "registeredBy": "Juan Pérez"
  },
  "c2": {
    "registered": false,
    "quantity": null,
    "registeredAt": null,
    "registeredBy": null
  },
  "variance": null,
  "readyForC2": true
}
```

**❌ Problema:** ¿El endpoint actual retorna C1/C2 info?
- Si SÍ: Usar tal cual
- Si NO: **Necesitamos actualizar este endpoint** para que incluya esos campos

---

## 🆕 NUEVO ENDPOINT (SOLO ESTE)

### POST /api/sigmav2/labels/scan/count

**Propósito:** Registrar C1/C2 con metadata del dispositivo (auditoría móvil)

**Request:**
```json
{
  "folio": 42,
  "countType": "C1",           // C1 o C2
  "quantity": 95,
  "warehouseId": 369,
  "periodId": 16,
  "deviceId": "device-uuid",   // Identificador único del móvil
  "scanTimestamp": "2026-03-23T14:35:22Z"
}
```

**Response:**
```json
{
  "success": true,
  "folio": 42,
  "countType": "C1",
  "quantity": 95,
  "registeredAt": "2026-03-23T14:35:23Z",
  "variance": -5,
  "message": "✓ C1 registrado"
}
```

**Diferencia vs /labels/counts/c1 existente:**
- ✅ Recibe `deviceId` + `scanTimestamp` (auditoría móvil)
- ✅ Retorna varianza
- ✅ @Auditable automático

---

## 📱 FLUJO EN FLUTTER

```
┌─────────────────────────────────────────┐
│ LOGIN                                   │
├─────────────────────────────────────────┤
│ [Email] [Contraseña]                   │
│ [Ingresar]                             │
└────────────────┬────────────────────────┘
                 │
      POST /auth/login → Token
                 │
┌────────────────▼────────────────────────┐
│ HOME SCREEN                             │
├─────────────────────────────────────────┤
│ ¡Hola, Juan!                            │
│ Almacén: [ALM_01 ▼]  ← GET /warehouses │
│ Período: Dic 2025    ← GET /periods    │
│ [📷 Iniciar Conteo]                    │
└────────────────┬────────────────────────┘
                 │
┌────────────────▼────────────────────────┐
│ SCANNER                                 │
├─────────────────────────────────────────┤
│ [Cámara activa...]                     │
│ QR Detectado: "42"                     │
│ [✓ Aceptar]                            │
└────────────────┬────────────────────────┘
                 │
  GET /labels/by-folio/42?warehouseId=369&periodId=16
  ↓ Retorna TODO: producto, C1, C2, teórico, estado
                 │
┌────────────────▼────────────────────────┐
│ CONFIRMACIÓN MARBETE                    │
├─────────────────────────────────────────┤
│ ✓ Marbete encontrado                   │
│                                         │
│ 📦 Laptop Dell                          │
│ Teórico: 100 unidades                   │
│ C1: ✓ Registrado (95)                   │
│ C2: ⏳ Pendiente                        │
│                                         │
│ ¿Registrar C2?  [Sí]  [Cancelar]       │
└────────────────┬────────────────────────┘
                 │ [Sí]
┌────────────────▼────────────────────────┐
│ INGRESO DE CANTIDAD                     │
├─────────────────────────────────────────┤
│ Conteo C2:                              │
│ [Cantidad: ________________] 95         │
│ [✓ GUARDAR] [✗ CANCELAR]               │
└────────────────┬────────────────────────┘
                 │ [GUARDAR]
   POST /labels/scan/count (deviceId, scanTimestamp)
   ↓ Backend registra + auditoría móvil
                 │
┌────────────────▼────────────────────────┐
│ ÉXITO                                   │
├─────────────────────────────────────────┤
│ ✅ C2 Registrado: 95 unidades          │
│ Varianza: -5 (teórico: 100)            │
│ [🔄 Siguiente Marbete]                  │
└────────────────┬────────────────────────┘
                 │ [Siguiente]
                 └──> [Volver a SCANNER]
```

---

## 🔧 VERIFICACIÓN PRE-IMPLEMENTACIÓN

### CHECKLIST: ¿Qué existe en backend?

- [ ] **POST /auth/login** → ✅ Existe (Security module)
- [ ] **GET /warehouses** → ✅ Existe (Warehouse module)
- [ ] **GET /periods/active** → ✅ Existe (Periods module)
- [ ] **GET /labels/by-folio/{folio}** → ✅ Existe (Labels module)
  - [ ] ¿Incluye `c1`, `c2`, `theoreticalQuantity`?
    - Si NO → Actualizar response DTO
    - Si SÍ → Usar tal cual
- [ ] **POST /labels/scan/count** → ❌ NO existe → **CREAR**

---

## 📝 LO QUE NECESITAMOS IMPLEMENTAR

### SOLO CREAR 1 NUEVO ENDPOINT

```java
@PostMapping("/scan/count")
@Auditable(action = "REGISTER_MOBILE_COUNT", resource = "LABEL")
public ResponseEntity<MobileCountResponse> registerMobileCount(
    @Valid @RequestBody MobileCountRequest request
) {
    // 1. Validar
    // 2. Registrar C1/C2 (reutilizar lógica existente)
    // 3. Guardar metadata: device_id, scan_timestamp
    // 4. Retornar con varianza
}
```

### POSIBLE ACTUALIZACIÓN DE DTO EXISTENTE

Si `GET /labels/by-folio/{folio}` no retorna C1/C2 info:

```java
// LabelResponse.java (actualizar)
private CountDetails c1;      // NEW
private CountDetails c2;      // NEW
private Integer theoreticalQuantity;  // NEW
private Integer variance;     // NEW
private Boolean readyForC2;   // NEW
```

---

## 📊 COMPARATIVA: v1 vs v2

| Aspecto | v1 | v2 |
|---------|----|----|
| Endpoints nuevos | 5 | 1 |
| Endpoints reutilizados | 3 | 3 |
| Redundancia | SÍ (validar + status) | NO |
| Complejidad frontend | Alta | Baja |
| Flujo móvil | Confuso (6 steps) | Claro (4 steps) |

---

## 🎯 PLAN DE ACCIÓN

### PASO 1: VERIFICAR (Ahora mismo)
- [ ] Revisar `GET /labels/by-folio/{folio}` response actual
- [ ] ¿Incluye C1, C2, theoreticalQuantity?

### PASO 2: ACTUALIZAR (Si necesario)
- [ ] Si faltan campos → Actualizar DTO + @Query para fetchear C1/C2

### PASO 3: CREAR NUEVO ENDPOINT
- [ ] Controller: `LabelScanMobileController.java`
- [ ] Método: `registerMobileCount()`
- [ ] Request DTO: `MobileCountRequest`
- [ ] Response DTO: `MobileCountResponse`

### PASO 4: AGREGAR AUDITORÍA MÓVIL
- [ ] Tabla: `label_count_events`
  - Columnas: `device_id`, `scan_timestamp`
- [ ] Migration SQL: `V1_2_X__Add_mobile_audit_fields.sql`

### PASO 5: DOCUMENTAR CONSUMO EN FLUTTER
- [ ] Ejemplos de código Flutter
- [ ] Manejo de errores
- [ ] Almacenamiento de token

---

## ✅ VENTAJAS DE v2

1. **Menos endpoints** → Menos código, menos bugs
2. **Sin redundancia** → Un solo punto de verdad (by-folio)
3. **Auditoría clara** → Solo el scan/count nuevo lleva device_id
4. **Fácil de mantener** → Cambios centralizados
5. **Flujo simple** → 4 pasos claros, no confusos

---

**Siguiente paso:** Verificar estado actual de `GET /labels/by-folio/{folio}` en código


