# 🚀 RESUMEN FINAL: Plan Completo QR/Scanner Mobile + Backend

**Proyecto:** SIGMAV2 v1.0 + Módulo QR/Scanner Móvil  
**Fecha:** 23 de Marzo 2026  
**Para:** Cesar Uriel Gonzalez Saldaña  
**Estado:** 🟢 LISTO PARA IMPLEMENTACIÓN

---

## 📊 ARQUITECTURA RESUMIDA

```
┌─────────────────────────────────────────────────────────┐
│                    FLUJO COMPLETO                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  📱 FLUTTER APP          🔌 API REST          🗄️ BD  │
│  (Móvil Físico)          (Backend)            (MySQL) │
│                                                         │
│  1. Login                                              │
│     └─> POST /auth/login ───────────────────────────┐ │
│                                                        │ │
│  2. Seleccionar Almacén                               │ │
│     └─> GET /warehouses ────────────────────────────┐ │ │
│                                                      │ │ │
│  3. Escanear QR/Folio                               │ │ │
│     └─> POST /labels/scan/validate ────────────────────┤ │
│         Response: ¿Válido para C1/C2?  │ │
│                                                      │ │ │
│  4. Ingresar Cantidad                               │ │ │
│     └─> POST /labels/scan/count ───────────────────────┤ │
│         ├─> Registra C1/C2              │ │
│         ├─> Crea evento con device_id   │ │
│         └─> Auditoría automática        │ │
│                                                      │ │ │
│  5. Confirmación                                    │ │ │
│     └─ GET /labels/scan/status ────────────────────────┤ │
│         (verifica progreso)              │ │
│                                                      │ │ │
└──────────────────────────────────────────────────────┘ │
                                                        │ │
                   ┌────────────────────────────────────┘ │
                   │                                      │
                   ▼                                      │
              BD UPDATES:                                │
              ├─ label_count_events (C1/C2)              │
              ├─ device_id + scan_timestamp              │
              ├─ audit_logs (automático)                 │
              └─ labels.estado (IMPRESO)                 │
                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 MATRIZ DE IMPLEMENTACIÓN

### BACKEND (Java 21 + Spring Boot)

| Componente | Reutilizado | Nuevo | Tiempo | Status |
|-----------|------------|-------|--------|--------|
| **Autenticación** | ✅ AuthController | - | - | ✅ |
| **Almacenes** | ✅ MainWarehouseController | - | - | ✅ |
| **BD (Migración)** | - | ✅ V1_3_0 | 1h | 📋 TODO |
| **Modelo Label** | ⚠️ Extender | ✅ +qrCode | 1h | 📋 TODO |
| **Puerto LabelScanService** | - | ✅ NUEVO | 2h | 📋 TODO |
| **Servicio Aplicación** | - | ✅ LabelScanApplicationService | 6h | 📋 TODO |
| **Controlador REST** | - | ✅ LabelScanMobileController (5 endpoints) | 4h | 📋 TODO |
| **DTOs** | - | ✅ +6 DTOs nuevos | 2h | 📋 TODO |
| **Tests** | - | ✅ 30+ tests | 10h | 📋 TODO |
| **JasperReports** | ⚠️ Modificar | ✅ Incluir QR | 3h | 📋 TODO |
| | | **TOTAL BACKEND** | **~29h (4-5 días)** | ⏳ PRÓXIMO |

---

### FRONTEND (Flutter + Dart)

| Componente | Descripción | Tiempo | Status |
|-----------|-----------|--------|--------|
| **Setup** | Dependencias (Dio, GetX, mobile_scanner) | 1h | 📋 TODO |
| **Servicios HTTP** | ApiService + LabelService + AuthService | 4h | 📋 TODO |
| **Modelos Dart** | JSON serialization con json_annotation | 3h | 📋 TODO |
| **Controllers** | GetX state management | 3h | 📋 TODO |
| **LoginScreen** | Autenticación + token storage | 3h | 📋 TODO |
| **ScannerScreen** | Camera + mobile_scanner | 5h | 📋 TODO |
| **ValidationScreen** | Mostrar info del marbete | 2h | 📋 TODO |
| **CountScreen** | Ingreso de cantidad + validaciones | 3h | 📋 TODO |
| **ConfirmationScreen** | Mostrar resultado + varianza | 2h | 📋 TODO |
| **Integración E2E** | Testing en dispositivos reales | 6h | 📋 TODO |
| | **TOTAL FRONTEND** | **~32h (5 días)** | ⏳ PRÓXIMO |

---

## 📅 TIMELINE PROPUESTO

### **Fase 1: Semana 1 (Backend Setup)**

```
LUN 24/3 - VIE 28/3
│
├─ LUNES
│  └─ ✅ Crear migración Flyway (V1_3_0)
│     └─ Agregar qr_code a labels
│     └─ Agregar device_id + scan_timestamp a label_count_events
│
├─ MARTES  
│  └─ ✅ Extender Label.java
│     └─ Agregar @Column qrCode
│     └─ Método generateQrCode()
│  └─ ✅ Crear puertos (interfaces)
│     └─ LabelScanService port
│
├─ MIÉRCOLES
│  └─ ✅ Implementar LabelScanApplicationService
│     └─ validateLabelForMobileScan()
│     └─ registerMobileCount()
│     └─ getLabelStatusForMobile()
│
├─ JUEVES
│  └─ ✅ Crear LabelScanMobileController
│     └─ 5 nuevos endpoints
│  └─ ✅ Crear DTOs + Mapper MapStruct
│
└─ VIERNES
   └─ ✅ Tests unitarios + integración
   └─ ✅ Deploy a ambiente QA
