# Solución: Error 500 al Actualizar Conteo C2

## 🐛 Problema Identificado

Al intentar actualizar el segundo conteo (C2) mediante el endpoint `PUT /api/sigmav2/labels/counts/c2`, se produce un error 500 (Internal Server Error).

```
PUT http://localhost:8080/api/sigmav2/labels/counts/c2 500 (Internal Server Error)
```

---

## ✅ Solución Implementada

Se han realizado las siguientes mejoras:

### 1. Manejo de Excepciones Mejorado en el Controlador

**Archivo modificado:** `LabelsController.java`

Se agregó manejo detallado de excepciones con bloques try-catch específicos:

```java
@PutMapping("/counts/c2")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR_DE_CONTEO')")
public ResponseEntity<?> updateCountC2(@Valid @RequestBody UpdateCountDTO dto) {
    Long userId = getUserIdFromToken();
    String userRole = getUserRoleFromToken();

    log.info("Actualizando conteo C2 para folio {} por usuario {} con rol {}", 
        dto.getFolio(), userId, userRole);
    log.info("Request body: folio={}, countedValue={}, observaciones={}", 
        dto.getFolio(), dto.getCountedValue(), dto.getObservaciones());

    try {
        LabelCountEvent ev = labelService.updateCountC2(dto, userId, userRole);
        log.info("✅ Conteo C2 actualizado exitosamente para folio {}", dto.getFolio());
        return ResponseEntity.ok(ev);
        
    } catch (LabelNotFoundException e) {
        log.warn("❌ Folio no encontrado o sin C2: {}", e.getMessage());
        return ResponseEntity.status(404)
            .body(Map.of(
                "error", "Conteo no encontrado",
                "message", e.getMessage()
            ));
            
    } catch (InvalidLabelStateException e) {
        log.warn("❌ Estado inválido: {}", e.getMessage());
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", "Estado inválido",
                "message", e.getMessage()
            ));
            
    } catch (PermissionDeniedException e) {
        log.warn("❌ Permiso denegado: {}", e.getMessage());
        return ResponseEntity.status(403)
            .body(Map.of(
                "error", "Permiso denegado",
                "message", e.getMessage()
            ));
            
    } catch (Exception e) {
        log.error("❌ Error inesperado al actualizar C2 para folio {}: {}", 
            dto.getFolio(), e.getMessage(), e);
        return ResponseEntity.status(500)
            .body(Map.of(
                "error", "Error interno del servidor",
                "message", "Error al actualizar el conteo C2: " + e.getMessage(),
                "details", e.getClass().getSimpleName()
            ));
    }
}
```

### 2. Logging Detallado en el Servicio

**Archivo modificado:** `LabelServiceImpl.java`

Se agregó logging extensivo para diagnosticar el problema:

```java
public LabelCountEvent updateCountC2(UpdateCountDTO dto, Long userId, String userRole) {
    log.info("🔄 Iniciando actualización de conteo C2 para folio {}", dto.getFolio());
    log.debug("Parámetros: folio={}, countedValue={}, observaciones={}, userId={}, userRole={}",
        dto.getFolio(), dto.getCountedValue(), dto.getObservaciones(), userId, userRole);

    try {
        // Validaciones...
        log.debug("Buscando marbete con folio {}", dto.getFolio());
        
        // Más logs durante el proceso...
        
        log.info("✅ Conteo C2 actualizado exitosamente para folio {} - Valor anterior: {}, Valor nuevo: {}", 
            dto.getFolio(), oldValue, dto.getCountedValue());
        
        return updated;
        
    } catch (PermissionDeniedException | LabelNotFoundException | InvalidLabelStateException e) {
        log.warn("Excepción controlada en updateCountC2: {}", e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("❌ Error inesperado en updateCountC2 para folio {}: {}", 
            dto.getFolio(), e.getMessage(), e);
        throw new RuntimeException("Error inesperado al actualizar C2: " + e.getMessage(), e);
    }
}
```

### 3. Import Corregido

Se agregó el import faltante de `BigDecimal`:

```java
import java.math.BigDecimal;
```

---

## 🔍 Diagnóstico del Error

Para identificar la causa del error 500, revise los logs del backend cuando se ejecuta el PUT:

```bash
# Los logs ahora mostrarán información detallada:
🔄 Iniciando actualización de conteo C2 para folio X
Parámetros: folio=X, countedValue=Y, observaciones=Z, userId=N, userRole=R
Buscando marbete con folio X
Marbete encontrado: productId=P, warehouseId=W, estado=E
...
```

