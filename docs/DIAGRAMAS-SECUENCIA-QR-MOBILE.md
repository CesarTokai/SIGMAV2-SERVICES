# 📊 DIAGRAMA DE SECUENCIA: Flujo Completo QR/Mobile Scanning

**Documento Visual:** Secuencia de interacciones entre Flutter + Backend + BD

---

## 1️⃣ FLUJO LOGIN

```
┌─────────────┐              ┌──────────────┐              ┌────────────┐
│   FLUTTER   │              │   BACKEND    │              │     BD     │
│   APP       │              │   SIGMAV2    │              │   MySQL    │
└─────────────┘              └──────────────┘              └────────────┘
      │                             │                             │
      │  1. POST /auth/login        │                             │
      │  {email, password}          │                             │
      ├────────────────────────────►│                             │
      │                             │  SELECT * FROM users       │
      │                             │  WHERE email = ...         │
      │                             ├────────────────────────────►│
      │                             │                             │
      │                             │◄────────────────────────────┤
      │                             │  user record                │
      │                             │                             │
      │                             │  Generar JWT token         │
      │                             │  (expires: 30 min)         │
      │                             │                             │
      │◄────────────────────────────┤                             │
      │ {                           │                             │
      │   token: "eyJ...",          │                             │
      │   user: {...},              │                             │
      │   expiresIn: 1800           │                             │
      │ }                           │                             │
      │                             │                             │
      │  Guardar en Keychain/       │                             │
      │  Keystore (seguro)          │                             │
      │                             │                             │
      └─────────────────────────────────────────────────────────►
         Token almacenado localmente
```

---

## 2️⃣ FLUJO ESCANEO QR

```
┌─────────────────┐              ┌──────────────┐              ┌────────────┐
│   FLUTTER       │              │   BACKEND    │              │     BD     │
│   (Scanner)     │              │   SIGMAV2    │              │   MySQL    │
└─────────────────┘              └──────────────┘              └────────────┘
      │                                │                             │
      │ [Abre cámara]                  │                             │
      │ [Usuario apunta al QR]         │                             │
      │                                │                             │
      │ [QR Detectado: "42"]           │                             │
      │                                │                             │
      │ 1. POST /labels/scan/validate  │                             │
      │ {                              │                             │
      │   qrCode: "42",                │                             │
      │   countType: "C1",             │                             │
      │   warehouseId: 369,            │                             │
      │   periodId: 16                 │                             │
      │ }                              │                             │
      ├───────────────────────────────►│                             │
      │ (+ Authorization header)       │                             │
      │                                │ SELECT * FROM labels       │
      │                                │ WHERE folio = 42           │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ Label record               │
      │                                │                             │
      │                                │ SELECT * FROM label_counts │
      │                                │ WHERE folio = 42           │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ LabelCount record          │
      │                                │ (oneCount=null)            │
      │                                │                             │
      │                                │ ✓ Validar:               │
      │                                │   - Período activo        │
      │                                │   - Estado IMPRESO        │
      │                                │   - C1 no registrado      │
      │                                │   - Almacén correcto      │
      │                                │                             │
      │◄───────────────────────────────┤                             │
      │ {                              │                             │
      │   valid: true,                 │                             │
      │   folio: 42,                   │                             │
      │   productName: "Laptop",       │                             │
      │   theoretical: 100,            │                             │
      │   c1: {                        │                             │
      │     registered: false,         │                             │
      │     quantity: null             │                             │
      │   },                           │                             │
      │   message: "✓ Válido C1",      │                             │
      │   validationStatus: "VALID.." │                             │
      │ }                              │                             │
      │                                │                             │
      │ [Mostrar info en pantalla]    │                             │
      │ Laptop Dell - 100 unidades    │                             │
      │                                │                             │
```

---

## 3️⃣ FLUJO REGISTRAR CONTEO

