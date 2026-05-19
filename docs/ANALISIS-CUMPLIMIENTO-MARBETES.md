# Análisis de Cumplimiento de Reglas de Negocio - Módulo de Marbetes

## Fecha: 27 de Noviembre de 2025

## Resumen Ejecutivo

Este documento analiza el cumplimiento de las reglas de negocio del módulo de Marbetes según la especificación proporcionada.

---

## 1. ROLES Y PERMISOS

### Reglas de Negocio Esperadas:

| Rol | Captura | Impresión | Conteo | Contexto |
|-----|---------|-----------|--------|----------|
| **Administrador** | ✓ | ✓ | ✓ | Todos los almacenes |
| **Auxiliar** | ✓ | ✓ | ✓ | Almacenes asignados |
| **Almacenista** | ✓ | ✓ | ✓ | Almacenes asignados |
| **Auxiliar de conteo** | ✗ | ✗ | ✓ | Almacenes asignados |

### Estado Actual de Implementación:

#### ✅ **ROLES DEFINIDOS CORRECTAMENTE**
```java
// LabelCountEvent.java
public enum Role {
    ADMINISTRADOR,
    ALMACENISTA,
    AUXILIAR,
    AUXILIAR_DE_CONTEO
}
```

#### ⚠️ **PROBLEMA 1: FALTA CONTROL DE ACCESO A NIVEL DE ENDPOINT**

**Situación Actual:**
```java
// LabelsController.java
@PostMapping("/request")  // SIN @PreAuthorize
public ResponseEntity<Void> requestLabels(...)

@PostMapping("/generate")  // SIN @PreAuthorize
public ResponseEntity<Void> generateBatch(...)

@PostMapping("/print")  // SIN @PreAuthorize
public ResponseEntity<LabelPrint> printLabels(...)

@PostMapping("/counts/c1")  // SIN @PreAuthorize
public ResponseEntity<LabelCountEvent> registerCountC1(...)

@PostMapping("/counts/c2")  // SIN @PreAuthorize
public ResponseEntity<LabelCountEvent> registerCountC2(...)
```

**Problema:** Todos los endpoints están protegidos únicamente con `.authenticated()` en SecurityConfig, lo que significa que cualquier usuario autenticado (sin importar su rol) puede acceder a todos los endpoints.

**Lo que debería ser:**
- **Captura** (`/request`, `/generate`): Solo ADMINISTRADOR, AUXILIAR, ALMACENISTA
- **Impresión** (`/print`): Solo ADMINISTRADOR, AUXILIAR, ALMACENISTA
- **Conteo C1** (`/counts/c1`): Todos los roles
- **Conteo C2** (`/counts/c2`): Solo AUXILIAR_DE_CONTEO

---

## 2. VALIDACIONES DE CONTEO

### Reglas de Negocio Esperadas:
1. Conteo C1 puede ser registrado por: ADMINISTRADOR, AUXILIAR, ALMACENISTA, AUXILIAR_DE_CONTEO
2. Conteo C2 SOLO puede ser registrado por: AUXILIAR_DE_CONTEO
3. No permitir C1 duplicado
4. No permitir C2 sin C1 previo
5. No permitir C2 duplicado
6. No permitir C1 si ya existe C2 (secuencia rota)

### Estado Actual de Implementación:

#### ✅ **VALIDACIONES DE C1 CORRECTAMENTE IMPLEMENTADAS**
```java
// LabelServiceImpl.java - registerCountC1()
// ✓ Verifica roles permitidos (línea 106-113)
boolean allowed = roleUpper.equals("ADMINISTRADOR") ||
                  roleUpper.equals("ALMACENISTA") ||
                  roleUpper.equals("AUXILIAR") ||
                  roleUpper.equals("AUXILIAR_DE_CONTEO");

// ✓ No permite C1 duplicado (línea 128-130)
if (persistence.hasCountNumber(dto.getFolio(), 1)) {
    throw new DuplicateCountException("El conteo C1 ya fue registrado");
}

// ✓ No permite C1 si ya existe C2 (línea 132-134)
if (persistence.hasCountNumber(dto.getFolio(), 2)) {
    throw new CountSequenceException("No se puede registrar C1 porque ya existe C2");
}
```

#### ⚠️ **PROBLEMA 2: VALIDACIÓN DE ROLES DE C2 ES DEMASIADO RESTRICTIVA**