---

## 🧪 Pasos para Probar

### 1. Reiniciar la Aplicación

```bash
# Detener la aplicación actual (Ctrl+C)
# Recompilar y ejecutar
mvn spring-boot:run
```

O en PowerShell con el wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

### 2. Verificar Logs Durante la Actualización

Al hacer el PUT, revise la consola del backend. Ahora verá logs detallados:

```
2026-01-22 15:59:24 INFO  - Actualizando conteo C2 para folio 123 por usuario 5 con rol ADMINISTRADOR
2026-01-22 15:59:24 INFO  - Request body: folio=123, countedValue=15.5, observaciones=null
2026-01-22 15:59:24 INFO  - 🔄 Iniciando actualización de conteo C2 para folio 123
...
2026-01-22 15:59:24 INFO  - ✅ Conteo C2 actualizado exitosamente para folio 123
```

### 3. Probar con cURL

```bash
curl -X PUT "http://localhost:8080/api/sigmav2/labels/counts/c2" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "folio": 123,
    "countedValue": 15.5,
    "observaciones": "Conteo corregido"
  }'
```

---

## 🔎 Posibles Causas del Error Original

Basándome en el código, estas son las causas más probables:

### 1. **Folio sin Conteo C2 Registrado**

Si intentas actualizar un C2 que no existe, el error original devolvería 500.

**Solución implementada:** Ahora retorna 404 con mensaje claro:

```json
{
  "error": "Conteo no encontrado",
  "message": "No existe un conteo C2 para actualizar"
}
```

### 2. **Marbete en Estado Incorrecto**

Si el marbete está CANCELADO o no está IMPRESO.

**Solución implementada:** Ahora retorna 400 con mensaje claro:

```json
{
  "error": "Estado inválido",
  "message": "No se puede actualizar conteo: el marbete está CANCELADO."
}
```

### 3. **Permisos Insuficientes**

Si el rol del usuario no tiene permisos para actualizar C2.

**Solución implementada:** Ahora retorna 403 con mensaje claro:

```json
{
  "error": "Permiso denegado",
  "message": "No tiene permiso para actualizar C2. Solo ADMINISTRADOR, ALMACENISTA o AUXILIAR_DE_CONTEO pueden actualizar el segundo conteo."
}
```

### 4. **Error de Validación de Datos**

Si `countedValue` es null o inválido.

**Solución implementada:** La validación de `@NotNull` en el DTO captura esto antes de llegar al servicio.

---

## 📋 Validaciones Pre-requisitos

Antes de actualizar un C2, asegúrese de:

### ✅ El folio existe
```sql
SELECT * FROM labels WHERE folio = 123;
```

### ✅ El C2 ya fue registrado
```sql
SELECT * FROM label_count_events 
WHERE folio = 123 AND count_number = 2;
```

### ✅ El marbete está IMPRESO (no CANCELADO)
```sql
SELECT estado FROM labels WHERE folio = 123;
-- Debe retornar 'IMPRESO'
```

### ✅ El usuario tiene permisos
Roles permitidos para actualizar C2:
- `ADMINISTRADOR`
- `ALMACENISTA`
- `AUXILIAR_DE_CONTEO`

---

## 🚨 Mensajes de Error Mejorados

### Antes (Error 500 genérico)
```json
{
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Error interno del servidor",
  "success": false,
  "timestamp": "2026-01-22T15:59:24.5350175"
}
```

### Ahora (Error específico)

#### Folio no encontrado (404)
```json
{
  "error": "Conteo no encontrado",
  "message": "No existe un conteo C2 para actualizar"
}
```

#### Estado inválido (400)
```json
{
  "error": "Estado inválido",
  "message": "No se puede actualizar conteo: el marbete está CANCELADO."
}
```

#### Permiso denegado (403)
```json
{
  "error": "Permiso denegado",
  "message": "No tiene permiso para actualizar C2. Solo ADMINISTRADOR, ALMACENISTA o AUXILIAR_DE_CONTEO pueden actualizar el segundo conteo."
}
```

#### Error inesperado (500 con detalles)
```json
{
  "error": "Error interno del servidor",
  "message": "Error al actualizar el conteo C2: [mensaje específico]",
  "details": "NullPointerException"
}
```

---

## 💡 Recomendaciones para el Frontend

### 1. Manejo de Errores Específicos