```

**Resultado:** Backend funcional + Postman collection

---

### **Fase 2: Semana 2 (Frontend Development)**

```
LUN 31/3 - VIE 4/4
│
├─ LUNES-MARTES
│  └─ ✅ Setup Flutter + dependencias
│  └─ ✅ Servicios HTTP (Dio, ApiService)
│  └─ ✅ Modelos Dart con JSON
│
├─ MIÉRCOLES
│  └─ ✅ LoginScreen (POST /auth/login)
│  └─ ✅ Storage seguro (Keychain/Keystore)
│
├─ JUEVES
│  └─ ✅ ScannerScreen (mobile_scanner)
│  └─ ✅ ValidationScreen
│  └─ ✅ CountScreen
│
└─ VIERNES
   └─ ✅ ConfirmationScreen
   └─ ✅ Integración E2E
   └─ ✅ Testing en dispositivo real
```

**Resultado:** App Flutter funcional + build APK/IPA

---

### **Fase 3: Semana 3 (QA + Refinamiento)**

```
LUN 7/4 - VIE 11/4
│
├─ LUNES-MARTES
│  └─ ✅ Testing E2E completo
│  └─ ✅ Pruebas en múltiples dispositivos
│  └─ ✅ Validación offline
│
├─ MIÉRCOLES
│  └─ ✅ Refinamiento UI/UX
│  └─ ✅ Feedback de usuarios (pilotos)
│
├─ JUEVES
│  └─ ✅ Correcciones críticas
│  └─ ✅ Documentación de usuario
│
└─ VIERNES
   └─ ✅ Capacitación del equipo almacén
   └─ ✅ Pre-release checklist
```

**Resultado:** App lista para producción

---

### **Fase 4: Semana 4 (Producción + Monitoreo)**

```
LUN 14/4 - VIE 18/4
│
├─ Lanzamiento en Producción
├─ Monitoreo 24/7
├─ Soporte de bugs críticos
└─ Iteración v1.1 (mejoras)
```

---

## 📊 MATRIZ DE APIs UTILIZADAS

### Reutilizadas (Implementadas en SIGMAV2)

```
┌──────────────────────────────────────────────────────┐
│ API EXISTENTE → REUTILIZAR EN FLUTTER               │
├──────────────────────────────────────────────────────┤
│                                                      │
│ ✅ POST /auth/login                                 │
│    Propósito: Autenticar usuario                    │
│    Response: { token, user, expiresIn }            │
│                                                      │
│ ✅ GET /warehouses                                  │
│    Propósito: Listar almacenes asignados            │
│    Response: List<Warehouse>                        │
│                                                      │
│ ✅ GET /periods/active                              │
│    Propósito: Obtener período actual                │
│    Response: { id, nombre, estado }                 │
│                                                      │
│ ✅ POST /labels/counts/c1                           │
│    Propósito: Registrar C1                          │
│    Response: { id, folio, oneCount, ... }          │
│                                                      │
│ ✅ POST /labels/counts/c2                           │
│    Propósito: Registrar C2                          │
│    Response: { id, folio, secondCount, ... }       │
│                                                      │
│ ✅ GET /labels/by-folio/{folio}                     │
│    Propósito: Obtener datos del marbete            │
│    Response: Label DTO                              │
│                                                      │
│ ✅ GET /labels/for-count                            │
│    Propósito: Validar marbete antes de conteo      │
│    Response: LabelForCountDTO                       │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### Nuevas (Crear para Móvil)

