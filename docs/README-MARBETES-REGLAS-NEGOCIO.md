# 📋 Módulo de Marbetes - Cumplimiento de Reglas de Negocio

## 🎯 Objetivo

Este conjunto de archivos documenta y implementa las correcciones necesarias para que el módulo de Marbetes cumpla completamente con las reglas de negocio especificadas.

---

## 📚 Documentos Generados

### 1. `ANALISIS-CUMPLIMIENTO-MARBETES.md`
**Propósito:** Análisis detallado del cumplimiento de reglas de negocio

**Contenido:**
- ✅ Validaciones correctamente implementadas
- ❌ Problemas identificados con severidad
- 📊 Tabla comparativa de permisos esperados vs implementados
- 🔍 Análisis de cada regla de negocio
- 💡 Recomendaciones de corrección con código de ejemplo

**Cuándo consultarlo:** Para entender QUÉ problemas había y POR QUÉ se implementaron las soluciones.

---

### 2. `IMPLEMENTACION-REGLAS-NEGOCIO-MARBETES.md`
**Propósito:** Documentación completa de los cambios implementados

**Contenido:**
- ✅ Cambios aplicados a cada archivo
- 📝 Código antes y después
- 🔄 Flujos de validación implementados
- 🎯 Ventajas de la implementación
- 📋 Checklist de pasos para completar
- ⚙️ Configuración necesaria

**Cuándo consultarlo:** Para entender CÓMO se implementaron las soluciones y QUÉ pasos seguir.

---

### 3. `EJEMPLOS-ASIGNACION-ALMACENES.sql`
**Propósito:** Scripts SQL de referencia para gestionar asignaciones

**Contenido:**
- 📝 10 ejemplos de uso común
- INSERT, UPDATE, DELETE, SELECT
- Consultas de reporte
- Mejores prácticas
- Notas importantes

**Cuándo consultarlo:** Para asignar almacenes a usuarios o consultar asignaciones.

---

## 🚀 Guía Rápida de Implementación

### Paso 1: Revisar el Análisis
```bash
# Lee primero el análisis para entender los problemas
docs/ANALISIS-CUMPLIMIENTO-MARBETES.md
```

**Problemas identificados:**
- 🔴 **#1 - ALTA:** Falta control de acceso en endpoints
- 🟡 **#2 - MEDIA:** Validación de roles en C2 ambigua
- 🔴 **#3 - ALTA:** No se valida contexto de almacenes
- 🟡 **#4 - MEDIA:** No se valida catálogos cargados

### Paso 2: Revisar Cambios Implementados
```bash
# Lee la documentación de implementación
docs/IMPLEMENTACION-REGLAS-NEGOCIO-MARBETES.md
```

**Correcciones aplicadas:**
- ✅ **#1 CORREGIDO:** Agregados `@PreAuthorize` en todos los endpoints
- ✅ **#3 IMPLEMENTADO:** Sistema completo de almacenes asignados
- 🟡 **#2 PENDIENTE:** Requiere aclaración del stakeholder
- 🟡 **#4 PENDIENTE:** Marcado como TODO en código

### Paso 3: Ejecutar Migraciones
```bash
# Al iniciar la aplicación, Flyway ejecutará automáticamente:
# V1_1_1__Create_user_warehouse_assignments.sql

# Verificar que la migración se ejecutó correctamente
mvn spring-boot:run
# O
./mvnw spring-boot:run
```

### Paso 4: Asignar Almacenes a Usuarios
```sql
-- Usar los ejemplos del archivo EJEMPLOS-ASIGNACION-ALMACENES.sql
-- Ejemplo básico:
INSERT INTO user_warehouse_assignments (id_user, id_warehouse, assigned_by, is_active)
VALUES (5, 1, 1, TRUE);
```

### Paso 5: Actualizar Frontend
```javascript
// Agregar header X-User-Role en todas las peticiones al módulo de marbetes
const headers = {
    'X-User-Id': currentUser.id,
    'X-User-Role': currentUser.role  // ← NUEVO
};

// Ejemplo con fetch
fetch('/api/sigmav2/labels/request', {
    method: 'POST',
    headers: headers,
    body: JSON.stringify(data)
});
```

---

## 📁 Archivos Modificados

### Backend - Java

#### 1. `LabelsController.java`
**Cambios:**
- ✅ Importado `@PreAuthorize`
- ✅ Agregadas anotaciones de seguridad en todos los endpoints
- ✅ Agregado parámetro `userRole` en todos los métodos

#### 2. `LabelService.java` (Interface)
**Cambios:**
- ✅ Agregado parámetro `userRole` en todas las firmas de métodos

#### 3. `LabelServiceImpl.java`
**Cambios:**
- ✅ Inyectado `WarehouseAccessService`
- ✅ Agregadas validaciones de acceso en:
  - `requestLabels()`
  - `generateBatch()`
  - `printLabels()`
  - `registerCountC1()`
  - `registerCountC2()`
- ✅ Agregado TODO para validación de catálogos

#### 4. `UserWarehouseAssignment.java` (NUEVO)
**Descripción:** Entidad JPA para asignaciones usuario-almacén

#### 5. `UserWarehouseAssignmentRepository.java` (NUEVO)
**Descripción:** Repositorio con queries optimizados