```typescript
const actualizarConteoC2 = async (folio: number, valor: number, observaciones?: string) => {
  try {
    const response = await axios.put('/labels/counts/c2', {
      folio,
      countedValue: valor,
      observaciones
    });
    
    showSuccess('Conteo C2 actualizado correctamente');
    return response.data;
    
  } catch (error) {
    if (error.response) {
      const { status, data } = error.response;
      
      switch (status) {
        case 404:
          showError('El folio no tiene un conteo C2 registrado');
          break;
        case 400:
          showError(data.message || 'El marbete no está en estado válido');
          break;
        case 403:
          showError('No tiene permisos para actualizar el segundo conteo');
          break;
        case 500:
          showError(`Error del servidor: ${data.message || 'Error desconocido'}`);
          console.error('Detalles:', data.details);
          break;
        default:
          showError('Error al actualizar el conteo');
      }
    } else {
      showError('No se pudo conectar con el servidor');
    }
  }
};
```

### 2. Validación Previa

```typescript
const validarAntesDeActualizar = async (folio: number) => {
  try {
    // Consultar información del marbete
    const label = await getLabelForCount(folio, periodId, warehouseId);
    
    // Verificar que existe C2
    if (!label.c2Value) {
      showWarning('Este folio no tiene un conteo C2 registrado');
      return false;
    }
    
    // Verificar estado
    if (label.estado !== 'IMPRESO') {
      showWarning(`El marbete está en estado ${label.estado}, no se puede actualizar`);
      return false;
    }
    
    return true;
    
  } catch (error) {
    showError('Error al validar el folio');
    return false;
  }
};

// Uso
const handleActualizar = async () => {
  const esValido = await validarAntesDeActualizar(folio);
  if (!esValido) return;
  
  await actualizarConteoC2(folio, nuevoValor, observaciones);
};
```

---

## 📊 Verificación de la Solución

### Verificar en los Logs

Después de reiniciar la aplicación, al intentar actualizar un C2, deberías ver:

```
✅ ÉXITO:
2026-01-22 16:05:00 INFO - Actualizando conteo C2 para folio 123 por usuario 5 con rol ADMINISTRADOR
2026-01-22 16:05:00 INFO - Request body: folio=123, countedValue=15.5, observaciones=Corregido
2026-01-22 16:05:00 INFO - 🔄 Iniciando actualización de conteo C2 para folio 123
2026-01-22 16:05:00 DEBUG - Parámetros: folio=123, countedValue=15.5, observaciones=Corregido, userId=5, userRole=ADMINISTRADOR
2026-01-22 16:05:00 DEBUG - Role normalizado: ADMINISTRADOR
2026-01-22 16:05:00 DEBUG - Buscando marbete con folio 123
2026-01-22 16:05:00 DEBUG - Marbete encontrado: productId=456, warehouseId=369, estado=IMPRESO
2026-01-22 16:05:00 DEBUG - Validando acceso al almacén 369
2026-01-22 16:05:00 DEBUG - Buscando evento C2 para folio 123
2026-01-22 16:05:00 DEBUG - Eventos encontrados: 2
2026-01-22 16:05:00 DEBUG - Evento C2 encontrado: id=789, countedValue=10.0
2026-01-22 16:05:00 DEBUG - Actualizando valor de 10.0 a 15.5
2026-01-22 16:05:00 INFO - ✅ Conteo C2 actualizado exitosamente para folio 123 - Valor anterior: 10.0, Valor nuevo: 15.5
```

O errores específicos:

```
❌ ERROR CONTROLADO:
2026-01-22 16:05:00 WARN - ❌ Folio no encontrado o sin C2: No existe un conteo C2 para actualizar
```

---

## 🔄 Cambios Aplicados en Otros Endpoints

Los mismos cambios se aplicaron a:

- `PUT /api/sigmav2/labels/counts/c1` - Actualizar C1
  - Mismo manejo de excepciones
  - Mismo nivel de logging

---

## 📞 Soporte

Si el error persiste después de estos cambios:

1. **Capture los logs completos** del backend al momento del error
2. **Verifique los datos del request** en el navegador (Network tab)
3. **Comparta el folio específico** que está causando el problema
4. **Verifique la base de datos** con las consultas SQL mencionadas

---

**Fecha de solución:** 2026-01-22  
**Archivos modificados:**
- `LabelsController.java`
- `LabelServiceImpl.java`

**Próximos pasos:** Reiniciar la aplicación y probar la actualización del conteo C2 con los logs mejorados.