**Situación Actual:**
```java
// LabelServiceImpl.java - registerCountC2()
if (!roleUpper.equals("AUXILIAR_DE_CONTEO")) {
    throw new PermissionDeniedException("No tiene permiso para registrar C2");
}
```

**Según la documentación:** "El objetivo de este sub-módulo es ingresar los conteos... Esta acción la pueden operar **todos los roles de usuario**"

**Contradicción detectada:** La documentación indica que C2 es realizado por "Auxiliar de conteo", pero también menciona que "todos los roles" pueden operar el conteo.

**Recomendación:** Clarificar con el stakeholder si C2 debe ser exclusivo de AUXILIAR_DE_CONTEO o si otros roles también pueden hacerlo.

#### ✅ **VALIDACIONES DE C2 CORRECTAMENTE IMPLEMENTADAS**
```java
// ✓ Debe existir C1 antes de C2 (línea 164-166)
if (!persistence.hasCountNumber(dto.getFolio(), 1)) {
    throw new CountSequenceException("No se puede registrar C2 sin C1 previo");
}

// ✓ No permite C2 duplicado (línea 169-171)
if (persistence.hasCountNumber(dto.getFolio(), 2)) {
    throw new DuplicateCountException("El conteo C2 ya fue registrado");
}
```

---

## 3. VALIDACIONES DE CAPTURA Y GENERACIÓN

### Reglas de Negocio Esperadas:
1. No se pueden solicitar folios si existen folios generados del mismo producto que no han sido impresos
2. No se pueden generar marbetes si existen marbetes generados que no han sido impresos

### Estado Actual de Implementación:

#### ✅ **CORRECTAMENTE IMPLEMENTADAS**

**Solicitud de Folios:**
```java
// LabelServiceImpl.java - requestLabels()
// ✓ Valida que no existan marbetes generados sin imprimir (línea 35-38)
boolean exists = persistence.existsGeneratedUnprintedForProductWarehousePeriod(
    dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
if (exists) {
    throw new InvalidLabelStateException(
        "Existen marbetes GENERADOS sin imprimir para este producto/almacén/periodo.");
}
```

**Generación de Marbetes:**
```java
// LabelServiceImpl.java - generateBatch()
// ✓ Verifica que exista una solicitud previa (línea 56-60)
// ✓ Verifica que haya folios pendientes por generar (línea 61-64)
```

---

## 4. VALIDACIONES DE ESTADO DE MARBETE

### Reglas de Negocio Esperadas:
1. Solo se pueden contar marbetes en estado IMPRESO
2. No se pueden contar marbetes CANCELADOS

### Estado Actual de Implementación:

#### ✅ **CORRECTAMENTE IMPLEMENTADAS**

**En ambos métodos de conteo (C1 y C2):**
```java
// Verifica que el marbete no esté cancelado
if (label.getEstado() == Label.State.CANCELADO) {
    throw new InvalidLabelStateException(
        "No se puede registrar conteo: el marbete está CANCELADO.");
}

// Verifica que el marbete esté impreso
if (label.getEstado() != Label.State.IMPRESO) {
    throw new InvalidLabelStateException(
        "No se puede registrar conteo: el marbete no está IMPRESO.");
}
```

---

## 5. CONTEXTO INFORMATIVO (ALMACENES ASIGNADOS)

### Reglas de Negocio Esperadas:
- **Administrador y Auxiliar:** Todos los almacenes
- **Almacenista y Auxiliar de conteo:** Solo almacenes asignados

### Estado Actual de Implementación:

#### ❌ **PROBLEMA 3: NO IMPLEMENTADO**

**Situación Actual:**
```java
// LabelsController.java y LabelServiceImpl.java
// NO hay validaciones de almacenes asignados por usuario
```

**Falta:**
1. Relación Usuario-Almacén en base de datos
2. Validación en cada operación que filtre por almacenes asignados según el rol
3. Queries que consideren los almacenes del usuario

**Impacto:** Un usuario con rol ALMACENISTA o AUXILIAR_DE_CONTEO podría operar sobre almacenes que no le corresponden.

---

## 6. DEPENDENCIAS DE CATÁLOGOS

### Reglas de Negocio Esperadas:
"Es necesario cargar en el sistema los catálogos de inventario y multialmacén"

### Estado Actual de Implementación:

#### ⚠️ **PROBLEMA 4: VALIDACIÓN NO VERIFICADA**

**Situación Actual:**
```java
// LabelServiceImpl.java - printLabels()
// Aquí podrían ir validaciones RBAC y verificación de catálogos cargados
return persistence.printLabelsRange(...);
```

**Comentario en código indica que falta implementar**, pero no se encontró validación explícita que verifique:
1. Que exista inventario cargado para el periodo/almacén
2. Que exista multialmacén cargado para el periodo/almacén

---

## RESUMEN DE PROBLEMAS ENCONTRADOS

| # | Problema | Severidad | Módulo Afectado |
|---|----------|-----------|-----------------|
| 1 | Falta control de acceso (@PreAuthorize) en endpoints | 🔴 ALTA | LabelsController |
| 2 | Validación de roles en C2 demasiado restrictiva vs documentación | 🟡 MEDIA | LabelServiceImpl |
| 3 | No se valida contexto de almacenes asignados por usuario | 🔴 ALTA | Todo el módulo |
| 4 | No se valida que los catálogos estén cargados antes de operar | 🟡 MEDIA | LabelServiceImpl |

---

## RECOMENDACIONES DE CORRECCIÓN

### 1. Agregar Anotaciones de Seguridad en el Controlador

```java
@RestController
@RequestMapping("/api/sigmav2/labels")
@RequiredArgsConstructor
public class LabelsController {

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> requestLabels(...)

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<Void> generateBatch(...)

    @PostMapping("/print")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")
    public ResponseEntity<LabelPrint> printLabels(...)

    @PostMapping("/counts/c1")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC1(...)

    @PostMapping("/counts/c2")
    @PreAuthorize("hasRole('AUXILIAR_DE_CONTEO')")
    public ResponseEntity<LabelCountEvent> registerCountC2(...)
}
```

### 2. Implementar Sistema de Almacenes Asignados

**a) Crear tabla de relación:**
```sql
CREATE TABLE user_warehouse_assignments (
    id_user BIGINT NOT NULL,
    id_warehouse BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_user, id_warehouse),
    FOREIGN KEY (id_user) REFERENCES users(id_user),
    FOREIGN KEY (id_warehouse) REFERENCES warehouses(id_warehouse)
);
```

**b) Agregar método de validación en servicio:**
```java
private void validateWarehouseAccess(Long userId, Long warehouseId, String role) {
    if (role.equals("ADMINISTRADOR") || role.equals("AUXILIAR")) {
        return; // Tienen acceso a todos
    }
    if (!persistence.userHasAccessToWarehouse(userId, warehouseId)) {
        throw new PermissionDeniedException(
            "No tiene acceso al almacén especificado");
    }
}
```

### 3. Validar Catálogos Cargados

```java
@Override
public LabelPrint printLabels(PrintRequestDTO dto, Long userId) {
    // Validar que exista inventario
    if (!inventoryService.existsInventoryForPeriodWarehouse(
            dto.getPeriodId(), dto.getWarehouseId())) {
        throw new InvalidStateException(
            "No existe inventario cargado para este periodo/almacén");
    }

    // Validar que exista multialmacén
    if (!multiWarehouseService.existsDataForPeriodWarehouse(
            dto.getPeriodId(), dto.getWarehouseId())) {
        throw new InvalidStateException(
            "No existe multialmacén cargado para este periodo/almacén");
    }

    return persistence.printLabelsRange(...);
}
```

---

## CONCLUSIÓN

El módulo de Marbetes tiene una **base sólida** con las validaciones de lógica de negocio (secuencia de conteos, estados, duplicados), pero presenta **deficiencias críticas** en:

1. **Seguridad y control de acceso a nivel de endpoint**
2. **Validación de contexto informativo (almacenes asignados)**
3. **Verificación de prerequisitos (catálogos cargados)**

**Prioridad de corrección:**
1. 🔴 URGENTE: Implementar @PreAuthorize en endpoints (Problema #1)
2. 🔴 URGENTE: Implementar validación de almacenes asignados (Problema #3)
3. 🟡 IMPORTANTE: Validar catálogos cargados (Problema #4)
4. 🟡 IMPORTANTE: Clarificar roles permitidos en C2 (Problema #2)

---

**Elaborado por:** GitHub Copilot
**Fecha:** 27 de Noviembre de 2025
**Versión:** 1.0