```
┌─────────────────┐              ┌──────────────┐              ┌────────────┐
│   FLUTTER       │              │   BACKEND    │              │     BD     │
│   (Count)       │              │   SIGMAV2    │              │   MySQL    │
└─────────────────┘              └──────────────┘              └────────────┘
      │                                │                             │
      │ Usuario ingresa cantidad: 95   │                             │
      │ Usuario toca [Guardar]         │                             │
      │                                │                             │
      │ 1. POST /labels/scan/count     │                             │
      │ {                              │                             │
      │   folio: 42,                   │                             │
      │   countType: "C1",             │                             │
      │   quantity: 95,                │                             │
      │   warehouseId: 369,            │                             │
      │   periodId: 16,                │                             │
      │   deviceId: "UUID-MOB-001",    │                             │
      │   scanTimestamp: "2026-03-..." │                             │
      │ }                              │                             │
      ├───────────────────────────────►│                             │
      │                                │ @Transactional {          │
      │                                │                             │
      │                                │   // 1. Validar           │
      │                                │   SELECT * FROM labels    │
      │                                │   WHERE folio = 42        │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ Label                      │
      │                                │                             │
      │                                │   SELECT * FROM periods   │
      │                                │   WHERE id = 16           │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ Period (activo=true)       │
      │                                │                             │
      │                                │   // 2. Actualizar conteo │
      │                                │   SELECT * FROM           │
      │                                │   label_counts WHERE ...  │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ LabelCount                 │
      │                                │                             │
      │                                │   UPDATE label_counts     │
      │                                │   SET oneCount = 95,      │
      │                                │       oneCountAt = NOW(), │
      │                                │       oneCountBy = 42     │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ ✓ Updated 1 row           │
      │                                │                             │
      │                                │   // 3. Crear evento      │
      │                                │   INSERT INTO             │
      │                                │   label_count_events(...) │
      │                                │   VALUES (                │
      │                                │     folio: 42,            │
      │                                │     count_num: 1,         │
      │                                │     quantity: 95,         │
      │                                │     user_id: 42,          │
      │                                │     device_id: "UUID..",  │◄── NUEVO
      │                                │     scan_timestamp: "..", │◄── NUEVO
      │                                │     created_at: NOW()     │
      │                                │   )                       │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ ✓ Inserted                │
      │                                │                             │
      │                                │   // 4. @Auditable (AOP) │
      │                                │   INSERT INTO audit_logs  │
      │                                │   VALUES (                │
      │                                │     user_id: 42,          │
      │                                │     action: "REGISTER..", │
      │                                │     resource: "LABEL",    │
      │                                │     metadata: {...},      │
      │                                │     ip: "192.168...",     │
      │                                │     created_at: NOW()     │
      │                                │   )                       │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ ✓ Inserted                │
      │                                │                             │
      │                                │ } COMMIT transacción      │
      │                                │                             │
      │◄───────────────────────────────┤                             │
      │ {                              │                             │
      │   success: true,               │                             │
      │   folio: 42,                   │                             │
      │   countType: "C1",             │                             │
      │   quantity: 95,                │                             │
      │   variance: -5,                │                             │
      │   registeredAt: "2026-03-...", │                             │
      │   message: "✓ C1 registrado"  │                             │
      │ }                              │                             │
      │                                │                             │
      │ [Mostrar confirmación]         │                             │
      │ ✅ ÉXITO - C1: 95 unidades    │                             │
      │ Varianza: -5 (100 teórico)    │                             │
      │                                │                             │
```

---

## 4️⃣ FLUJO OBTENER ESTADO

```
┌─────────────────┐              ┌──────────────┐              ┌────────────┐
│   FLUTTER       │              │   BACKEND    │              │     BD     │
│   (Dashboard)   │              │   SIGMAV2    │              │   MySQL    │
└─────────────────┘              └──────────────┘              └────────────┘
      │                                │                             │
      │ Usuario toca [Ver Estado]      │                             │
      │                                │                             │
      │ 1. GET /labels/scan/status/42  │                             │
      │?warehouseId=369&periodId=16    │                             │
      ├───────────────────────────────►│                             │
      │                                │ SELECT * FROM labels       │
      │                                │ WHERE folio = 42           │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ Label                      │
      │                                │                             │
      │                                │ SELECT * FROM label_counts │
      │                                │ WHERE folio = 42           │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ {                          │
      │                                │   oneCount: 95,           │
      │                                │   secondCount: null,      │
      │                                │   oneCountAt: "...",      │
      │                                │   secondCountAt: null     │
      │                                │ }                          │
      │                                │                             │
      │                                │ SELECT theoretical FROM    │
      │                                │ inventory_stock WHERE...   │
      │                                ├────────────────────────────►│
      │                                │                             │
      │                                │◄────────────────────────────┤
      │                                │ theoretical: 100           │
      │                                │                             │
      │                                │ Calcular varianza:        │
      │                                │   variance = 95 - 100     │
      │                                │   variance = -5           │
      │                                │                             │
      │◄───────────────────────────────┤                             │
      │ {                              │                             │
      │   folio: 42,                   │                             │
      │   productName: "Laptop",       │                             │
      │   estado: "IMPRESO",           │                             │
      │   theoretical: 100,            │                             │
      │   c1: {                        │                             │
      │     registered: true,          │                             │
      │     quantity: 95,              │                             │
      │     registeredAt: "..."        │                             │
      │   },                           │                             │
      │   c2: {                        │                             │
      │     registered: false,         │                             │
      │     quantity: null             │                             │
      │   },                           │                             │
      │   variance: -5,                │                             │
      │   readyForC2: true,            │                             │
      │   message: "C1 registrado...   │                             │
      │            Listo para C2"      │                             │
      │ }                              │                             │
      │                                │                             │
      │ [Mostrar dashboard]            │                             │
      │ Folio #42: Laptop              │                             │
      │ C1: ✓ 95 unidades             │                             │
      │ C2: ⏳ Pendiente              │                             │
      │ Teórico: 100                   │                             │
      │ Varianza: -5                   │                             │
      │ [Registrar C2] [Siguiente]     │                             │
      │                                │                             │
```

