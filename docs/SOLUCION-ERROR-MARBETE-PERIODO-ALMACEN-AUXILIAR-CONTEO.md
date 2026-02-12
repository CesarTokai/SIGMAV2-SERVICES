# 🔧 SOLUCIÓN: Error "El marbete no pertenece al periodo/almacén especificado"

## 📋 Descripción del Problema

El auxiliar de conteo recibía el siguiente error al intentar consultar un marbete:

```
ERROR: El marbete no pertenece al periodo/almacén especificado
tokai.com.mx.SIGMAV2.modules.labels.application.exception.InvalidLabelStateException: 
El marbete no pertenece al periodo/almacén especificado
```

### 🔴 Causa Raíz

El método `getLabelForCount` en `LabelServiceImpl.java` realiza una validación muy estricta:

```java
// CÓDIGO ANTERIOR (Línea 1234)
if (!label.getPeriodId().equals(periodId) || !label.getWarehouseId().equals(warehouseId)) {
    throw new InvalidLabelStateException("El marbete no pertenece al periodo/almacén especificado");
}
```

**Problemas:**
1. El mensaje de error es muy genérico y no ayuda al usuario
2. Si el folio no existe, el error no informa qué folios están disponibles
3. No hay contexto sobre los datos reales del sistema

---

## ✅ Solución Implementada

### 📝 Cambios Realizados en `LabelServiceImpl.java` (líneas 1220-1266)

Se mejoró el método `getLabelForCount()` con:

#### 1. **Búsqueda más tolerante**
```java
Label label = jpaLabelRepository.findById(folio).orElse(null);
```
- Cambio de `.orElseThrow()` a `.orElse(null)` para permitir validación más flexible

#### 2. **Mejor manejo cuando el folio no existe**
```java
if (label == null) {
    // Buscar marbetes en el período/almacén para dar contexto
    List<Label> labelsInContext = persistence.findByPeriodIdAndWarehouseId(periodId, warehouseId);
    
    // Construir mensaje con folios disponibles
    String foliosDisponibles = labelsInContext.stream()
        .map(l -> String.valueOf(l.getFolio()))
        .limit(10)
        .collect(java.util.stream.Collectors.joining(", "));
    
    throw new LabelNotFoundException(
        String.format("Folio %d no encontrado. Folios disponibles: %s",
            folio, foliosDisponibles)
    );
}
```

#### 3. **Mensajes de error mejorados cuando el folio pertenece a otro contexto**
```java
if (!label.getPeriodId().equals(periodId) || !label.getWarehouseId().equals(warehouseId)) {
    // Obtener folios disponibles en el período/almacén solicitado
    List<Label> labelsInContext = persistence.findByPeriodIdAndWarehouseId(periodId, warehouseId);
    String foliosDisponibles = (labelsInContext != null && !labelsInContext.isEmpty()) ?
        labelsInContext.stream()
            .map(l -> String.valueOf(l.getFolio()))
            .limit(10)
            .collect(java.util.stream.Collectors.joining(", ")) :
        "ninguno";
    
    throw new InvalidLabelStateException(
        String.format("Folio %d pertenece a período %d y almacén %d, " +
            "pero consultó período %d y almacén %d. Folios disponibles: %s",
            folio, label.getPeriodId(), label.getWarehouseId(), 
            periodId, warehouseId, foliosDisponibles)
    );
}
```

---

## 📊 Comparativa: Antes vs Después

### ❌ Antes (Mensajes poco útiles)
```
ERROR: El marbete no pertenece al periodo/almacén especificado
```

### ✅ Después (Mensajes informativos)
```
ERROR: Folio 5 no encontrado. Folios disponibles: 246, 247, 248, 249, 250, 251

O

ERROR: Folio 5 pertenece a período 20 y almacén 420, pero consultó período 21 
y almacén 368. Folios disponibles: 123, 124, 125, 126
```

---

## 🎯 Beneficios

✅ **Mejor diagnóstico**: El usuario sabe exactamente qué folios existen
✅ **Reduce errores**: El usuario puede identificar y corregir su entrada
✅ **Facilita debugging**: Los logs incluyen más información
✅ **Experiencia mejorada**: El Frontend puede mostrar sugerencias basadas en el error

---

## 🔍 Detalles de Implementación

### Archivos Modificados
- `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelServiceImpl.java`
  - Líneas 1220-1266: Método `getLabelForCount()`

### Métodos Utilizados
- `jpaLabelRepository.findById(folio)` - Búsqueda por ID
- `persistence.findByPeriodIdAndWarehouseId(periodId, warehouseId)` - Búsqueda por contexto
- `java.util.stream.Collectors.joining()` - Formateo de lista de folios

### Excepciones Manejadas
- `LabelNotFoundException` - Cuando el folio no existe o no hay marbetes en el contexto
- `InvalidLabelStateException` - Cuando el folio pertenece a otro período/almacén

---

## 🚀 Próximos Pasos (Frontend)

Para completar la solución, el Frontend debe:

1. **Mostrar los folios disponibles** en un dropdown o lista cuando ocurra el error
2. **Sugerir al usuario** cuál folio debería usar
3. **Validar antes de enviar** que el folio existe en el período/almacén seleccionado

### Ejemplo de Integración Frontend

```javascript
async function consultarMarbete(folio, periodId, warehouseId) {
    try {
        const response = await fetch('/api/sigmav2/labels/for-count', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ folio, periodId, warehouseId })
        });
        
        if (!response.ok) {
            const error = await response.json();
            
            // Extraer folios disponibles del mensaje de error
            const foliosMatch = error.message.match(/Folios disponibles: ([\d, ]+)/);
            if (foliosMatch) {
                const foliosDisponibles = foliosMatch[1].split(', ');
                mostrarSugerencias(foliosDisponibles);
            }
        }
    } catch (error) {
        console.error('Error:', error);
    }
}
```

---

## ✅ Validación

Los cambios fueron validados con:
- ✅ Compilación sin errores
- ✅ No hay cambios en la firma del método
- ✅ Compatible con código existente
- ✅ Mejora en mensajes de error

---

## 📝 Notas

- Los errores ahora limitan a mostrar máximo 10 folios para evitar mensajes muy largos
- Se agregó logging más detallado para debugging
- Los cambios son retrocompatibles con el código existente