#### 6. `WarehouseAccessService.java` (NUEVO)
**Descripción:** Servicio de validación de acceso

### Backend - SQL

#### 7. `V1_1_1__Create_user_warehouse_assignments.sql` (NUEVO)
**Descripción:** Migración Flyway para crear tabla de asignaciones

---

## 🧪 Testing Recomendado

### Test 1: Control de Acceso por Rol
```bash
# Usuario ALMACENISTA intenta solicitar marbetes → ✅ Permitido
curl -X POST http://localhost:8080/api/sigmav2/labels/request \
  -H "X-User-Id: 5" \
  -H "X-User-Role: ALMACENISTA" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"warehouseId":1,"periodId":1,"requestedLabels":10}'

# Usuario sin rol permitido intenta → ❌ 403 Forbidden
```

### Test 2: Validación de Almacenes Asignados
```bash
# ALMACENISTA con acceso al almacén 1 → ✅ Permitido
curl -X POST ... -d '{"...":"warehouseId":1}'

# ALMACENISTA sin acceso al almacén 2 → ❌ PermissionDeniedException
curl -X POST ... -d '{"...":"warehouseId":2}'
```

### Test 3: Conteo C2 Exclusivo
```bash
# AUXILIAR_DE_CONTEO registra C2 → ✅ Permitido
curl -X POST http://localhost:8080/api/sigmav2/labels/counts/c2 \
  -H "X-User-Role: AUXILIAR_DE_CONTEO"

# ALMACENISTA intenta registrar C2 → ❌ 403 Forbidden
curl -X POST ... -H "X-User-Role: ALMACENISTA"
```

---

## 📊 Matriz de Permisos Implementada

| Operación | ADMIN | AUXILIAR | ALMACENISTA | AUX_CONTEO | Validación Almacén |
|-----------|-------|----------|-------------|------------|-------------------|
| Solicitar Folios | ✅ | ✅ | ✅ | ❌ | ADMIN/AUX: No<br>Otros: Sí |
| Generar Marbetes | ✅ | ✅ | ✅ | ❌ | ADMIN/AUX: No<br>Otros: Sí |
| Imprimir | ✅ | ✅ | ✅ | ❌ | ADMIN/AUX: No<br>Otros: Sí |
| Conteo C1 | ✅ | ✅ | ✅ | ✅ | ADMIN/AUX: No<br>Otros: Sí |
| Conteo C2 | ❌ | ❌ | ❌ | ✅ | Sí |

---

## 🔧 Configuración Adicional Recomendada

### 1. Endpoint de Administración de Asignaciones (Futuro)
```java
@RestController
@RequestMapping("/api/sigmav2/admin/warehouse-assignments")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class WarehouseAssignmentController {

    @PostMapping("/users/{userId}/warehouses/{warehouseId}")
    public ResponseEntity<Void> assignWarehouse(
        @PathVariable Long userId,
        @PathVariable Long warehouseId,
        @RequestHeader("X-User-Id") Long adminId
    ) {
        // Implementar lógica de asignación
    }

    @DeleteMapping("/users/{userId}/warehouses/{warehouseId}")
    public ResponseEntity<Void> removeAssignment(...) {
        // Implementar lógica de desasignación
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<Warehouse>> getUserWarehouses(...) {
        // Implementar consulta de almacenes del usuario
    }
}
```

### 2. Handler de Excepciones Global
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PermissionDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionDenied(
        PermissionDeniedException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getMessage()));
    }
}
```

---

## 📞 Contacto y Soporte

**Documentación elaborada por:** GitHub Copilot
**Fecha:** 27 de Noviembre de 2025
**Versión:** 1.0

**Para consultas sobre:**
- Reglas de negocio → Consultar con stakeholder
- Implementación técnica → Revisar documentos en `/docs`
- Errores o bugs → Verificar logs de aplicación

---

## ✅ Checklist de Implementación Completa

- [x] Análisis de cumplimiento documentado
- [x] Control de acceso con `@PreAuthorize`
- [x] Sistema de almacenes asignados implementado
- [x] Migración de BD creada
- [x] Entidades y repositorios creados
- [x] Servicio de validación implementado
- [x] Integración en servicios de marbetes
- [x] Documentación completa
- [x] Scripts SQL de ejemplo
- [ ] Ejecutar migración en BD
- [ ] Asignar almacenes a usuarios existentes
- [ ] Actualizar frontend (agregar header X-User-Role)
- [ ] Testing de integración
- [ ] Implementar validación de catálogos (Problema #4)
- [ ] Clarificar y ajustar roles en C2 (Problema #2)
- [ ] Crear endpoints de administración de asignaciones (Opcional)

---

## 🎉 Resultado Final

El módulo de Marbetes ahora **cumple con las reglas de negocio críticas**:

✅ **Seguridad:** Control de acceso multinivel por rol
✅ **Contexto:** Usuarios solo operan en almacenes asignados
✅ **Auditoría:** Registro completo de asignaciones
✅ **Flexibilidad:** Fácil extensión para nuevos roles
✅ **Performance:** Consultas optimizadas con índices

**¡Listo para producción!** (después de completar el checklist pendiente)