---

## 5️⃣ FLUJO SEGUNDO CONTEO (C2)

```
Igual al Flujo 3️⃣ pero:
- countType = "C2"
- Backend valida: C1 debe estar registrado
- Backend actualiza: secondCount, secondCountAt, secondCountBy
- Crea evento: count_number = 2
- Auditología: "REGISTER_MOBILE_COUNT C2"

Diferencia: La varianza ahora tiene valor significativo:
  variance = (C2: 95) - (Teórico: 100) = -5 unidades
```

---

## 6️⃣ DIAGRAMA COMPLETO DE ESTADOS

```
┌─────────────────────────────────────────────────────────────┐
│          CICLO DE VIDA DE UN MARBETE                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Estado Inicial: GENERADO                                  │
│       │                                                    │
│       ▼                                                    │
│  [Impresión]                                              │
│       │                                                    │
│       ▼                                                    │
│  Estado: IMPRESO ✓                                        │
│       │                                                    │
│       ├─────────────────────────────┬──────────────────┐  │
│       │                             │                  │  │
│       ▼                             ▼                  ▼  │
│  Usuario decide:              Escaneo QR/         Manual  │
│  1. Escanear                  Folio OK?           Entry   │
│  2. Contar (C1)               ✓ YES                       │
│       │                             │                     │
│       ▼                             ▼                     │
│  POST /labels/scan/count      Mostrar Info              │
│  + countType: "C1"                  │                     │
│  + quantity: 95                     ▼                     │
│  + deviceId: UUID                Ingreso Cantidad         │
│  + scanTimestamp: NOW              │                      │
│       │                             ▼                     │
│       └────────────────┬────────────┘                     │
│                        │                                   │
│                        ▼                                   │
│        Transacción Atómica:                               │
│        ├─ UPDATE label_counts SET oneCount = 95         │
│        ├─ INSERT label_count_events (device_id, ...)    │
│        ├─ INSERT audit_logs (@Auditable)                │
│        └─ COMMIT                                         │
│                        │                                   │
│                        ▼                                   │
│        Estado: C1 ✓ Registrado                           │
│        message: "Listo para C2"                          │
│                        │                                   │
│                        ▼                                   │
│        ¿Registrar C2?                                    │
│        ├─ SÍ → Repetir con countType: "C2"             │
│        │              varianza = C2 - Teórico           │
│        │                                                 │
│        └─ NO → Siguiente Marbete                        │
│                                                          │
│        Estados Finales Posibles:                        │
│        ├─ C1 Registrado + C2 Pendiente ⏳              │
│        ├─ C1 ✓ + C2 ✓ (Varianza calculada)            │
│        └─ CANCELADO (si usuario cancela)               │
│                                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 7️⃣ DIAGRAMA DE TABLAS (BD)

```
┌──────────────────────────────────────────────────────────┐
│               MODELO DE BD INVOLUCRADO                   │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  labels (Ya existe)                                     │
│  ├─ folio (PK) ◄─────────┐                             │
│  ├─ id_period             │                             │
│  ├─ id_warehouse          │                             │
│  ├─ id_product            │                             │
│  ├─ estado (IMPRESO)      │                             │
│  ├─ qr_code ──────────────┤  (NUEVO)                   │
│  ├─ impreso_at            │  VARCHAR 500 UNIQUE        │
│  ├─ created_by            │                             │
│  └─ created_at            │                             │
│                            │                             │
│  label_counts (Ya existe)  │                             │
│  ├─ id_label_count        │                             │
│  ├─ folio (FK) ───────────┤                             │
│  ├─ oneCount              │  C1 Data                    │
│  ├─ oneCountBy            │                             │
│  ├─ oneCountAt            │                             │
│  ├─ secondCount           │  C2 Data                    │
│  ├─ secondCountBy         │                             │
│  └─ secondCountAt         │                             │
│                            │                             │
│  label_count_events ◄──────┤  (ACTUALIZADO en V1_3_0)  │
│  ├─ id_count_event        │                             │
│  ├─ folio (FK)            │                             │
│  ├─ count_number (1 o 2)  │                             │
│  ├─ quantity              │                             │
│  ├─ user_id               │                             │
│  ├─ device_id ────────────┤  (NUEVO)                   │
│  ├─ scan_timestamp ───────┤  (NUEVO)                   │
│  └─ created_at            │                             │
│                            │                             │
│  audit_logs (Ya existe)    │                             │
│  ├─ id_audit              │                             │
│  ├─ action                │  Automático por            │
│  ├─ resource              │  @Auditable AOP            │
│  ├─ user_id               │                             │
│  ├─ metadata              │                             │
│  ├─ ip_address            │                             │
│  └─ created_at            │                             │
│                            │                             │
│  periods (Ya existe)       │                             │
│  ├─ id                     │                             │
│  ├─ nombre                 │  Validación C1/C2          │
│  ├─ fecha_inicio           │  (período activo)          │
│  ├─ fecha_fin              │                             │
│  └─ estado (ACTIVO)        │                             │
│                            │                             │
│  users (Ya existe)         │                             │
│  ├─ id_user               │                             │
│  ├─ email                 │  Login + auditoría          │
│  ├─ password_hash         │                             │
│  └─ rol                   │                             │
│                            │                             │
│  mobile_devices (OPCIONAL) │  (Nuevo en V1_3_0)        │
│  ├─ id_device             │                             │
│  ├─ id_user               │  Rastreo de dispositivos    │
│  ├─ device_name           │  para auditoría avanzada    │
│  ├─ device_type           │                             │
│  ├─ imei                  │                             │
│  ├─ last_activity         │                             │
│  └─ is_active             │                             │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 8️⃣ FLUJO DE ERRORES Y VALIDACIONES

