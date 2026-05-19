# 📊 DIAGNOSTICO: CÓMO FUNCIONA EL FILTRO DE ACCESO A ALMACENES

## 🔍 FLUJO COMPLETO DEL ERROR

```
1. USUARIO HACE REQUEST
   ↓
   POST /api/sigmav2/labels/summary
   {
     "periodId": 1,
     "warehouseId": 183,
     "page": 0,
     "size": 100
   }
   ↓
2. CONTROLLER EXTRAE CREDENCIALES
   (LabelsController.java, línea 313-316)
   
   Long userId = getUserIdFromToken();          // → userId = 2
   String userRole = getUserRoleFromToken();    // → userRole = "ALMACENISTA"
   ↓
3. LLAMA AL SERVICIO
   (LabelsController.java, línea 317)
   
   labelService.getLabelSummary(dto, userId, userRole)
   ↓
4. SERVICIO VALIDA ACCESO
   (LabelServiceImpl.java, línea 605-608)
   
   warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole)
   //                                              2,     183,         "ALMACENISTA"
   ↓
5. SERVICIO DE ACCESO VERIFICA
   (WarehouseAccessService.java, línea 32-48)
   
   a) ¿Es ADMINISTRADOR o AUXILIAR?
      roleUpper = "ALMACENISTA".toUpperCase()  // → "ALMACENISTA"
      ROLES_WITH_FULL_ACCESS.contains("ALMACENISTA")  // → FALSE ❌
      
   b) Entonces consulta la BD
      assignmentRepository.existsByUserIdAndWarehouseIdAndIsActiveTrue(2, 183)
      ↓
      EJECUTA QUERY SQL:
      ┌─────────────────────────────────────────────────────────────┐
      │ SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END      │
      │ FROM UserWarehouseAssignment u                              │
      │ WHERE u.userId = 2                                          │
      │   AND u.warehouseId = 183                                   │
      │   AND u.isActive = true                                     │
      └─────────────────────────────────────────────────────────────┘
      ↓
      RESULTADO DE LA QUERY: FALSE ❌
      (No existe registro: usuario 2 asignado al almacén 183)
      ↓
   c) Lanza excepción
      throw new PermissionDeniedException(
          "El usuario no tiene acceso al almacén 183"
      )
      ↓
6. CONTROLLER CAPTURA LA EXCEPCIÓN
   (LabelsController.java, línea 328-336)
   
   catch (PermissionDeniedException e) {
       return ResponseEntity.status(403)
           .body(Map.of(
               "error", "Permiso denegado",
               "message", "El usuario no tiene acceso al almacén 183"
           ));
   }
   ↓
7. RESPUESTA HTTP
   HTTP 403 Forbidden
   {
     "error": "Permiso denegado",
     "message": "El usuario no tiene acceso al almacén 183"
   }
```

---

## 📍 UBICACIONES EXACTAS DEL CÓDIGO

### 1️⃣ CONTROLLER (Punto de entrada)
**Archivo:** `LabelsController.java` (Líneas 313-322)

```java
@PostMapping("/summary")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
public ResponseEntity<List<LabelSummaryResponseDTO>> getLabelSummary(
    @RequestBody LabelSummaryRequestDTO dto) {
    
    Long userId = getUserIdFromToken();        // ← Extrae userId del JWT
    String userRole = getUserRoleFromToken();  // ← Extrae rol del JWT
    List<LabelSummaryResponseDTO> summary = labelService.getLabelSummary(dto, userId, userRole);
    return ResponseEntity.ok(summary);
}
```

---

### 2️⃣ SERVICIO (Valida acceso)
**Archivo:** `LabelServiceImpl.java` (Líneas 590-620)

```java
// Línea 605: Validación de acceso
try {
    log.info("Validando acceso al almacén...");
    warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);
    //                                             ↑       ↑             ↑
    //                                           userId=2 warehouseId=183 ALMACENISTA
    log.info("Acceso validado correctamente");
} catch (Exception e) {
    log.warn("Error en validateWarehouseAccess: {}", e.getMessage());
    // Si falla la validación pero es ADMINISTRADOR o AUXILIAR, permitir acceso
    if (userRole != null && 
        (userRole.equalsIgnoreCase("ADMINISTRADOR") || 
         userRole.equalsIgnoreCase("AUXILIAR"))) {
        log.info("Usuario es ADMINISTRADOR o AUXILIAR, permitiendo acceso");
    } else {
        log.error("Usuario sin acceso al almacén", e);
        throw e; // ← RE-LANZA LA EXCEPCIÓN
    }
}
```

---

### 3️⃣ SERVICIO DE ACCESO (Verifica en BD)
**Archivo:** `WarehouseAccessService.java` (Líneas 32-48)

```java
public void validateWarehouseAccess(Long userId, Long warehouseId, String userRole) {
    if (userRole == null) {
        throw new PermissionDeniedException("El rol del usuario es requerido");
    }

    String roleUpper = userRole.toUpperCase();  // → "ALMACENISTA"

    // PASO 1: ¿Tiene acceso total?
    if (ROLES_WITH_FULL_ACCESS.contains(roleUpper)) {  // → FALSE
        return;  // ← NO EJECUTA (porque no es ADMINISTRADOR ni AUXILIAR)
    }

    // PASO 2: Verificar asignación específica en BD
    boolean hasAccess = assignmentRepository.existsByUserIdAndWarehouseIdAndIsActiveTrue(
        userId,      // → 2
        warehouseId  // → 183
    );  // ← CONSULTA LA BD

    if (!hasAccess) {  // → TRUE (porque no existe la asignación)
        throw new PermissionDeniedException(
            String.format("El usuario no tiene acceso al almacén %d", warehouseId)
        );  // ← LANZA ESTA EXCEPCIÓN
    }
}
```

