# 🔌 ARQUITECTURA DE APIs: Reutilización + Nuevos Endpoints para QR Mobile

**Fecha:** 23 de Marzo 2026  
**Sistema:** SIGMAV2 v1.0 + Módulo QR/Scanner Móvil  
**Nivel:** Técnico (Arquitecto Backend)

---

## 📋 ÍNDICE

1. [APIs Existentes a Reutilizar](#1-apis-existentes-a-reutilizar)
2. [Nuevos Endpoints para Móvil](#2-nuevos-endpoints-para-móvil)
3. [Dinámica Completa del Flujo](#3-dinámica-completa-del-flujo)
4. [Diagrama de Interacción](#4-diagrama-de-interacción)
5. [Implementación Técnica](#5-implementación-técnica)
6. [DTOs y Validaciones](#6-dtos-y-validaciones)

---

## 1. APIs EXISTENTES A REUTILIZAR

### 1.1 AUTENTICACIÓN (Security Module)

**Endpoint:** `POST /api/sigmav2/auth/login`

```http
POST /api/sigmav2/auth/login
Content-Type: application/json

{
  "email": "juan@tokai.mx",
  "password": "contraseña123"
}

HTTP/1.1 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 42,
    "email": "juan@tokai.mx",
    "name": "Juan Pérez",
    "role": "AUXILIAR_DE_CONTEO"
  },
  "expiresIn": 86400
}
```

**Reutilización en Móvil:**
- ✅ Flutter: Llamar mismo endpoint
- ✅ Almacenar token en Keychain (iOS) / Keystore (Android)
- ✅ Usar token en header `Authorization: Bearer {token}`

---

### 1.2 OBTENER ALMACENES ASIGNADOS

**Endpoint:** `GET /api/sigmav2/warehouses` (con filtro por usuario)

**O servicio interno:** `WarehouseServiceImpl.findActiveWarehousesByUserId(userId)`

```http
GET /api/sigmav2/warehouses
Authorization: Bearer {token}

HTTP/1.1 200 OK
[
  {
    "id": 369,
    "nombre": "ALM_01",
    "descripcion": "Almacén Principal",
    "ubicacion": "Planta 1"
  },
  {
    "id": 370,
    "nombre": "ALM_02",
    "descripcion": "Almacén Secundario",
    "ubicacion": "Planta 2"
  }
]
```

**Reutilización en Móvil:**
- ✅ Al login: obtener almacenes asignados
- ✅ Dropdown para seleccionar dónde contar

---

### 1.3 OBTENER PERÍODO ACTIVO

**Endpoint:** `GET /api/sigmav2/periods/active` (si existe)

**O alternativa:** Consultar desde modelo Period

```http
GET /api/sigmav2/periods/active
Authorization: Bearer {token}

HTTP/1.1 200 OK
{
  "id": 16,
  "nombre": "Diciembre 2025",
  "fechaInicio": "2025-12-01",
  "fechaFin": "2025-12-31",
  "estado": "ACTIVO"
}
```

**Reutilización en Móvil:**
- ✅ Validar que existe período activo antes de conteos
- ✅ Usar periodId en conteos

---

### 1.4 REGISTRAR CONTEO C1 (Existente)

**Endpoint:** `POST /api/sigmav2/labels/counts/c1`

```http
POST /api/sigmav2/labels/counts/c1
Authorization: Bearer {token}
Content-Type: application/json

{
  "folio": 42,
  "quantity": 95,
  "periodId": 16,
  "warehouseId": 369
}

HTTP/1.1 200 OK
{
  "id": 1001,
  "folio": 42,
  "oneCount": 95,
  "oneCountAt": "2026-03-23T14:35:22Z",
  "estado": "ACTIVO"
}
```

**Reutilización en Móvil:**
- ✅ Llamar directamente para registrar C1
- ✅ YA EXISTE → No duplicar lógica

---

### 1.5 REGISTRAR CONTEO C2 (Existente)

**Endpoint:** `POST /api/sigmav2/labels/counts/c2`

```http
POST /api/sigmav2/labels/counts/c2
Authorization: Bearer {token}
Content-Type: application/json

{
  "folio": 42,
  "quantity": 95,
  "periodId": 16,
  "warehouseId": 369
}

HTTP/1.1 200 OK
{
  "id": 1001,
  "folio": 42,
  "secondCount": 95,
  "secondCountAt": "2026-03-23T14:35:23Z",
  "estado": "ACTIVO"
}
```

**Reutilización en Móvil:**
- ✅ Llamar directamente para registrar C2
- ✅ YA EXISTE → No duplicar lógica

---

### 1.6 OBTENER ESTADO DE MARBETE

**Endpoint:** `GET /api/sigmav2/labels/by-folio/{folio}` (Existente)

```http
GET /api/sigmav2/labels/by-folio/42
Authorization: Bearer {token}

HTTP/1.1 200 OK
{
  "folio": 42,
  "productId": 123,
  "productName": "Laptop Dell Inspiron 15",
  "warehouseId": 369,
  "periodId": 16,
  "estado": "IMPRESO",
  "impresoAt": "2026-03-20T10:00:00Z"
}
```

**Reutilización en Móvil:**
- ✅ Obtener datos del marbete después de escanear QR
- ✅ Mostrar info al usuario

---

### 1.7 OBTENER MARBETE PARA CONTEO (Existente)

**Endpoint:** `GET /api/sigmav2/labels/for-count?folio=42&periodId=16&warehouseId=369`

**O POST:**  `POST /api/sigmav2/labels/for-count`

```http
POST /api/sigmav2/labels/for-count
Authorization: Bearer {token}
Content-Type: application/json

{
  "folio": 42,
  "periodId": 16,
  "warehouseId": 369
}

HTTP/1.1 200 OK
{
  "folio": 42,
  "productId": 123,
  "productName": "Laptop Dell Inspiron 15",
  "theoreticalQuantity": 100,
  "c1Registered": false,
  "c2Registered": false,
  "estado": "IMPRESO",
  "variance": null
}
```

**Reutilización en Móvil:**
- ✅ Validar marbete antes de permitir conteo
- ✅ Mostrar info: "¿C1 registrado?", "¿C2 registrado?"

---

## 2. NUEVOS ENDPOINTS PARA MÓVIL

### 2.1 VALIDAR QR/FOLIO PARA CONTEO (NUEVO)

**Endpoint:** `POST /api/sigmav2/labels/scan/validate`

**Propósito:** Validación rápida desde Flutter antes de pedir cantidad

```http
POST /api/sigmav2/labels/scan/validate
Authorization: Bearer {token}
Content-Type: application/json

{
  "qrCode": "42",                    # Puede ser solo número o "SIGMAV2-FOLIO-42-P16-W369"
  "countType": "C1",                 # C1 o C2
  "warehouseId": 369,
  "periodId": 16
}

HTTP/1.1 200 OK
{
  "valid": true,
  "folio": 42,
  "qrCode": "SIGMAV2-FOLIO-42-P16-W369",
  "productName": "Laptop Dell Inspiron 15",
  "theoreticalQuantity": 100,
  "c1": {
    "registered": false,
    "quantity": null,
    "registeredAt": null
  },
  "c2": {
    "registered": false,
    "quantity": null,
    "registeredAt": null
  },
  "estado": "IMPRESO",
  "message": "✓ Marbete válido. Listo para registrar C1"
}
```

**Error Response:**

```json
{
  "valid": false,
  "message": "❌ C1 ya está registrado: 95 unidades",
  "error": "ALREADY_COUNTED_C1",
  "folio": 42
}
```

**Lógica Backend:**
```java
1. Parsear QR (si es "SIGMAV2-FOLIO-42-P16-W369" → extraer folio 42)
2. Buscar Label por folio
3. Validar: período activo, estado IMPRESO, almacén correcto
4. Validar según countType:
   - C1: ¿C1 no está registrado?
   - C2: ¿C1 está registrado pero C2 no?
5. Retornar estado actual del marbete
```

---

### 2.2 REGISTRAR CONTEO DESDE MÓVIL (NUEVO)

**Endpoint:** `POST /api/sigmav2/labels/scan/count`

**Propósito:** Registrar C1/C2 con metadata del dispositivo

```http
POST /api/sigmav2/labels/scan/count
Authorization: Bearer {token}
Content-Type: application/json

{
  "folio": 42,
  "countType": "C1",
  "quantity": 95,
  "warehouseId": 369,
  "periodId": 16,
  "deviceId": "UUID-MOB-DEVICE-001",      # ID único del dispositivo
  "scanTimestamp": "2026-03-23T14:35:22Z" # Timestamp del escaneo
}

HTTP/1.1 201 CREATED
{
  "success": true,
  "folio": 42,
  "qrCode": "SIGMAV2-FOLIO-42-P16-W369",
  "countType": "C1",
  "quantity": 95,
  "registeredAt": "2026-03-23T14:35:23Z",
  "message": "✓ Conteo C1 registrado exitosamente",
  "variance": -5
}
```

**Error Response:**

```json
{
  "success": false,
  "folio": 42,
  "error": "VALIDATION_FAILED",
  "message": "C1 ya existe: 95 unidades",
  "timestamp": "2026-03-23T14:35:24Z"
}
```

**Diferencia con `/labels/counts/c1`:**
- ✅ Recibe `deviceId` y `scanTimestamp` (auditoría móvil)
- ✅ Hace validaciones de QR antes
- ✅ Retorna info de varianza

**Lógica Backend:**
```java
@Transactional
public CountResponse registerMobileCount(CountRequest req, Long userId) {
  1. Validar QR/folio (usar endpoint anterior lógica)
  2. Validar cantidad (0 < qty <= 999,999)
  3. Buscar o crear LabelCount
  4. Registrar C1 o C2
  5. Crear evento en label_count_events con device_id + scan_timestamp
  6. Ejecutar @Auditable automáticamente
  7. Retornar confirmación + varianza
}
```

---

### 2.3 OBTENER ESTADO DE MARBETE (OPTIMIZADO PARA MÓVIL) (NUEVO)

**Endpoint:** `GET /api/sigmav2/labels/scan/status/{qrCode}`

**Propósito:** Información del marbete optimizada para display en móvil

```http
GET /api/sigmav2/labels/scan/status/42?warehouseId=369&periodId=16
Authorization: Bearer {token}

HTTP/1.1 200 OK
{
  "folio": 42,
  "qrCode": "SIGMAV2-FOLIO-42-P16-W369",
  "productName": "Laptop Dell Inspiron 15",
  "estado": "IMPRESO",
  "theoretical": 100,
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
  "variance": null,                          # null si no hay C2 aún
  "readyForC2": true,                        # ¿Puede registrar C2?
  "message": "✓ C1 registrado. Listo para C2"
}
```

**Reutilización:**
- ✅ Dashboard en móvil: mostrar progreso de conteos
- ✅ Validar antes de registrar C2

---

### 2.4 BUSCAR MARBETE POR FOLIO MANUAL (NUEVO)

**Endpoint:** `GET /api/sigmav2/labels/scan/folio/{folioNumber}`

**Propósito:** Fallback cuando QR no se escanea bien

```http
GET /api/sigmav2/labels/scan/folio/42?warehouseId=369&countType=C1
Authorization: Bearer {token}

HTTP/1.1 200 OK
{
  "valid": true,
  "folio": 42,
  "qrCode": "SIGMAV2-FOLIO-42-P16-W369",
  "productName": "Laptop Dell Inspiron 15",
  "estado": "IMPRESO",
  "c1Registered": false,
  "c2Registered": false,
  "message": "✓ Folio encontrado. Listo para registrar C1"
}
```

**Uso:**
- ✅ Si escaneo QR falla → usuario ingresa folio manualmente
- ✅ Same validations como `/scan/validate`

---

### 2.5 OBTENER LISTA DE MARBETES PENDIENTES (OPTIMIZADO) (NUEVO)

**Endpoint:** `GET /api/sigmav2/labels/scan/pending?warehouseId=369&periodId=16&countType=C1`

**Propósito:** Dashboard de marbetes sin conteo

```http
GET /api/sigmav2/labels/scan/pending?warehouseId=369&periodId=16&countType=C1&limit=20
Authorization: Bearer {token}

HTTP/1.1 200 OK
{
  "total": 150,
  "pending": 45,
  "completed": 105,
  "labels": [
    {
      "folio": 1,
      "qrCode": "SIGMAV2-FOLIO-1-P16-W369",
      "productName": "Mouse Logitech",
      "theoretical": 500,
      "c1": null,
      "c2": null,
      "lastScannedAt": null
    },
    {
      "folio": 2,
      "qrCode": "SIGMAV2-FOLIO-2-P16-W369",
      "productName": "Teclado HP",
      "theoretical": 300,
      "c1": 298,
      "c2": null,
      "lastScannedAt": "2026-03-23T14:00:00Z"
    }
  ]
}
```

**Uso:**
- ✅ Ver progreso de conteos en móvil
- ✅ Seleccionar siguiente marbete a contar

---

## 3. DINÁMICA COMPLETA DEL FLUJO

### Fase 1: Login en Móvil

```
┌─────────────────────────────────────────────────┐
│ 📱 FLUTTER APP                                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  [LOGIN SCREEN]                                │
│  Email:        [juan@tokai.mx]                 │
│  Contraseña:   [••••••••]                      │
│                                                 │
│  [INGRESAR]                                     │
│       │                                         │
│       ▼                                         │
└─────────────────────────────────────────────────┘
           │
           │ POST /api/sigmav2/auth/login
           │
           ▼
┌─────────────────────────────────────────────────┐
│ 🔐 BACKEND                                      │
├─────────────────────────────────────────────────┤
│                                                 │
│  AuthController.login(email, password)         │
│  ├─> Validar credenciales                     │
│  ├─> Generar JWT Token (30 min expiry)        │
│  └─> Retornar token + user data                │
│                                                 │
│  Response: {                                    │
│    token: "eyJhbGc...",                        │
│    user: { id: 42, name: "Juan", role: "..." }│
│  }                                              │
│                                                 │
└─────────────────────────────────────────────────┘
           │
           │
           ▼
┌─────────────────────────────────────────────────┐
│ 📱 FLUTTER - HOME SCREEN                       │
├─────────────────────────────────────────────────┤
│                                                 │
│  ¡Hola, Juan!                                  │
│                                                 │
│  Almacén:  [ALM_01 ▼] (Get /warehouses)       │
│  Período:  Diciembre 2025 (Get /periods)      │
│                                                 │
│  Marbetes sin C1: 150                          │
│  Marbetes sin C2: 45                           │
│                                                 │
│  [📷 Escanear]   [🔢 Ingreso Manual]           │
│  [📊 Ver Reporte] [⚙️ Sincronizar]             │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

### Fase 2: Escanear QR

```
┌─────────────────────────────────────────────────┐
│ 📱 FLUTTER - SCANNER                           │
├─────────────────────────────────────────────────┤
│                                                 │
│  [Cámara Activa...]                            │
│                                                 │
│  ┌──────────────────────────┐                 │
│  │   📷 QR DETECTADO        │                 │
│  │   Folio: 42              │                 │
│  │   ┌────────────────┐     │                 │
│  │   │ SIGMAV2-FOLIO- │     │                 │
│  │   │ 42-P16-W369    │     │                 │
│  │   └────────────────┘     │                 │
│  └──────────────────────────┘                 │
│                                                 │
│  [Aceptar] [Escanear Otro]                     │
│                                                 │
└─────────────────────────────────────────────────┘
           │ (Aceptar)
           │
           ▼ POST /api/sigmav2/labels/scan/validate
┌─────────────────────────────────────────────────┐
│ 🔐 BACKEND                                      │
├─────────────────────────────────────────────────┤
│                                                 │
│  LabelScanController.validateLabel()           │
│  ├─> Parsear QR: "SIGMAV2-FOLIO-42-P16-W369"│
│  ├─> Extraer folio: 42                        │
│  ├─> Buscar Label en BD                       │
│  ├─> Validar: período activo, estado impreso  │
│  ├─> Validar: C1 no registrado (countType)    │
│  └─> Retornar estado del marbete              │
│                                                 │
│  Response: {                                    │
│    valid: true,                                │
│    folio: 42,                                 │
│    productName: "Laptop Dell",                 │
│    theoretical: 100,                          │
│    c1: { registered: false },                 │
│    message: "✓ Válido para C1"                │
│  }                                              │
│                                                 │
└─────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────┐
│ 📱 FLUTTER - VALIDACIÓN OK                     │
├─────────────────────────────────────────────────┤
│                                                 │
│  ✓ MARBETE VÁLIDO                             │
│                                                 │
│  📦 Laptop Dell Inspiron 15                    │
│  Almacén: ALM_01                               │
│  Teórico: 100 unidades                         │
│                                                 │
│  C1: Pendiente ⏳                              │
│  C2: No disponible (requiere C1)              │
│                                                 │
│  [Continuar]                                   │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

### Fase 3: Registrar Cantidad

```
┌─────────────────────────────────────────────────┐
│ 📱 FLUTTER - INGRESO DE CANTIDAD               │
├─────────────────────────────────────────────────┤
│                                                 │
│  Conteo: ⚫ C1 (Primer Conteo)                 │
│                                                 │
│  Cantidad Contada:                             │
│  [____________________________] 95             │
│                                                 │
│  [✓ GUARDAR]  [✗ CANCELAR]                    │
│                                                 │
└─────────────────────────────────────────────────┘
           │ (GUARDAR)
           │
           ▼ POST /api/sigmav2/labels/scan/count
┌─────────────────────────────────────────────────┐
│ 🔐 BACKEND                                      │
├─────────────────────────────────────────────────┤
│  @Transactional                                 │
│  LabelScanController.registerCount()           │
│                                                 │
│  1. Validar cantidad: 0 < 95 <= 999,999       │
│  2. Re-validar marbete (período, estado)      │
│  3. Buscar/crear LabelCount                    │
│  4. Registrar C1:                              │
│     ├─> oneCount = 95                          │
│     ├─> oneCountAt = NOW()                     │
│     └─> oneCountBy = userId                    │
│  5. Crear evento en label_count_events:        │
│     ├─> folio = 42                             │
│     ├─> count_number = 1                       │
│     ├─> quantity = 95                          │
│     ├─> device_id = "UUID-MOB-001"             │
│     ├─> scan_timestamp = "2026-03-23T14:35Z"   │
│     └─> user_id = 42                           │
│  6. @Auditable AOP:                            │
│     └─> Insertar audit_logs                    │
│  7. COMMIT transacción                         │
│                                                 │
│  Response: {                                    │
│    success: true,                              │
│    folio: 42,                                 │
│    quantity: 95,                              │
│    variance: -5,                              │
│    message: "✓ C1 registrado"                 │
│  }                                              │
│                                                 │
└─────────────────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────┐
│ 📱 FLUTTER - CONFIRMACIÓN                       │
├─────────────────────────────────────────────────┤
│                                                 │
│  ✅ ÉXITO                                       │
│                                                 │
│  Folio: #42                                    │
│  Producto: Laptop Dell                         │
│  C1 Registrado: 95 unidades                    │
│  Varianza: -5 unidades (teórico: 100)         │
│  Registrado: 14:35:22                          │
│                                                 │
│  📊 Progreso:                                  │
│  C1: ✓ Completado                             │
│  C2: ⏳ Pendiente (registra C2)               │
│                                                 │
│  [🔄 Siguiente Marbete]  [📋 Ver Reporte]     │
│                                                 │
└─────────────────────────────────────────────────┘
           │ (Siguiente)
           │
           ▼ [VOLVER A SCANNER]
```

---

### Fase 4: Segundo Conteo (C2)

```
Similar a Fase 2-3, pero:
• countType = "C2"
• Validación: C1 debe estar registrado
• secondCount se actualiza en LabelCount
• Genera reporte de varianza

Response:
{
  success: true,
  folio: 42,
  quantity: 95,
  variance: -5,  # (C2: 95) vs (Teórico: 100)
  message: "✓ C2 registrado. Varianza: -5 (95%)"
}
```

---

## 4. DIAGRAMA DE INTERACCIÓN

```
┌─────────────────┐                    ┌─────────────────┐
│   FLUTTER APP   │                    │   BACKEND REST  │
│                 │                    │                 │
│  • QR Scanner   │                    │  • Controllers  │
│  • LocalStorage │                    │  • Services     │
│  • HTTP Client  │                    │  • BD           │
│                 │                    │                 │
└────────┬────────┘                    └────────┬────────┘
         │                                      │
         │  1. POST /auth/login                 │
         │─────────────────────────────────────►│
         │                                      │
         │◄─────────────────────── JWT Token    │
         │  (store in Keychain)                 │
         │                                      │
         │  2. GET /warehouses                  │
         │─────────────────────────────────────►│
         │  (with Authorization header)         │
         │                                      │
         │◄─────────────────────── List[Wh]    │
         │  (dropdown en móvil)                 │
         │                                      │
         │  [USER SELECTS WAREHOUSE]            │
         │                                      │
         │  3. GET /periods/active              │
         │─────────────────────────────────────►│
         │                                      │
         │◄─────────────────────── Period      │
         │                                      │
         │  [USER OPENS SCANNER]                │
         │                                      │
         │  4. Camera → Reads QR: "42"          │
         │                                      │
         │  5. POST /labels/scan/validate       │
         │     { qrCode: "42", countType:"C1" }│
         │─────────────────────────────────────►│
         │                                      │ parse QR
         │                                      │ find Label
         │                                      │ validate
         │                                      │
         │◄─────────┬─ LabelValidationResponse  │
         │          │ (valid: true, state, c1)│
         │          │                          │
         │  [SHOW PRODUCT INFO]                 │
         │                                      │
         │  [USER INPUTS QUANTITY: 95]          │
         │                                      │
         │  6. POST /labels/scan/count          │
         │     { folio: 42, qty: 95, ...}       │
         │─────────────────────────────────────►│
         │                                      │ @Transactional
         │                                      │ validate qty
         │                                      │ register C1
         │                                      │ create event
         │                                      │ @Auditable
         │                                      │
         │◄─────────────────────── CountResult  │
         │  (success: true, variance: -5)      │
         │                                      │
         │  [SHOW CONFIRMATION]                 │
         │                                      │
         │  7. GET /labels/scan/status/42       │
         │─────────────────────────────────────►│
         │  (for dashboard / next action)       │
         │                                      │
         │◄─────────────────────── LabelStatus  │
         │  (c1: registered, c2: pending)      │
         │                                      │
         │  [NEXT LABEL or C2?]                 │
         │                                      │
         └─────────────────────────────────────┘
```

---

## 5. IMPLEMENTACIÓN TÉCNICA

### 5.1 Nuevo Controller: LabelScanMobileController.java

```java
package tokai.com.mx.SIGMAV2.modules.labels.adapter.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import tokai.com.mx.SIGMAV2.modules.labels.application.dto.*;
import tokai.com.mx.SIGMAV2.modules.labels.application.service.LabelService;
import tokai.com.mx.SIGMAV2.shared.audit.Auditable;

@Slf4j
@RestController
@RequestMapping("/api/sigmav2/labels/scan")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AUXILIAR_DE_CONTEO', 'ALMACENISTA')")
public class LabelScanMobileController {

    private final LabelService labelService;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * ENDPOINT 1: Validar QR/Folio
     */
    @PostMapping("/validate")
    @Auditable(action = "VALIDATE_LABEL_SCAN", resource = "LABEL")
    public ResponseEntity<LabelValidationResponse> validateLabel(
        @Valid @RequestBody LabelScanValidationRequest request
    ) {
        log.info("🔍 Validando marbete: qrCode={}, countType={}, warehouseId={}",
            request.getQrCode(), request.getCountType(), request.getWarehouseId());

        Long userId = getUserIdFromToken();
        
        LabelValidationResponse response = labelService.validateLabelForMobileScan(
            request.getQrCode(),
            request.getCountType(),
            request.getWarehouseId(),
            request.getPeriodId(),
            userId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 2: Registrar Conteo (C1/C2)
     */
    @PostMapping("/count")
    @Auditable(action = "REGISTER_MOBILE_COUNT", resource = "LABEL")
    public ResponseEntity<LabelCountResponse> registerCount(
        @Valid @RequestBody MobileCountRequest request
    ) {
        log.info("📊 Registrando conteo móvil: folio={}, countType={}, deviceId={}",
            request.getFolio(), request.getCountType(), request.getDeviceId());

        Long userId = getUserIdFromToken();
        
        LabelCountResponse response = labelService.registerMobileCount(
            request.getFolio(),
            request.getCountType(),
            request.getQuantity(),
            request.getWarehouseId(),
            request.getPeriodId(),
            request.getDeviceId(),
            request.getScanTimestamp(),
            userId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * ENDPOINT 3: Obtener Estado de Marbete
     */
    @GetMapping("/status/{folio}")
    public ResponseEntity<LabelStatusResponse> getStatus(
        @PathVariable Long folio,
        @RequestParam Long warehouseId,
        @RequestParam Long periodId
    ) {
        log.info("📋 Obteniendo estado: folio={}, warehouseId={}", folio, warehouseId);

        Long userId = getUserIdFromToken();
        
        LabelStatusResponse response = labelService.getLabelStatusForMobile(
            folio,
            warehouseId,
            periodId,
            userId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 4: Buscar por Folio Manual
     */
    @GetMapping("/folio/{folioNumber}")
    public ResponseEntity<LabelValidationResponse> findByFolioManual(
        @PathVariable Long folioNumber,
        @RequestParam Long warehouseId,
        @RequestParam Long periodId,
        @RequestParam String countType
    ) {
        log.info("🔎 Buscando por folio manual: folio={}, countType={}", folioNumber, countType);

        Long userId = getUserIdFromToken();
        
        LabelValidationResponse response = labelService.validateLabelForMobileScan(
            folioNumber.toString(),
            countType,
            warehouseId,
            periodId,
            userId
        );

        return ResponseEntity.ok(response);
    }

    /**
     * ENDPOINT 5: Obtener Pendientes (Dashboard)
     */
    @GetMapping("/pending")
    public ResponseEntity<PendingLabelsResponse> getPendingLabels(
        @RequestParam Long warehouseId,
        @RequestParam Long periodId,
        @RequestParam(defaultValue = "C1") String countType,
        @RequestParam(defaultValue = "20") Integer limit
    ) {
        log.info("📱 Obteniendo pendientes: warehouse={}, period={}, countType={}, limit={}",
            warehouseId, periodId, countType, limit);

        Long userId = getUserIdFromToken();
        
        PendingLabelsResponse response = labelService.getPendingLabelsForMobile(
            warehouseId,
            periodId,
            countType,
            limit,
            userId
        );

        return ResponseEntity.ok(response);
    }

    // ==================== HELPERS ====================

    private Long getUserIdFromToken() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return authenticatedUserService.getUserIdByEmail(email);
    }
}
```

---

### 5.2 Servicio: LabelService (Agregar métodos)

```java
// En LabelApplicationService

/**
 * Validar marbete para escaneo móvil
 */
public LabelValidationResponse validateLabelForMobileScan(
    String qrCode,
    String countType,
    Long warehouseId,
    Long periodId,
    Long userId
) {
    // Parsear QR
    Long folio = parseQrCode(qrCode);
    
    // Buscar label
    Label label = labelRepository.findById(folio)
        .orElseThrow(() -> new LabelNotFoundException(folio));
    
    // Validar almacén
    if (!label.getWarehouseId().equals(warehouseId)) {
        return LabelValidationResponse.builder()
            .valid(false)
            .message("Marbete pertenece a otro almacén")
            .error("WAREHOUSE_MISMATCH")
            .build();
    }
    
    // Validar período
    Period period = periodRepository.findById(periodId)
        .orElseThrow(() -> new PeriodNotFoundException(periodId));
    
    if (!period.isActive()) {
        return LabelValidationResponse.builder()
            .valid(false)
            .message("Período " + period.getName() + " está cerrado")
            .error("PERIOD_CLOSED")
            .build();
    }
    
    // Validar estado
    if (label.getEstado() != Label.State.IMPRESO) {
        return LabelValidationResponse.builder()
            .valid(false)
            .message("Marbete debe estar IMPRESO, actual: " + label.getEstado())
            .error("INVALID_STATE")
            .build();
    }
    
    // Obtener conteos existentes
    LabelCount counts = labelCountRepository.findByFolio(folio)
        .orElse(null);
    
    // Validar según countType
    boolean valid = true;
    String message = "";
    String validationStatus = "";
    
    if ("C1".equals(countType)) {
        if (counts != null && counts.getOneCount() != null) {
            valid = false;
            message = "C1 ya registrado: " + counts.getOneCount() + " unidades";
            validationStatus = "ALREADY_COUNTED_C1";
        } else {
            message = "✓ Válido para registrar C1";
            validationStatus = "VALID_FOR_C1";
        }
    } else if ("C2".equals(countType)) {
        if (counts == null || counts.getOneCount() == null) {
            valid = false;
            message = "Debe registrar C1 antes de C2";
            validationStatus = "MISSING_C1";
        } else if (counts.getSecondCount() != null) {
            valid = false;
            message = "C2 ya registrado: " + counts.getSecondCount() + " unidades";
            validationStatus = "ALREADY_COUNTED_C2";
        } else {
            message = "✓ Válido para registrar C2";
            validationStatus = "VALID_FOR_C2";
        }
    }
    
    // Obtener info de producto
    Product product = productRepository.findById(label.getProductId())
        .orElseThrow(() -> new ProductNotFoundException(label.getProductId()));
    
    // Obtener cantidad teórica
    Integer theoretical = inventoryRepository
        .findTheoricalQuantity(label.getProductId(), label.getWarehouseId(), periodId)
        .orElse(0);
    
    return LabelValidationResponse.builder()
        .valid(valid)
        .folio(folio)
        .qrCode(label.getQrCode() != null ? label.getQrCode() : "SIGMAV2-FOLIO-" + folio)
        .productId(label.getProductId())
        .productName(product.getDescripcion())
        .theoreticalQuantity(theoretical)
        .estado(label.getEstado().toString())
        .c1(counts != null ? 
            new CountInfo(counts.getOneCount() != null, counts.getOneCount(), counts.getOneCountAt()) :
            new CountInfo(false, null, null))
        .c2(counts != null ? 
            new CountInfo(counts.getSecondCount() != null, counts.getSecondCount(), counts.getSecondCountAt()) :
            new CountInfo(false, null, null))
        .message(message)
        .validationStatus(validationStatus)
        .build();
}

/**
 * Registrar conteo desde móvil
 */
@Transactional
public LabelCountResponse registerMobileCount(
    Long folio,
    String countType,
    Integer quantity,
    Long warehouseId,
    Long periodId,
    String deviceId,
    LocalDateTime scanTimestamp,
    Long userId
) {
    // 1. Re-validar
    LabelValidationResponse validation = validateLabelForMobileScan(
        folio.toString(), countType, warehouseId, periodId, userId);
    
    if (!validation.isValid()) {
        throw new ValidationException(validation.getMessage());
    }
    
    // 2. Validar cantidad
    if (quantity == null || quantity < 0 || quantity > 999999) {
        throw new ValidationException("Cantidad inválida: " + quantity);
    }
    
    // 3. Buscar label y counts
    Label label = labelRepository.findById(folio)
        .orElseThrow(() -> new LabelNotFoundException(folio));
    
    LabelCount counts = labelCountRepository.findByFolio(folio)
        .orElse(new LabelCount());
    counts.setFolio(folio);
    
    // 4. Registrar conteo
    LocalDateTime now = LocalDateTime.now();
    if ("C1".equals(countType)) {
        counts.setOneCount(quantity.longValue());
        counts.setOneCountBy(userId);
        counts.setOneCountAt(now);
    } else {
        counts.setSecondCount(quantity.longValue());
        counts.setSecondCountBy(userId);
        counts.setSecondCountAt(now);
    }
    
    // 5. Guardar
    labelCountRepository.save(counts);
    
    // 6. Crear evento (auditoría móvil)
    LabelCountEvent event = new LabelCountEvent();
    event.setFolio(folio);
    event.setCountNumber("C1".equals(countType) ? 1 : 2);
    event.setQuantity(quantity.longValue());
    event.setUserId(userId);
    event.setCreatedAt(now);
    event.setDeviceId(deviceId);  // NUEVO
    event.setScanTimestamp(scanTimestamp != null ? scanTimestamp : now);  // NUEVO
    labelCountEventRepository.save(event);
    
    // 7. Calcular varianza
    Integer theoretical = inventoryRepository
        .findTheoricalQuantity(label.getProductId(), warehouseId, periodId)
        .orElse(0);
    
    Integer variance = null;
    if ("C2".equals(countType)) {
        variance = quantity - theoretical;
    }
    
    return LabelCountResponse.builder()
        .success(true)
        .folio(folio)
        .countType(countType)
        .quantity(quantity)
        .registeredAt(now)
        .variance(variance)
        .message("✓ " + countType + " registrado exitosamente")
        .build();
}
```

---

## 6. DTOs Y VALIDACIONES

### Requests

**LabelScanValidationRequest.java:**
```java
@Data
@Valid
public class LabelScanValidationRequest {
    @NotBlank(message = "QR/Folio requerido")
    private String qrCode;
    
    @NotBlank(message = "CountType requerido")
    @Pattern(regexp = "C1|C2", message = "Debe ser C1 o C2")
    private String countType;
    
    @NotNull(message = "WarehouseId requerido")
    private Long warehouseId;
    
    @NotNull(message = "PeriodId requerido")
    private Long periodId;
}
```

**MobileCountRequest.java:**
```java
@Data
@Valid
public class MobileCountRequest {
    @NotNull(message = "Folio requerido")
    @Positive(message = "Folio debe ser positivo")
    private Long folio;
    
    @NotBlank(message = "CountType requerido")
    private String countType;  // C1 o C2
    
    @NotNull(message = "Cantidad requerida")
    @Min(value = 0)
    @Max(value = 999999)
    private Integer quantity;
    
    @NotNull(message = "WarehouseId requerido")
    private Long warehouseId;
    
    @NotNull(message = "PeriodId requerido")
    private Long periodId;
    
    @NotBlank(message = "DeviceId requerido")
    private String deviceId;  // UUID único del dispositivo
    
    @NotNull(message = "ScanTimestamp requerido")
    private LocalDateTime scanTimestamp;
}
```

### Responses

**LabelValidationResponse.java:**
```java
@Data
@Builder
public class LabelValidationResponse {
    private boolean valid;
    private Long folio;
    private String qrCode;
    private Long productId;
    private String productName;
    private Integer theoreticalQuantity;
    private String estado;
    private CountInfo c1;
    private CountInfo c2;
    private String message;
    private String validationStatus;  // VALID_FOR_C1, ALREADY_COUNTED_C1, etc.
    private String error;
    
    @Data
    @Builder
    public static class CountInfo {
        private boolean registered;
        private Long quantity;
        private LocalDateTime registeredAt;
    }
}
```

**LabelCountResponse.java:**
```java
@Data
@Builder
public class LabelCountResponse {
    private boolean success;
    private Long folio;
    private String countType;
    private Integer quantity;
    private LocalDateTime registeredAt;
    private Integer variance;
    private String message;
}
```

---

## RESUMEN: APIs a Usar en Flutter

| Operación | Endpoint | Reutilizado | Nuevo |
|-----------|----------|------------|-------|
| **Login** | `POST /auth/login` | ✅ | - |
| **Obtener almacenes** | `GET /warehouses` | ✅ | - |
| **Obtener período** | `GET /periods/active` | ✅ | - |
| **Validar QR** | `POST /labels/scan/validate` | - | ✅ |
| **Registrar C1/C2** | `POST /labels/scan/count` | - | ✅ |
| **Ver estado** | `GET /labels/scan/status/{folio}` | - | ✅ |
| **Buscar por folio** | `GET /labels/scan/folio/{folio}` | - | ✅ |
| **Ver pendientes** | `GET /labels/scan/pending` | - | ✅ |

**Total:** 3 endpoints reutilizados + 5 nuevos endpoints específicos para móvil

---

**Próximo documento:** Implementación Flutter + Code Example