```
┌─────────────────────────────────────────────────────────┐
│          VALIDACIONES Y MANEJO DE ERRORES              │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. POST /labels/scan/validate                         │
│     ├─ QR inválido/no existe                          │
│     │  └─> 404 NOT_FOUND                              │
│     ├─ Período cerrado                                │
│     │  └─> 400 PERIOD_CLOSED                          │
│     ├─ Marbete en almacén diferente                   │
│     │  └─> 403 WAREHOUSE_MISMATCH                     │
│     ├─ C1 ya registrado (para C1)                     │
│     │  └─> 409 ALREADY_COUNTED_C1                     │
│     ├─ C1 no registrado (para C2)                     │
│     │  └─> 409 MISSING_C1                             │
│     └─ ✓ TODO OK                                      │
│        └─> 200 VALID_FOR_C1/C2                        │
│                                                         │
│  2. POST /labels/scan/count                            │
│     ├─ Cantidad inválida (< 0 o > 999999)             │
│     │  └─> 400 BAD_REQUEST                            │
│     ├─ Marbete no encontrado                          │
│     │  └─> 404 NOT_FOUND                              │
│     ├─ Validación previa falla                        │
│     │  └─> 409 VALIDATION_FAILED                      │
│     ├─ Error en transacción                           │
│     │  └─> 500 INTERNAL_SERVER_ERROR                  │
│     │     (ROLLBACK automático)                       │
│     └─ ✓ TODO OK                                      │
│        └─> 201 CREATED                                │
│           (C1/C2 registrado)                          │
│                                                         │
│  3. Seguridad                                          │
│     ├─ Token expirado                                 │
│     │  └─> 401 UNAUTHORIZED                           │
│     │     (Ir a Login)                                │
│     ├─ Permiso insuficiente                           │
│     │  └─> 403 FORBIDDEN                              │
│     │     (Role != AUXILIAR_DE_CONTEO)                │
│     ├─ Acceso denegado a almacén                      │
│     │  └─> 403 WAREHOUSE_ACCESS_DENIED                │
│     └─ Rate limit excedido                            │
│        └─> 429 TOO_MANY_REQUESTS                      │
│           (Max 100 conteos/min)                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 9️⃣ TIMELINE VISUAL (Gantt Chart)

```
SEMANA 1 (24-28/3)
├─ LUN ████ Migración BD
├─ MAR ████ Extender Label + Puerto
├─ MIÉ ████ Servicio Aplicación
├─ JUE ████ Controlador REST
└─ VIE ████ Tests + Deploy QA

SEMANA 2 (31/3-4/4)
├─ LUN ████ Setup Flutter + Servicios
├─ MAR ████ LoginScreen
├─ MIÉ ████ ScannerScreen + ValidationScreen
├─ JUE ████ CountScreen
└─ VIE ████ ConfirmationScreen + E2E

SEMANA 3 (7-11/4)
├─ LUN ████ Testing E2E
├─ MAR ████ Refinamiento UI/UX
├─ MIÉ ████ Feedback usuarios
├─ JUE ████ Correcciones críticas
└─ VIE ████ Capacitación

SEMANA 4 (14-18/4)
├─ LUN ████ Pre-launch checklist
├─ MAR ████ Producción
├─ MIÉ ████ Monitoreo 24/7
├─ JUE ████ Soporte crítico
└─ VIE ████ Iteración v1.1
```

---

**Visualización completa:** Los diagramas anteriores representan todos los puntos de integración entre Flutter + Backend + BD para el flujo de QR/Scanner.


