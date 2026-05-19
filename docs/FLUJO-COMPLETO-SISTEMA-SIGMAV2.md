# 🗺️ FLUJO COMPLETO DEL SISTEMA SIGMAV2

**Documento maestro que integra toda la arquitectura, componentes y workflows del sistema.**

**Última actualización:** 2026-03-23

---

## 📋 TABLA DE CONTENIDOS

1. [Arquitectura General](#arquitectura-general)
2. [Flujo de Autenticación](#flujo-de-autenticación)
3. [Flujo Principal del Sistema](#flujo-principal-del-sistema)
4. [Flujo Detallado por Módulo](#flujo-detallado-por-módulo)
5. [Interacción de Tablas de BD](#interacción-de-tablas-de-bd)
6. [Estados y Transiciones](#estados-y-transiciones)
7. [Flujo de Errores y Excepciones](#flujo-de-errores-y-excepciones)

---

## 🏗️ ARQUITECTURA GENERAL

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENTE (Frontend)                      │
│                    (Navegador / Aplicación)                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTP/REST
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                       SPRING BOOT 3.5.5                          │
│                    (Puerto 8080 por defecto)                     │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │               SECURITY LAYER (Filtros)                  │   │
│  │                                                          │   │
│  │  1. JwtRevocationFilter      (Valida token no revocado) │   │
│  │  2. JwtAuthenticationFilter  (Autentica con JWT)        │   │
│  │  3. UserActivityFilter       (Registra actividad)       │   │
│  │  4. CORS Filter              (Valida origen)            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │            ADAPTADORES WEB (Controllers)                │   │
│  │                                                          │   │
│  │  • UserController           (Autenticación, usuarios)   │   │
│  │  • PeriodController         (Períodos contables)        │   │
│  │  • InventoryController      (Catálogo productos)        │   │
│  │  • MultiWarehouseController (Stock multialmacén)        │   │
│  │  • LabelController          (Marbetes completo)         │   │
│  │  • WarehouseController      (Almacenes)                 │   │
│  │  • MailController           (Notificaciones)            │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │         PUERTOS DE DOMINIO (Interfaces)                 │   │
│  │                                                          │   │
│  │  Input Ports (Casos de uso):                            │   │
│  │  • UserService              (domain/port/input)         │   │
│  │  • PeriodService            (domain/port/input)         │   │
│  │  • LabelService             (domain/port/input)         │   │
│  │  • InventoryService         (domain/port/input)         │   │
│  │                                                          │   │
│  │  Output Ports (Persistencia):                           │   │
│  │  • UserRepository           (domain/port/output)        │   │
│  │  • PeriodRepository         (domain/port/output)        │   │
│  │  • LabelRepository          (domain/port/output)        │   │
│  │  • InventoryStockRepository (domain/port/output)        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │    SERVICIOS DE APLICACIÓN (Business Logic)             │   │
│  │                                                          │   │
│  │  • UserApplicationService   (Autenticación)             │   │
│  │  • PeriodApplicationService (Gestión períodos)          │   │
│  │  • LabelApplicationService  (Generación marbetes)       │   │
│  │  • InventoryService         (Gestión inventario)        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │      ADAPTADORES DE INFRAESTRUCTURA                      │   │
│  │                                                          │   │
│  │  Persistencia:                                           │   │
│  │  • UserRepositoryAdapter    (JPA → Domain)              │   │
│  │  • LabelRepositoryAdapter   (JPA → Domain)              │   │
│  │  • InventoryAdapter         (JPA → Domain)              │   │
│  │                                                          │   │
│  │  Mappers:                                                │   │
│  │  • UserMapper               (BeanUser ↔ User)           │   │
│  │  • LabelMapper              (BeanLabel ↔ Label)         │   │
│  │                                                          │   │
│  │  Externos:                                               │   │
│  │  • MailSenderAdapter        (Gmail)                      │   │
│  │  • JasperReportsAdapter     (PDF)                        │   │
│  │  • ExcelFileAdapter         (Excel)                      │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │    MODELOS DE DOMINIO (Domain Layer - PURO)             │   │
│  │                                                          │   │
│  │  • User         (Id, email, rol, estado)                │   │
│  │  • Period       (Contexto temporal)                      │   │
│  │  • Label        (Marbete individual)                     │   │
│  │  • Product      (Artículo de inventario)                 │   │
│  │  • Warehouse    (Almacén físico)                         │   │
│  │  • FolioRequest (Solicitud de rango de folios)           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │      ASPECTOS TRANSVERSALES (AOP)                        │   │
│  │                                                          │   │
│  │  • @Auditable   (Auditoría de acciones)                  │   │
│  │  • @Transactional (Gestión de transacciones)             │   │
│  └──────────────────────────────────────────────────────────┘   │
│                             ↓                                    │
└─────────────────────────────┬────────────────────────────────────┘
                             │
                             │ SQL
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                     MYSQL 8.0+ DATABASE                          │
│                   (SIGMAV2_2 por defecto)                        │
│                                                                   │
│  Migraciones Flyway:                                             │
│  • V1_0_1__Initial_schema.sql                                    │
│  • V1_0_7__Create_revoked_tokens_table.sql                       │
│  • V1_1_1__Create_user_warehouse_assignments.sql                 │
│  • V1_2_0__Add_user_activity_tracking.sql                        │
│  • V1_2_2__Add_audit_fields_to_labels.sql                        │
│                                                                   │
│  Tablas principales:                                             │
│  • users, products, warehouses, periods                          │
│  • labels, label_counts, folio_requests                          │
│  • inventory_stock, multiwarehouse_existences                    │
│  • audit_logs, revoked_tokens, user_warehouse_assignments        │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔐 FLUJO DE AUTENTICACIÓN

```
CLIENTE                              SERVIDOR
  │                                     │
  │  1. POST /api/sigmav2/auth/login    │
  │     {email, password}               │
  ├────────────────────────────────────>│
  │                                     │
  │                          ┌──────────────────────────┐
  │                          │ UserDetailsServicePer    │
  │                          │ .login(email, password)  │
  │                          └───────┬──────────────────┘
  │                                  │
  │                          ┌───────▼──────────────────┐
  │                          │ Validar credenciales     │
  │                          │ • Buscar usuario por email
  │                          │ • Comparar password hash │
  │                          │ • Verificar cuenta activa│
  │                          └───────┬──────────────────┘
  │                                  │
  │                          ┌───────▼──────────────────┐
  │                          │ ¿Válido?                 │
  │                          └───────┬──────────────────┘
  │                                  │
  │                    ┌─────────────┴──────────────┐
  │                    │                            │
  │              ✅ SÍ │                       ❌ NO │
  │                    │                            │
  │          ┌─────────▼──────┐           ┌────────▼──────┐
  │          │ Actualizar:     │           │ Respuesta:    │
  │          │ • lastLoginAt   │           │ 401 Unautho.  │
  │          │ • lastActivityAt│           │ "Credenciales │
  │          │ • Generar JWT   │           │  inválidas"   │
  │          └────────┬────────┘           └───────────────┘
  │                   │
  │          ┌────────▼──────────────────┐
  │          │ JwtUtils.createToken()    │
  │          │ • Firmar JWT con clave    │
  │          │ • Incluir: email, rol     │
  │          │ • Exp: configurable (ej:  │
  │          │   24 horas)               │
  │          └────────┬──────────────────┘
  │                   │
  │          ┌────────▼──────────────────┐
  │          │ ResponseAuthDTO:          │
  │          │ {                         │
  │          │   accessToken: JWT,       │
  │          │   tokenType: "Bearer",    │
  │          │   expiresIn: 86400        │
  │          │ }                         │
  │          └────────┬──────────────────┘
  │                   │
  │  2. Response 200  │
  │     {token...}    │
  │<────────────────────────────────────┤
  │                                     │
  │                           ✅ Token almacenado en cliente
  │
  │
  │  3. Siguientes requests:            │
  │     GET /api/sigmav2/users/me       │
  │     Authorization: Bearer JWT       │
  ├────────────────────────────────────>│
  │                                     │
  │                      ┌──────────────────────────┐
  │                      │ JwtAuthenticationFilter  │
  │                      │ 1. Extrae token del      │
  │                      │    header Authorization  │
  │                      │ 2. Valida firma del JWT  │
  │                      │ 3. Verifica no está      │
  │                      │    revocado              │
  │                      └───────┬──────────────────┘
  │                              │
  │                    ┌─────────▼──────────┐
  │                    │ ¿Token válido?     │
  │                    └─────────┬──────────┘
  │                              │
  │                ┌─────────────┴──────────────┐
  │                │                            │
  │          ✅ SÍ │                       ❌ NO │
  │                │                            │
  │  ┌─────────────▼───────────┐    ┌──────────▼──────┐
  │  │ Crear Authentication:   │    │ Respuesta:      │
  │  │ • Principal = email     │    │ 401 Unauthorized│
  │  │ • Authorities = rol     │    │ "Token inválido"│
  │  │ • Guardar en Security   │    └─────────────────┘
  │  │   ContextHolder         │
  │  └─────────────┬───────────┘
  │                │
  │       ┌────────▼──────────────┐
  │       │ UserActivityFilter    │
  │       │ • Obtiene email del   │
  │       │   Authentication      │
  │       │ • Actualiza           │
  │       │   lastActivityAt      │
  │       │ • Guarda en BD        │
  │       └────────┬───────────────┘
  │                │
  │       ┌────────▼──────────────┐
  │       │ Controlador           │
  │       │ (ej: UserController.  │
  │       │  getCurrentUser())     │
  │       │ • Procesa request     │
  │       │ • Retorna datos       │
  │       └────────┬───────────────┘
  │                │
  │  4. Response 200│
  │     {user...}   │
  │<────────────────────────────────────┤
  │                                     │
  │
  │  5. LOGOUT:                         │
  │     POST /api/auth/logout           │
  ├────────────────────────────────────>│
  │                                     │
  │                   ┌─────────────────────────┐
  │                   │ TokenRevocationService  │
  │                   │ • Extraer token         │
  │                   │ • Agregar a tabla       │
  │                   │   revoked_tokens        │
  │                   │ • Con exp_time          │
  │                   └────────┬────────────────┘
  │                            │
  │  6. Response 200           │
  │     "Logged out"           │
  │<────────────────────────────────────┤
  │                                     │
  │  7. Siguiente request con mismo token:
  │     GET /api/sigmav2/users/me       │
  ├────────────────────────────────────>│
  │                                     │
  │                   ┌─────────────────────────┐
  │                   │ JwtRevocationFilter     │
  │                   │ • Busca token en        │
  │                   │   revoked_tokens        │
  │                   │ • Encontrado ❌          │
  │                   └────────┬────────────────┘
  │                            │
  │  8. Response 401           │
  │     "Token revoked"        │
  │<────────────────────────────────────┤
  │                                     │

```

---

## 📊 FLUJO PRINCIPAL DEL SISTEMA

### Vista de Alto Nivel: Verificación Física vs Teórica

```
                    PERIODO CONTABLE NUEVO
                             │
                             ↓
        ┌────────────────────────────────────┐
        │ 1. POST /api/sigmav2/periods       │
        │    Crear período contable          │
        │    (ej: "Auditoría Marzo 2026")    │
        └────────────┬───────────────────────┘
                     │
                     ↓
        ┌────────────────────────────────────┐
        │ 2. POST /api/sigmav2/inventory/    │
        │    import                          │
        │    Subir: inventario.xlsx          │
        │    • Clave del producto            │
        │    • Nombre                        │
        │    • Descripción                   │
        │    → Crear catálogo de productos   │
        └────────────┬───────────────────────┘
                     │
                     ↓
        ┌────────────────────────────────────┐
        │ 3. POST /api/sigmav2/multiwarehouse│
        │    /import                         │
        │    Subir: multialmacen.xlsx        │
        │    • Código warehouse              │
        │    • Clave producto                │
        │    • Cantidad teórica              │
        │    → Llena tabla inventory_stock   │
        │      (caché de existencias)        │
        └────────────┬───────────────────────┘
                     │
                     ├─── VERIFICACIÓN TEÓRICA ────────┐
                     │                                  │
                     ↓                                  ↓
        ┌────────────────────────────┐   ┌─────────────────────────┐
        │ 4. Solicitar Folios        │   │ BD: inventory_stock     │
        │ POST /api/sigmav2/labels/  │   │ • producto_id           │
        │ request                    │   │ • warehouse_id          │
        │ • period_id                │   │ • cantidad_teórica      │
        │ • warehouse_id             │   └─────────────────────────┘
        │ • folios_requested: N      │
        │ → Crea folio_requests      │
        └────────────┬───────────────┘
                     │
                     ↓
        ┌────────────────────────────────────┐
        │ 5. Generar Marbetes                │
        │ POST /api/sigmav2/labels/          │
        │ generate                           │
        │ • period_id                        │
        │ • warehouse_id                     │
        │ • qty_per_label (ej: 100)          │
        │                                    │
        │ Para CADA producto en warehouse:   │
        │ ├─ Crear Labels (marbetes)         │
        │ ├─ Asignar folios consecutivos     │
        │ ├─ Copiar cantidad teórica         │
        │ │  (de inventory_stock)            │
        │ ├─ Estado: PENDING_PRINT           │
        │ └─ Guardar en tabla labels         │
        │                                    │
        │ → N etiquetas individuales listas  │
        └────────────┬───────────────────────┘
                     │
                     ↓
        ┌────────────────────────────────────┐
        │ 6. Imprimir Marbetes               │
        │ POST /api/sigmav2/labels/print     │
        │ • folio_start: 001                 │
        │ • folio_end: 100                   │
        │                                    │
        │ Para CADA marbete en rango:        │
        │ ├─ Usar JasperReports              │
        │ ├─ Diseño PDF (./reports/label.jr) │
        │ ├─ Generar PDF con código de       │
        │ │  barras (folio)                  │
        │ ├─ Guardar en BD:                  │
        │ │  labels_printed (fecha_impresión)│
        │ ├─ Estado: PRINTED                 │
        │ └─ Enviar a impresora física       │
        │                                    │
        │ ✅ Marbetes impresos físicamente   │
        └────────────┬───────────────────────┘
                     │
         ┌───────────┴──────────────────────────┐
         │                                      │
         │      INICIO: VERIFICACIÓN FÍSICA     │
         │      (Contador va al almacén)        │
         │                                      │
         ↓                                      ↓
    ┌────────────────────┐          ┌──────────────────────┐
    │ 7. Conteo Físico   │          │ BD antes del conteo: │
    │    C1 (Primer)     │          │ • label.qty_teorica  │
    │ POST /api/sigmav2/ │          │ • label.qty_c1: NULL │
    │ labels/counts/c1   │          │ • label.qty_c2: NULL │
    │                    │          └──────────────────────┘
    │ • folio: 001       │
    │ • cantidad_contada │
    │   (ej: 95)         │
    │ • comentarios: ... │
    │                    │
    │ Actualiza:         │
    │ • labels.qty_c1    │
    │ • label_counts     │
    │ • Estado: C1_DONE  │
    │                    │
    │ ⚠️ Si qty_c1 ≠     │
    │   qty_teórica      │
    │ → Registrar        │
    │   diferencia para  │
    │   análisis         │
    └────────────────────┘
         │
         ↓
    ┌────────────────────┐
    │ 8. Conteo Físico   │
    │    C2 (Segundo)    │
    │ POST /api/sigmav2/ │
    │ labels/counts/c2   │
    │                    │
    │ • folio: 001       │
    │ • cantidad_contada │
    │   (ej: 95 ó 96)    │
    │ • comentarios: ... │
    │                    │
    │ Actualiza:         │
    │ • labels.qty_c2    │
    │ • Estado: C2_DONE  │
    │                    │
    │ ⚠️ Si qty_c1 ≠     │
    │   qty_c2:          │
    │ → Revisar y        │
    │   conteo final     │
    │   (C2 tiene peso)  │
    │                    │
    │ • labels.qty_final │
    │   = qty_c2 (o prom)│
    └────────────────────┘
         │
         ↓
    ┌────────────────────────────────────┐
    │ 9. Reportes                        │
    │ GET /api/sigmav2/labels/reports/.. │
    │                                    │
    │ Variantes:                         │
    │ • /by-product (qty, diferencias)   │
    │ • /summary (totales por almacén)   │
    │ • /discrepancies (desajustes)      │
    │ • /pdf (PDF del reporte)           │
    │                                    │
    │ Campos por marbete:                │
    │ • Folio                            │
    │ • Producto                         │
    │ • Teórico (qty_teorica)            │
    │ • C1 (qty_c1)                      │
    │ • C2 (qty_c2)                      │
    │ • Final (qty_final)                │
    │ • Diferencia (|teórico - final|)   │
    │ • % Varianza                       │
    └────────────┬───────────────────────┘
                 │
                 ↓
    ┌────────────────────────────────────┐
    │ 10. Generar Archivo Final          │
    │ POST /api/sigmav2/labels/          │
    │ generate-file                      │
    │                                    │
    │ Genera: existencias_finales.txt    │
    │                                    │
    │ Formato:                           │
    │ Clave|Nombre|CantidadFinal|Almacén│
    │ PROD001|Widget A|95|ALM01          │
    │ PROD002|Widget B|200|ALM01         │
    │ ...                                │
    │                                    │
    │ Ubicación:                         │
    │ C:\Sistemas\SIGMA\Documentos\      │
    │ existencias_finales.txt            │
    │                                    │
    │ ✅ Archivo listo para auditoría   │
    └────────────────────────────────────┘
                 │
                 ✅ CICLO COMPLETO DE VERIFICACIÓN
```

---

## 🔄 FLUJO DETALLADO POR MÓDULO

### A. MÓDULO DE USUARIOS (Autenticación y Seguridad)

```
USUARIOS
  ├─ REGISTRO (POST /api/sigmav2/users/register)
  │  ├─ Input: { email, password, nombre, apellido }
  │  ├─ Validar email único
  │  ├─ Hash password con BCrypt
  │  ├─ Crear BeanUser con status=false (no verificado)
  │  ├─ Enviar código de verificación por email (MailModule)
  │  └─ Output: { id, email, createdAt }
  │
  ├─ VERIFICACIÓN (POST /api/sigmav2/users/verify)
  │  ├─ Input: { email, verificationCode }
  │  ├─ Validar código
  │  ├─ Actualizar: user.verified=true, user.verificationCode=null
  │  ├─ Guardar en BD
  │  └─ Output: Success message
  │
  ├─ LOGIN (POST /api/sigmav2/auth/login)
  │  ├─ Input: { email, password }
  │  ├─ Validar credenciales
  │  ├─ ¿Bloqueado por intentos fallidos? → Rechazar
  │  ├─ ¿No verificado? → Rechazar
  │  ├─ Generar JWT token
  │  ├─ Actualizar: lastLoginAt, lastActivityAt
  │  ├─ Resetear intentos fallidos
  │  └─ Output: { accessToken, tokenType, expiresIn }
  │
  ├─ LOGOUT (POST /api/auth/logout)
  │  ├─ Input: JWT token en header
  │  ├─ Extraer token
  │  ├─ Agregar a tabla revoked_tokens
  │  ├─ Job de purga cada 1 hora
  │  └─ Output: Success message
  │
  ├─ PROFILE (GET /api/sigmav2/users/me)
  │  ├─ Input: JWT token en header
  │  ├─ Obtener email del token
  │  ├─ Buscar usuario en BD
  │  ├─ Obtener datos personales (personal_information)
  │  ├─ Mapear BeanUser → User (dominio)
  │  ├─ Mapear User → UserResponse (DTO)
  │  └─ Output: { id, email, nombre, rol, almacenes, ... }
  │
  ├─ CAMBIAR CONTRASEÑA (POST /api/sigmav2/users/change-password)
  │  ├─ Input: { oldPassword, newPassword }
  │  ├─ Validar old password
  │  ├─ Hash new password con BCrypt
  │  ├─ Actualizar: passwordHash, password_changed_at
  │  ├─ Revocar todos los tokens (logout forzado)
  │  └─ Output: { message: "Password updated" }
  │
  ├─ RECUPERAR CONTRASEÑA (POST /api/sigmav2/auth/createRequest)
  │  ├─ Input: { email }
  │  ├─ Buscar usuario
  │  ├─ Crear RequestRecoveryPassword (request_recovery_password tabla)
  │  ├─ Generar código único
  │  ├─ Enviar enlace con código (MailModule)
  │  ├─ Status: PENDING
  │  └─ Output: Success message
  │
  ├─ VERIFICAR RECUPERACIÓN (POST /api/sigmav2/auth/verifyUser)
  │  ├─ Input: { email, recoveryCode }
  │  ├─ Buscar RequestRecoveryPassword
  │  ├─ Validar código no expirado
  │  ├─ Status: VERIFIED
  │  └─ Output: { recoveryToken (temporal) }
  │
  ├─ RESET PASSWORD (POST /api/sigmav2/auth/resetPassword)
  │  ├─ Input: { recoveryToken, newPassword }
  │  ├─ Validar recovery token
  │  ├─ Hash new password
  │  ├─ Actualizar user.passwordHash
  │  ├─ Status en RequestRecoveryPassword: COMPLETED
  │  ├─ Revocar todos los tokens
  │  └─ Output: { message: "Password reset successful" }
  │
  └─ GESTIÓN DE ADMIN (solo ADMINISTRADOR)
     ├─ GET /api/sigmav2/users (listar todos)
     ├─ PUT /api/sigmav2/users/{id} (actualizar)
     ├─ DELETE /api/sigmav2/users/{id} (desactivar)
     ├─ POST /api/sigmav2/users/{id}/reset-attempts (desbloquear)
     └─ POST /api/sigmav2/users/{id}/warehouse-assignments (asignar almacenes)
```

### B. MÓDULO DE PERÍODOS

```
PERÍODOS (Contexto temporal)
  ├─ CREAR (POST /api/sigmav2/periods)
  │  ├─ Input: { name, description, fecha_inicio, fecha_fin }
  │  ├─ Validar fecha_fin > fecha_inicio
  │  ├─ Crear Period (dominio)
  │  ├─ Mapear a BeanPeriod (JPA)
  │  ├─ Guardar en tabla periods
  │  └─ Output: { id, name, createdAt }
  │
  ├─ LISTAR (GET /api/sigmav2/periods)
  │  ├─ Input: página, size
  │  ├─ Query: SELECT * FROM periods ORDER BY createdAt DESC
  │  ├─ Paginar resultados
  │  └─ Output: Page<PeriodResponse>
  │
  ├─ OBTENER (GET /api/sigmav2/periods/{id})
  │  ├─ Input: period_id
  │  ├─ Buscar en BD
  │  ├─ Mapear a response DTO
  │  └─ Output: { id, name, description, fechas, stats }
  │
  ├─ ACTUALIZAR (PUT /api/sigmav2/periods/{id})
  │  ├─ Input: { name, description, ... }
  │  ├─ Validar sin conflictos con otras operaciones
  │  ├─ Actualizar registro
  │  └─ Output: { id, name, updatedAt }
  │
  └─ CERRAR (POST /api/sigmav2/periods/{id}/close)
     ├─ Input: period_id
     ├─ Validar que todos los conteos estén completos
     ├─ Set status = CLOSED
     ├─ Generar resumen final
     └─ Output: { message: "Period closed", stats }
```

### C. MÓDULO DE INVENTARIO (Catálogo)

```
INVENTARIO (Productos)
  ├─ IMPORTAR (POST /api/sigmav2/inventory/import)
  │  ├─ Input: archivo inventario.xlsx
  │  ├─ Parser Excel (Apache POI):
  │  │  ├─ Leer filas
  │  │  ├─ Mapear: { clave, nombre, descripción, ... }
  │  │  └─ Validar estructura
  │  ├─ Para CADA fila:
  │  │  ├─ ¿Producto ya existe? → Actualizar
  │  │  ├─ ¿Producto nuevo? → Crear
  │  │  ├─ Crear Product (dominio)
  │  │  ├─ Mapear a BeanProduct (JPA)
  │  │  └─ Guardar en tabla products
  │  ├─ Registrar en import_log:
  │  │  ├─ archivo_nombre
  │  │  ├─ cantidad_registros
  │  │  ├─ timestamp
  │  │  ├─ usuario_id
  │  │  └─ estado: SUCCESS/FAILED
  │  └─ Output: { importedCount, errors[] }
  │
  ├─ LISTAR (GET /api/sigmav2/inventory/products)
  │  ├─ Input: search, page, size
  │  ├─ Query: SELECT * FROM products
  │  │          WHERE nombre LIKE ? OR clave LIKE ?
  │  │          ORDER BY nombre
  │  ├─ Paginar
  │  └─ Output: Page<ProductResponse>
  │
  ├─ OBTENER (GET /api/sigmav2/inventory/products/{id})
  │  ├─ Input: product_id
  │  ├─ Buscar en BD
  │  ├─ Incluir: categoría, historial precios, etc
  │  └─ Output: ProductDetailResponse
  │
  ├─ BUSCAR (GET /api/sigmav2/inventory/search)
  │  ├─ Input: { q: "widget", almacén_id, ... }
  │  ├─ Full-text search en nombre + descripción
  │  ├─ Filtrar por almacén
  │  └─ Output: Product[]
  │
  └─ STOCK EN BD (inventory_stock - Caché)
     ├─ Se actualiza por MultiWarehouse.import()
     ├─ Estructura: { product_id, warehouse_id, cantidad_teórica }
     ├─ Índices: (product_id, warehouse_id) UNIQUE
     └─ Sirve para: generar marbetes con cantidades
```

### D. MÓDULO MULTIALMACÉN (Existencias Teóricas)

```
MULTIALMACÉN (Stock teórico de múltiples almacenes)
  ├─ IMPORTAR (POST /api/sigmav2/multiwarehouse/import)
  │  ├─ Input: archivo multialmacen.xlsx
  │  ├─ Parser Excel:
  │  │  ├─ Leer filas: { warehouse_code, product_clave, qty }
  │  │  ├─ Validar estructura
  │  │  └─ Mapear a MultiWarehouseExistence
  │  ├─ Para CADA fila:
  │  │  ├─ Buscar warehouse por código
  │  │  ├─ Buscar product por clave
  │  │  ├─ ¿Ya existe en inventory_stock? → Actualizar cantidad
  │  │  ├─ ¿Nuevo? → Crear registro
  │  │  ├─ Guardar en inventory_stock
  │  │  └─ Registrar en multiwarehouse_existences (histórico)
  │  ├─ Registrar en import_log
  │  └─ Output: { totalRecords, successCount, errors[] }
  │
  ├─ STOCK ACTUAL (GET /api/sigmav2/multiwarehouse/stock)
  │  ├─ Input: { product_id, warehouse_id, period_id }
  │  ├─ Query: SELECT qty FROM inventory_stock WHERE ...
  │  ├─ Incluir: producto, almacén, actualizado_en
  │  └─ Output: { product, warehouse, qty_teórica, fecha_actualización }
  │
  └─ HISTORIAL (GET /api/sigmav2/multiwarehouse/history)
     ├─ Input: { product_id, warehouse_id, desde, hasta }
     ├─ Query: SELECT * FROM multiwarehouse_existences
     │          WHERE fecha BETWEEN desde AND hasta
     ├─ Ordenar por fecha DESC
     └─ Output: MultiWarehouseExistence[]
```

### E. MÓDULO DE MARBETES (Núcleo del Sistema)

```
MARBETES (Etiquetas de verificación)
  │
  ├─ 1. SOLICITAR FOLIOS (POST /api/sigmav2/labels/request)
  │  ├─ Input: { period_id, warehouse_id, qty_folios_solicitados }
  │  ├─ Validar: period existe, warehouse existe, qty > 0
  │  ├─ Buscar folio_inicio disponible (último folio + 1)
  │  ├─ Calcular folio_fin = folio_inicio + qty - 1
  │  ├─ Crear FolioRequest:
  │  │  ├─ period_id
  │  │  ├─ warehouse_id
  │  │  ├─ folio_inicio
  │  │  ├─ folio_fin
  │  │  ├─ qty_requested
  │  │  ├─ status: PENDING
  │  │  └─ created_at
  │  ├─ Guardar en tabla folio_requests
  │  └─ Output: { id, folios_start, folios_end, qty }
  │
  ├─ 2. GENERAR MARBETES (POST /api/sigmav2/labels/generate)
  │  ├─ Input: { period_id, warehouse_id, qty_per_label (ej: 100) }
  │  ├─ Validar: FolioRequest esté PENDING
  │  ├─ Obtener folios del rango solicitado
  │  ├─ Obtener stock teórico de inventory_stock
  │  │  └─ Por: warehouse_id, period_id
  │  ├─ Para CADA producto con stock en warehouse:
  │  │  ├─ Calcular cantidad de marbetes:
  │  │  │  └─ N_marbetes = ceil(qty_teórica / qty_per_label)
  │  │  │
  │  │  ├─ Para CADA marbete a crear:
  │  │  │  ├─ Asignar folio consecutivo (folio++)
  │  │  │  ├─ Crear Label (dominio):
  │  │  │  │  ├─ folio
  │  │  │  │  ├─ product_id
  │  │  │  │  ├─ warehouse_id
  │  │  │  │  ├─ period_id
  │  │  │  │  ├─ qty_teórica (de inventory_stock)
  │  │  │  │  ├─ qty_c1: NULL
  │  │  │  │  ├─ qty_c2: NULL
  │  │  │  │  ├─ qty_final: NULL
  │  │  │  │  ├─ status: PENDING_PRINT
  │  │  │  │  ├─ created_at
  │  │  │  │  └─ created_by
  │  │  │  │
  │  │  │  └─ Guardar en tabla labels
  │  │  │
  │  │  └─ ✅ N marbetes creados para producto
  │  │
  │  ├─ Actualizar FolioRequest:
  │  │  ├─ status: PROCESSED
  │  │  └─ processed_at
  │  │
  │  └─ Output: { generatedLabels: N, totalFolios, errors[] }
  │
  ├─ 3. IMPRIMIR MARBETES (POST /api/sigmav2/labels/print)
  │  ├─ Input: { folio_start, folio_end }
  │  ├─ Validar rango existe en tabla labels
  │  ├─ Query: SELECT * FROM labels
  │  │          WHERE folio BETWEEN folio_start AND folio_end
  │  │          AND status IN (PENDING_PRINT, PENDING_REPRINT)
  │  │
  │  ├─ Para CADA label en rango:
  │  │  ├─ Usar JasperReports:
  │  │  │  ├─ Template: ./resources/reports/label.jrxml
  │  │  │  ├─ Parámetros:
  │  │  │  │  ├─ folio (convertir a barcode)
  │  │  │  │  ├─ producto_nombre
  │  │  │  │  ├─ producto_clave
  │  │  │  │  ├─ warehouse_nombre
  │  │  │  │  ├─ qty_teórica
  │  │  │  │  └─ fecha_impresión
  │  │  │  └─ Generar PDF
  │  │  │
  │  │  ├─ Actualizar Label:
  │  │  │  ├─ status: PRINTED
  │  │  │  ├─ fecha_impresión: NOW()
  │  │  │  ├─ impreso_por: usuario_actual
  │  │  │  └─ cantidad_impresiones++
  │  │  │
  │  │  └─ Guardar en BD
  │  │
  │  ├─ Archivar PDF:
  │  │  ├─ Ubicación: C:\Sistemas\SIGMA\Documentos\pdfs\{folio}.pdf
  │  │  └─ Permiso solo lectura (auditoría)
  │  │
  │  ├─ Enviar a impresora:
  │  │  ├─ Si PrintService disponible
  │  │  └─ Rastrear estatus
  │  │
  │  └─ Output: { printedCount, pdfLocation, errors[] }
  │
  ├─ 4. CANCELAR MARBETE (POST /api/sigmav2/labels/{id}/cancel)
  │  ├─ Input: label_id, motivo
  │  ├─ Validar: label no está C1_DONE
  │  ├─ Crear registro en labels_cancelled:
  │  │  ├─ label_id
  │  │  ├─ folio
  │  │  ├─ motivo
  │  │  ├─ conteo1_al_cancelar (qty_c1 de label original)
  │  │  ├─ conteo2_al_cancelar (qty_c2 de label original)
  │  │  ├─ cancelado_por: usuario_actual
  │  │  ├─ cancelled_at: NOW()
  │  │  └─ status: CANCELLED
  │  │
  │  ├─ Marcar Label como CANCELLED
  │  ├─ Auditoría: registrar cancelación
  │  └─ Output: { message: "Label cancelled", archivedData }
  │
  ├─ 5. REGISTRAR CONTEO C1 (POST /api/sigmav2/labels/counts/c1)
  │  ├─ Input: { folio, qty_contada, comentarios }
  │  ├─ Validar: folio existe
  │  ├─ Obtener Label
  │  ├─ Validar: status NO sea CANCELLED
  │  ├─ Actualizar Label:
  │  │  ├─ qty_c1 = qty_contada
  │  │  ├─ status: C1_DONE
  │  │  ├─ c1_contador = usuario_actual
  │  │  └─ c1_fecha = NOW()
  │  │
  │  ├─ Crear evento en label_count_events:
  │  │  ├─ label_id
  │  │  ├─ tipo_evento: C1_REGISTERED
  │  │  ├─ qty_valor: qty_contada
  │  │  ├─ comentarios
  │  │  ├─ registrado_por: usuario_actual
  │  │  ├─ registrado_en: NOW()
  │  │  ├─ previous_value: NULL
  │  │  └─ updated_at: NULL
  │  │
  │  ├─ Auditoría: @Auditable(action="REGISTER_C1")
  │  └─ Output: { labelId, folio, qty, status: C1_DONE }
  │
  ├─ 6. ACTUALIZAR C1 (PUT /api/sigmav2/labels/{id}/counts/c1)
  │  ├─ Input: { folio, new_qty, justificación }
  │  ├─ Validar: folio existe, usuario tiene permisos (ADMIN)
  │  ├─ Obtener Label
  │  ├─ Validar: qty_c1 ya registrada
  │  ├─ Guardar en label_count_events:
  │  │  ├─ tipo_evento: C1_UPDATED
  │  │  ├─ previous_value: old_qty_c1
  │  │  ├─ qty_valor: new_qty
  │  │  ├─ actualizado_por: usuario_actual
  │  │  ├─ updated_at: NOW()
  │  │  └─ comentarios (justificación)
  │  │
  │  ├─ Actualizar Label.qty_c1 = new_qty
  │  ├─ Auditoría: @Auditable(action="UPDATE_C1")
  │  └─ Output: { labelId, oldQty, newQty, updatedAt }
  │
  ├─ 7. REGISTRAR CONTEO C2 (POST /api/sigmav2/labels/counts/c2)
  │  ├─ Input: { folio, qty_contada, comentarios }
  │  ├─ Validar: folio existe, qty_c1 ya está registrada
  │  ├─ Obtener Label
  │  ├─ Actualizar Label:
  │  │  ├─ qty_c2 = qty_contada
  │  │  ├─ status: C2_DONE
  │  │  ├─ c2_contador = usuario_actual
  │  │  └─ c2_fecha = NOW()
  │  │
  │  ├─ Calcular qty_final:
  │  │  ├─ SI qty_c1 == qty_c2: qty_final = qty_c1
  │  │  ├─ SI qty_c1 != qty_c2:
  │  │  │  ├─ Si diferencia < 5%: qty_final = promedio(C1, C2)
  │  │  │  └─ Si diferencia >= 5%: qty_final = qty_c2 (tiene peso)
  │  │  └─ qty_final se guarda en BD
  │  │
  │  ├─ Crear evento en label_count_events:
  │  │  ├─ tipo_evento: C2_REGISTERED
  │  │  ├─ qty_valor: qty_contada
  │  │  ├─ qty_final (calculada)
  │  │  ├─ registrado_por: usuario_actual
  │  │  └─ registrado_en: NOW()
  │  │
  │  ├─ Auditoría: @Auditable(action="REGISTER_C2")
  │  └─ Output: { labelId, folio, qty_c2, qty_final, status: C2_DONE }
  │
  ├─ 8. ACTUALIZAR C2 (PUT /api/sigmav2/labels/{id}/counts/c2)
  │  ├─ Input: { folio, new_qty, justificación }
  │  ├─ Similar a actualizar C1
  │  ├─ Recalcular qty_final
  │  └─ Output: { labelId, oldQty, newQty, newQtyFinal, updatedAt }
  │
  ├─ 9. LISTAR PARA CONTEO (GET /api/sigmav2/labels/for-counting)
  │  ├─ Input: { period_id, warehouse_id, folio_start, folio_end }
  │  ├─ Query: SELECT * FROM labels
  │  │          WHERE period_id = ? AND warehouse_id = ?
  │  │          AND folio BETWEEN start AND end
  │  │          AND status IN (PRINTED, C1_DONE)
  │  │          ORDER BY folio
  │  │
  │  ├─ Mapear a LabelForCountResponse:
  │  │  ├─ folio
  │  │  ├─ producto (nombre + clave)
  │  │  ├─ qty_teórica
  │  │  ├─ qty_c1 (si existe)
  │  │  ├─ diferencia_c1 (teórica - c1)
  │  │  ├─ estado_c1 (REGISTERED / PENDING)
  │  │  └─ estado_c2 (REGISTERED / PENDING / N/A)
  │  │
  │  └─ Output: Page<LabelForCountResponse>
  │
  ├─ 10. REPORTES
  │   ├─ BY PRODUCT (GET /api/sigmav2/labels/reports/by-product)
  │   │  ├─ Input: { period_id, warehouse_id }
  │   │  ├─ Query: SELECT product, COUNT(*) as total,
  │   │  │                SUM(qty_teórica), SUM(qty_c1), SUM(qty_c2)
  │   │  │          FROM labels GROUP BY product
  │   │  │
  │   │  ├─ Calcular: diferencias, porcentajes
  │   │  └─ Output: ProductReportResponse[]
  │   │
  │   ├─ SUMMARY (GET /api/sigmav2/labels/reports/summary)
  │   │  ├─ Input: { period_id, warehouse_id }
  │   │  ├─ Query: SELECT COUNT(*), SUM(qty_*) FROM labels
  │   │  ├─ Incluir: marbetes generados, impresos, contados
  │   │  └─ Output: SummaryReportResponse
  │   │
  │   ├─ DISCREPANCIES (GET /api/sigmav2/labels/reports/discrepancies)
  │   │  ├─ Input: { period_id, warehouse_id, min_variance % }
  │   │  ├─ Query: SELECT * FROM labels
  │   │  │          WHERE ABS(qty_teórica - qty_final) / qty_teórica > min_variance
  │   │  │          ORDER BY varianza DESC
  │   │  │
  │   │  ├─ Mostrar: folio, producto, teórica, final, diferencia
  │   │  └─ Output: DiscrepancyReportResponse[]
  │   │
  │   └─ PDF (GET /api/sigmav2/labels/reports/pdf)
  │      ├─ Input: { period_id, warehouse_id, tipo_reporte }
  │      ├─ Generar con JasperReports (reportes/reporte.jrxml)
  │      ├─ Guardar en C:\Sistemas\SIGMA\Documentos\reportes\
  │      └─ Output: PDF binary
  │
  └─ 11. GENERAR ARCHIVO FINAL (POST /api/sigmav2/labels/generate-file)
     ├─ Input: { period_id, warehouse_id }
     ├─ Query: SELECT producto.clave, producto.nombre, labels.qty_final,
     │                warehouse.nombre
     │          FROM labels
     │          JOIN productos ON labels.product_id = productos.id
     │          JOIN warehouses ON labels.warehouse_id = warehouses.id
     │          WHERE labels.period_id = period_id
     │          AND labels.status = C2_DONE
     │          ORDER BY clave
     │
     ├─ Generar archivo TXT:
     │  ├─ Cabecera: Reporte de Existencias Finales
     │  ├─ Metadata: fecha, almacén, período
     │  ├─ Columnas: Clave|Nombre|CantidadFinal|Diferencia|%Varianza
     │  ├─ Para CADA marbete:
     │  │  └─ PROD001|Widget A|95|5|-5%
     │  │
     │  └─ Pie: Total items verificados, total cantidad
     │
     ├─ Guardar: C:\Sistemas\SIGMA\Documentos\
     │           existencias_finales_{date}.txt
     │
     ├─ Permisos: Read-only (auditoría)
     └─ Output: { filePath, recordsIncluded, totalQty, checksum }
```

---

## 🗄️ INTERACCIÓN DE TABLAS DE BD

```
RELACIONES PRINCIPALES:

users (Principal)
  ├─ 1:N → user_warehouse_assignments (Qué almacenes puede acceder)
  ├─ 1:1 → personal_information (Datos adicionales)
  ├─ 1:N → audit_logs (Qué acciones realizó)
  ├─ 1:N → user_activity_log (LOGIN, LOGOUT, BLOCKED)
  └─ 1:N → request_recovery_password (Solicitudes recuperación)

periods (Período contable)
  ├─ 1:N → folio_requests (Solicitudes de folios)
  ├─ 1:N → labels (Marbetes creados)
  ├─ 1:N → inventory_stock (Stock teórico para este período)
  └─ 1:N → multiwarehouse_existences (Histórico de importaciones)

products (Catálogo)
  ├─ 1:N → inventory_stock (Stock en BD)
  ├─ 1:N → labels (Marbetes por producto)
  └─ 1:N → import_log (Rastreo de importación)

warehouses (Almacenes)
  ├─ 1:N → user_warehouse_assignments (Usuarios asignados)
  ├─ 1:N → labels (Marbetes por almacén)
  ├─ 1:N → folio_requests (Folios solicitados)
  └─ 1:N → inventory_stock (Stock teórico)

folio_requests (Solicitudes de folios)
  ├─ N:1 → periods
  ├─ N:1 → warehouses
  ├─ N:1 → users (quien solicitó)
  └─ 1:N → labels (marbetes generados)

labels (Marbetes)
  ├─ N:1 → periods
  ├─ N:1 → warehouses
  ├─ N:1 → products
  ├─ N:1 → users (quien creó)
  ├─ 1:N → label_count_events (Histórico de conteos)
  ├─ 1:1 → labels_cancelled (Si se canceló)
  └─ 1:1 → labels_printed (Si se imprimió)

label_count_events (Eventos de conteo)
  ├─ N:1 → labels
  ├─ N:1 → users (quien registró el conteo)
  └─ Almacena: C1_REGISTERED, C2_REGISTERED, C1_UPDATED, C2_UPDATED

labels_cancelled (Marbetes cancelados)
  ├─ 1:1 → labels
  ├─ N:1 → users (quien canceló)
  └─ Almacena: motivo, conteos_al_cancelar (auditoría)

revoked_tokens (Tokens invalidados)
  └─ 1:N → users
     └─ Almacena: token, exp_time (para purga automática)

audit_logs (Auditoría)
  └─ N:1 → users
     └─ Almacena: acción, recurso, IP, timestamp, resultado

inventory_stock (Caché de stock teórico)
  ├─ N:1 → products
  ├─ N:1 → warehouses
  ├─ N:1 → periods
  └─ Se actualiza: POST /multiwarehouse/import
     Se consulta: POST /labels/generate (para llenar qty_teórica)
```

---

## 🔄 ESTADOS Y TRANSICIONES

```
ESTADO DE FOLIO_REQUEST:

  PENDING
    ↓ [POST /labels/generate con folio request ID]
    → PROCESSED
       └─ No se puede generar de nuevo (idempotencia)

ESTADO DE LABEL (Marbete):

  PENDING_PRINT
    ├─ [POST /labels/print] → PRINTED
    │  └─ Se genera PDF, se asigna fecha_impresión
    │
    ├─ [POST /labels/{id}/cancel] → CANCELLED
    │  └─ Se archiva en labels_cancelled
    │
    └─ [POST /labels/counts/c1] → C1_DONE (si qty_teórica es válida)
       │
       ├─ [POST /labels/counts/c2] → C2_DONE
       │  └─ Se calcula qty_final
       │
       └─ [POST /labels/{id}/cancel] → CANCELLED (antes de C2)

  PRINTED
    ├─ [POST /labels/counts/c1] → C1_DONE
    │
    └─ [POST /labels/{id}/cancel] → CANCELLED

  C1_DONE
    ├─ [POST /labels/counts/c2] → C2_DONE
    │
    └─ [POST /labels/{id}/cancel] → CANCELLED (aún permitido)

  C2_DONE
    ├─ [PUT /labels/{id}/counts/c2] → C2_DONE (actualizar)
    │  └─ Se recalcula qty_final
    │
    ├─ Status final (Ya no puede cambiar a PRINTED ni C1_DONE)
    │
    └─ [POST /labels/{id}/cancel] → NO PERMITIDO

  CANCELLED
    └─ Registrado en labels_cancelled
    └─ No se puede reactivar

CICLO DE VIDA COMPLETO:

  PENDING_PRINT
      ↓ [Imprimir]
      PRINTED
      ↓ [Contar C1]
      C1_DONE
      ↓ [Contar C2]
      C2_DONE ✅ Final

O:

  PENDING_PRINT
      ↓ [Cancelar]
      CANCELLED ✅ Final
```

---

## 🚨 FLUJO DE ERRORES Y EXCEPCIONES

```
ERROR HANDLING ARQUITECTURA:

1. NIVEL FILTRO (Security):
   ├─ JwtAuthenticationFilter
   │  ├─ Respuesta: 401 Unauthorized
   │  └─ Body: { error: "Invalid token", message: "..." }
   │
   ├─ UserActivityFilter
   │  ├─ Si BD cae: Log WARN, pero NO bloquea request
   │  └─ Body: Request continúa normalmente
   │
   └─ CORS Filter
      ├─ Respuesta: 403 Forbidden
      └─ Body: { error: "CORS policy violated" }

2. NIVEL CONTROLADOR:
   ├─ Validación de Input (@Valid)
   │  ├─ Respuesta: 400 Bad Request
   │  └─ Body: { errors: [{field, message}, ...] }
   │
   ├─ Recurso no encontrado
   │  ├─ Respuesta: 404 Not Found
   │  └─ Body: { error: "Resource not found", id: 123 }
   │
   └─ Operación no permitida
      ├─ Respuesta: 403 Forbidden
      └─ Body: { error: "Access denied", reason: "..." }

3. NIVEL SERVICIO:
   ├─ Violación de reglas de negocio
   │  ├─ Excepción personalizada (ej: InvalidLabelStateException)
   │  ├─ Respuesta: 422 Unprocessable Entity
   │  └─ Body: { error: "Invalid operation", details: "..." }
   │
   ├─ Conflicto de estado
   │  ├─ Respuesta: 409 Conflict
   │  └─ Body: { error: "Label already C1_DONE" }
   │
   └─ Recurso agotado
      ├─ Respuesta: 429 Too Many Requests
      └─ Body: { error: "Rate limit exceeded" }

4. NIVEL BD:
   ├─ Constraint violation
   │  ├─ Respuesta: 400 Bad Request (en general)
   │  └─ Body: { error: "Invalid data", constraint: "..." }
   │
   ├─ Deadlock
   │  ├─ Spring JPA reintenta automáticamente
   │  ├─ Si persiste: Respuesta: 500 Internal Server Error
   │  └─ Body: { error: "Database deadlock" }
   │
   └─ Conexión perdida
      ├─ Respuesta: 500 Internal Server Error
      └─ Body: { error: "Database connection failed" }

5. GLOBAL @RestControllerAdvice:
   ├─ Cualquier Exception no capturada
   ├─ Respuesta: 500 Internal Server Error
   ├─ Body: { error: "Internal server error", timestamp, path }
   └─ Log: Completo en console y archivo

AUDITORÍA DE ERRORES:
  └─ Todo error se registra en audit_logs
     ├─ usuario_id
     ├─ acción_intentada
     ├─ resultado: ERROR
     ├─ error_message (sin detalles sensibles)
     ├─ error_code
     ├─ timestamp
     ├─ ip_address
     └─ path_solicitado

EJEMPLO DE FLUJO DE ERROR:

  Cliente: POST /api/sigmav2/labels/counts/c1
           { folio: 999, qty: 50 }  (folio no existe)

  ↓

  UserController.registerCountC1()
    ├─ @Valid valida el request ✅
    ├─ Llama: labelService.registerCountC1(999, 50)
    │
    └─ LabelApplicationService.registerCountC1()
       ├─ labelRepository.findByFolio(999)
       ├─ Resultado: Optional.empty() → Lanza LabelNotFoundException
       │
       └─ @ExceptionHandler(LabelNotFoundException.class)
          ├─ Log: ERROR "Label folio 999 not found"
          ├─ Auditoría: { usuario, acción: "REGISTER_C1", resultado: ERROR }
          ├─ Respuesta: 404 Not Found
          └─ Body: {
               error: "Not found",
               message: "Label with folio 999 not found",
               timestamp: 2026-03-23T14:30:00,
               path: "/api/sigmav2/labels/counts/c1"
             }

  ↓ (si status = CANCELLED)

  labelRepository.findByFolio(999) ✅ Encontrado
    ├─ Valida: label.status != CANCELLED ❌ FALSO
    ├─ Lanza: InvalidLabelStateException("Label already cancelled")
    │
    └─ @ExceptionHandler(InvalidLabelStateException.class)
       ├─ Log: WARN "Attempt to count cancelled label"
       ├─ Auditoría: { resultado: ERROR }
       ├─ Respuesta: 422 Unprocessable Entity
       └─ Body: {
            error: "Invalid state",
            message: "Label already cancelled",
            allowedStates: ["PRINTED", "C1_DONE"],
            currentState: "CANCELLED"
          }
```

---

## 📈 RESUMEN DE FLUJO TOTAL

```
Usuario                 Aplicación              BD
  │                        │                     │
  ├─ POST /login ────────→ │                     │
  │                        ├─ UserDetailsService │
  │                        │                     ├─ SELECT user
  │                        │                     ← Respuesta
  │                        ├─ Hash compare       │
  │                        ├─ Generate JWT       │
  │                        │                     ├─ UPDATE lastLoginAt
  │  ← JWT Token ──────────┤                     │
  │                        │                     ├─ INSERT audit_log
  │                        │                     ← OK
  │
  ├─ GET /labels/... ────→ │                     │
  │ Headers: Auth: JWT     ├─ JwtAuthFilter      │
  │                        ├─ UserActivityFilter ├─ UPDATE lastActivityAt
  │                        │                     ← OK
  │                        ├─ Controller         │
  │                        │                     ├─ SELECT labels
  │                        │                     ← Datos
  │  ← Response 200 ───────┤                     │
  │  [labels data]         │                     │
  │                        │                     │
  ├─ POST /logout ───────→ │                     │
  │                        ├─ TokenRevocation    ├─ INSERT revoked_tokens
  │  ← Success ────────────┤                     ← OK
```

---

**Estado del Documento:** ✅ Completo  
**Última Actualización:** 2026-03-23  
**Versión:** 1.0 (Maestro)