```
┌──────────────────────────────────────────────────────┐
│ API NUEVA → ESPECÍFICA PARA MÓVIL                    │
├──────────────────────────────────────────────────────┤
│                                                      │
│ 🆕 POST /labels/scan/validate                       │
│    Validar QR antes de pedir cantidad               │
│    Input: { qrCode, countType, warehouseId, ... }  │
│    Output: LabelValidationResponse                  │
│                                                      │
│ 🆕 POST /labels/scan/count                          │
│    Registrar conteo con device_id                   │
│    Input: { folio, qty, deviceId, scanTimestamp } │
│    Output: LabelCountResponse                       │
│                                                      │
│ 🆕 GET /labels/scan/status/{folio}                  │
│    Obtener estado del marbete                       │
│    Output: { c1, c2, variance, ready ForC2 }       │
│                                                      │
│ 🆕 GET /labels/scan/folio/{folio}                   │
│    Fallback: buscar por folio manual                │
│    Output: LabelValidationResponse                  │
│                                                      │
│ 🆕 GET /labels/scan/pending                         │
│    Dashboard: marbetes sin conteo                   │
│    Output: PendingLabelsResponse                    │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## 🎓 RESUMEN: QR vs CÓDIGO DE BARRAS (Decisión Final)

```
╔════════════════════════════════════════════════════════╗
║  RECOMENDACIÓN FINAL: USAR QR CODE                   ║
╠════════════════════════════════════════════════════════╣
║                                                        ║
║  ✅ RAZONES:                                          ║
║  ┌────────────────────────────────────────────────┐  ║
║  │ 1. Robustez en almacén (polvo, humedad)       │  ║
║  │    - QR: funciona 70% dañado (30% corrección) │  ║
║  │    - Barras: funciona 100% entero o nada      │  ║
║  │                                                │  ║
║  │ 2. Ya tienes infraestructura                  │  ║
║  │    - Flutter + cámara disponible              │  ║
║  │    - NO necesitas lector USB ($150)           │  ║
║  │                                                │  ║
║  │ 3. Escalabilidad futura                       │  ║
║  │    - Hoy: "42"                                │  ║
║  │    - Mañana: "SIGMAV2-FOLIO-42-P16-W369"    │  ║
║  │    - QR: 7,000 caracteres                    │  ║
║  │    - Barras: 40 caracteres máx                │  ║
║  │                                                │  ║
║  │ 4. Costo igual (impresión)                    │  ║
║  │    - +$0.00 por etiqueta                      │  ║
║  │                                                │  ║
║  └────────────────────────────────────────────────┘  ║
║                                                        ║
║  OPCIONAL: Dual (QR + Barras) si quieres máxima     ║
║  confiabilidad. Costo: +$0.01 por etiqueta.         ║
║                                                        ║
╚════════════════════════════════════════════════════════╝
```

---

## 💻 CÓDIGO GENERADO (Ubicaciones)

### Backend Java

```
📁 src/main/java/tokai/com/mx/SIGMAV2/
├─ modules/labels/
│  ├─ domain/
│  │  ├─ model/
│  │  │  └─ Label.java (✅ extender con qrCode)
│  │  └─ port/input/
│  │     └─ LabelScanService.java (🆕 CREAR)
│  ├─ application/service/
│  │  └─ LabelScanApplicationService.java (🆕 CREAR)
│  ├─ infrastructure/persistence/
│  │  ├─ JpaLabelRepository.java (⚠️ extender)
│  │  └─ LabelScanRepositoryAdapter.java (🆕 CREAR)
│  └─ adapter/controller/
│     └─ LabelScanMobileController.java (🆕 CREAR)
│
└─ shared/
   └─ infrastructure/
      └─ qr/
         └─ QrCodeGenerator.java (🆕 CREAR)