---

### 4️⃣ REPOSITORIO (Consulta BD)
**Archivo:** `UserWarehouseAssignmentRepository.java` (Línea 29)

```java
@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
       "FROM UserWarehouseAssignment u " +
       "WHERE u.userId = :userId " +
       "  AND u.warehouseId = :warehouseId " +
       "  AND u.isActive = true")
boolean existsByUserIdAndWarehouseIdAndIsActiveTrue(
    @Param("userId") Long userId,
    @Param("warehouseId") Long warehouseId
);
```

**Consulta SQL Generada:**
```sql
SELECT CASE WHEN COUNT(u) > 0 THEN 1 ELSE 0 END 
FROM user_warehouse_assignments u1_0 
WHERE u1_0.id_user = 2 
  AND u1_0.id_warehouse = 183 
  AND u1_0.is_active = 1
```

**Resultado esperado:** `FALSE` (porque no hay registro)

---

## 🔐 TABLA DE CONTROL DE ACCESO

**Tabla:** `user_warehouse_assignments`

```sql
CREATE TABLE user_warehouse_assignments (
    id_user       BIGINT NOT NULL,
    id_warehouse  BIGINT NOT NULL,
    is_active     TINYINT(1) DEFAULT 1,
    PRIMARY KEY (id_user, id_warehouse),
    FOREIGN KEY (id_user) REFERENCES users(id_user),
    FOREIGN KEY (id_warehouse) REFERENCES warehouses(id_warehouse)
);
```

**Registros existentes (ejemplo):**

| id_user | id_warehouse | is_active |
|---------|--------------|-----------|
| 2       | 100          | 1         |
| 2       | 102          | 1         |
| 3       | 183          | 1         |
| 2       | 183          | 0         | ← ⚠️ Desactivo

**Estado actual:**
- Usuario 2 tiene acceso a almacenes: 100, 102
- Usuario 2 NO tiene acceso a almacén: 183 ❌

---

## 🎯 CÓMO VERIFICARLO

### Opción 1: Ver registros de usuario
```sql
SELECT uwa.*, u.email, w.name_warehouse
FROM user_warehouse_assignments uwa
JOIN users u ON uwa.id_user = u.id_user
JOIN warehouses w ON uwa.id_warehouse = w.id_warehouse
WHERE u.id_user = 2 AND uwa.is_active = 1;
```

### Opción 2: Verificar almacén específico
```sql
SELECT COUNT(*) as tiene_acceso
FROM user_warehouse_assignments
WHERE id_user = 2 AND id_warehouse = 183 AND is_active = 1;
-- Resultado: 0 (no tiene acceso)
```

### Opción 3: Ver todos los almacenes del usuario
```sql
SELECT w.id_warehouse, w.name_warehouse, uwa.is_active
FROM user_warehouse_assignments uwa
JOIN warehouses w ON uwa.id_warehouse = w.id_warehouse
WHERE uwa.id_user = 2
ORDER BY w.name_warehouse;
```

---

## 🔑 ROLES ESPECIALES (ACCESO TOTAL)

En `WarehouseAccessService.java` (Línea 23):

```java
private static final List<String> ROLES_WITH_FULL_ACCESS = 
    List.of("ADMINISTRADOR", "AUXILIAR");
```

**Estos roles SALTEAN la validación de `user_warehouse_assignments`:**

- ✅ ADMINISTRADOR → Acceso a TODOS los almacenes
- ✅ AUXILIAR → Acceso a TODOS los almacenes
- ❌ ALMACENISTA → Solo almacenes asignados
- ❌ AUXILIAR_DE_CONTEO → Solo almacenes asignados

---

## 📋 RESUMEN

| Paso | Qué sucede | Código | Resultado |
|------|-----------|--------|-----------|
| 1 | Usuario hace request | LabelsController:313 | userId=2, role=ALMACENISTA |
| 2 | Valida acceso | LabelServiceImpl:605 | Llama al servicio de acceso |
| 3 | Verifica rol | WarehouseAccessService:37 | No es admin → continúa |
| 4 | Consulta BD | UserWarehouseAssignmentRepository:29 | Query devuelve FALSE |
| 5 | Lanza excepción | WarehouseAccessService:48 | PermissionDeniedException |
| 6 | Controller captura | LabelsController:328 | HTTP 403 Forbidden |

---

## 🔧 CÓMO ARREGLARLO

### ✅ SOLUCIÓN 1: Asignar el almacén al usuario

```sql
INSERT INTO user_warehouse_assignments (id_user, id_warehouse, is_active)
VALUES (2, 183, 1);
```

### ✅ SOLUCIÓN 2: Cambiar el rol a ADMINISTRADOR

```sql
UPDATE users
SET role = 'ADMINISTRADOR'
WHERE id_user = 2;
```

### ✅ SOLUCIÓN 3: Activar asignación existente (si estaba desactiva)

```sql
UPDATE user_warehouse_assignments
SET is_active = 1
WHERE id_user = 2 AND id_warehouse = 183;
```