```

### Migraciones

```
📁 src/main/resources/db/migration/
└─ V1_3_0__Add_qr_code_to_labels.sql (🆕 CREAR)
```

### Flutter

```
📁 sigmav2_mobile/lib/
├─ services/
│  ├─ api_service.dart (✅ CREAR)
│  ├─ label_service.dart (✅ CREAR)
│  └─ auth_service.dart (✅ CREAR)
├─ models/
│  ├─ label_model.dart (✅ CREAR)
│  └─ count_response_model.dart (✅ CREAR)
├─ controllers/
│  ├─ label_controller.dart (✅ CREAR)
│  └─ auth_controller.dart (✅ CREAR)
└─ views/
   ├─ login_screen.dart (✅ CREAR)
   ├─ scanner_screen.dart (✅ CREAR)
   ├─ validation_screen.dart (✅ CREAR)
   ├─ count_screen.dart (✅ CREAR)
   └─ confirmation_screen.dart (✅ CREAR)
```

---

## 🎯 CHECKLIST DE INICIO

### Antes de Empezar

- [ ] **Confirmar stack móvil:** ¿Solo Flutter? ¿PWA + Flutter?
- [ ] **Recursos:** ¿Cuántos devs backend? ¿Cuántos flutter?
- [ ] **Ambiente:** ¿Servidor QA disponible?
- [ ] **BD:** ¿Acceso para ejecutar migraciones?
- [ ] **Dispositivos:** ¿Smartphones Android/iOS para testing?
- [ ] **Conectividad:** ¿WiFi en almacenes durante piloto?

### Fase 1: Backend (Semana 1)

- [ ] Crear rama `feature/qr-mobile-scanning`
- [ ] Ejecutar migración V1_3_0
- [ ] Extender Label entity
- [ ] Implementar LabelScanService port
- [ ] Implementar LabelScanApplicationService
- [ ] Crear LabelScanMobileController
- [ ] Escribir tests unitarios
- [ ] Deploy a QA
- [ ] Probar endpoints con Postman

### Fase 2: Frontend (Semana 2)

- [ ] Crear proyecto Flutter `sigmav2_mobile`
- [ ] Configurar Dio + GetX + mobile_scanner
- [ ] Implementar servicios HTTP
- [ ] Crear modelos Dart
- [ ] Construir LoginScreen
- [ ] Construir ScannerScreen
- [ ] Integración E2E con backend
- [ ] Testing en Android + iOS
- [ ] Refinamiento UI

### Fase 3: QA (Semana 3)

- [ ] Testing completo de flujos
- [ ] Pruebas de seguridad (JWT, revocación)
- [ ] Pruebas de carga (100 conteos/min)
- [ ] Testing en offline (caché local)
- [ ] Documentación usuario
- [ ] Capacitación personal almacén

---

## 📋 ENTREGABLES FINALES

✅ **Backend:**
- [ ] 5 nuevos endpoints REST
- [ ] Migración Flyway ejecutada
- [ ] 30+ tests unitarios/integración
- [ ] Documentación API (Swagger/OpenAPI)
- [ ] Postman collection

✅ **Frontend:**
- [ ] App Flutter completa
- [ ] Build APK (Android)
- [ ] Build IPA (iOS)
- [ ] Código documentado
- [ ] Manual de usuario

✅ **Datos:**
- [ ] Auditoría completa (audit_logs)
- [ ] Trazabilidad (device_id + scan_timestamp)
- [ ] Reportes de varianza (C1 vs C2)

---

## 📞 CONTACTOS Y REFERENCIAS

**Documentos Generados en `/docs/`:**
1. `RESUMEN-EJECUTIVO-QR-MOVIL.md` — Análisis completo
2. `ARQUITECTURA-APIs-REUTILIZACION-NUEVOS-ENDPOINTS.md` — Design técnico
3. `FLUTTER-IMPLEMENTACION-COMPLETA.md` — Código Flutter
4. `QR-vs-CODIGO-BARRAS-COMPARATIVA.md` — Decisión QR
5. **Este documento** — Plan de ejecución

---

## 🚀 PRÓXIMOS PASOS

**Hoy:** ✅ Aprobación del plan

**Mañana:**
1. Confirmar recursos (devs backend + flutter)
2. Crear rama de feature
3. Empezar migración BD
4. Setup inicial Flutter

**¿Preguntas?** 📧 Contacta a Cesar Uriel Gonzalez Saldaña

---

**Estado:** 🟢 LISTO PARA IMPLEMENTACIÓN  
**Fecha:** 23 de Marzo 2026  
**Versión:** 1.0 Final  
**Autor:** GitHub Copilot + Análisis SIGMAV2 v1.0


