# 🔧 Corrección: Generación de Marbetes por Producto (No Global)

**Fecha:** 2026-03-19  
**Problema:** La validación bloqueaba todo el período/almacén si había marbetes impresos  
**Solución:** Validar por producto individual, permitir generar productos nuevos

---

## 📋 Cambio de Lógica

### ❌ ANTES (Incorrecto - v1)
```java
// Bloqueaba TODO el periodo/almacén si había marbetes impresos
if (persistence.existsImpresosForPeriodAndWarehouse(periodId, warehouseId)) {
    throw new Exception("Bloqueado: no se pueden generar más marbetes");
}
```

**Resultado:** Si el producto 1, 2, 3 ya estaban impresos, NO podía generar el producto 4 (nuevo).

---

### ❌ ANTES (Incorrecto - v2)
```java
// Bloqueaba si el LabelRequest existía, sin importar si tenía folios
List<Long> productosYaGenerados = new ArrayList<>();
for (ProductBatchDTO product : dto.getProducts()) {
    Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
        product.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
    if (existing.isPresent()) {
        productosYaGenerados.add(product.getProductId());
    }
}
if (!productosYaGenerados.isEmpty()) {
    throw new Exception("Bloqueado: " + productosYaGenerados);
}
```

**Resultado:** Aunque "Folios Existentes = 0", no podía regenerar porque el LabelRequest existía.

---

### ✅ DESPUÉS (Correcto - v3)
```java
// Solo bloquea si foliosGenerados > 0
List<Long> productosConFoliosExistentes = new ArrayList<>();
for (ProductBatchDTO product : dto.getProducts()) {
    Optional<LabelRequest> existing = persistence.findByProductWarehousePeriod(
        product.getProductId(), dto.getWarehouseId(), dto.getPeriodId());
    
    if (existing.isPresent()) {
        LabelRequest lr = existing.get();
        // Solo bloquear si tiene folios generados (> 0)
        if (lr.getFoliosGenerados() != null && lr.getFoliosGenerados() > 0) {
            productosConFoliosExistentes.add(product.getProductId());
        }
    }
}

if (!productosConFoliosExistentes.isEmpty()) {
    throw new Exception("No se pueden regenerar: " + productosConFoliosExistentes);
}
```

**Resultado:** 
- Si `foliosGenerados = 0` → ✅ permite generar
- Si `foliosGenerados > 0` → ❌ bloquea regeneración

---

## 🎯 Comportamientos Permitidos y Bloqueados

| Escenario | LabelRequest | foliosGenerados | ¿Se permite? | Resultado |
|-----------|---|---|---|---|
| Producto nuevo, sin LabelRequest | ❌ NO | N/A | ✅ **SÍ** | Crea LabelRequest y genera folios |
| Producto existente con folios = 0 | ✅ SÍ | 0 | ✅ **SÍ** | Reutiliza LabelRequest y genera folios |
| Producto existente con folios > 0 | ✅ SÍ | 10+ | ❌ **NO** | Error: no se pueden regenerar |
| Mezcla: P1 (nuevo) + P2 (folios=0) + P3 (folios>0) | Mixed | Mixed | ❌ **NO** | Error solo por P3 |

---

## 📊 Ejemplo Real

**Paso 1:** Generar productos 1, 2, 3 en período 7, almacén 280
```
Período 7, Almacén 280:
- Producto 1: LabelRequest.foliosGenerados=10 (GENERADO)
- Producto 2: LabelRequest.foliosGenerados=10 (GENERADO)
- Producto 3: LabelRequest.foliosGenerados=10 (GENERADO)
```

**Paso 2:** Imprimir todos
```
Período 7, Almacén 280:
- Producto 1: foliosGenerados=10 (IMPRESO)
- Producto 2: foliosGenerados=10 (IMPRESO)
- Producto 3: foliosGenerados=10 (IMPRESO)
```

**Paso 3:** Error en generación de producto 2, foliosGenerados queda en 0
```
Período 7, Almacén 280:
- Producto 1: foliosGenerados=10 (IMPRESO)
- Producto 2: foliosGenerados=0  (ERROR - LabelRequest existe pero sin folios)  ← FALLO
- Producto 3: foliosGenerados=10 (IMPRESO)
```

**Paso 4:** Intentar regenerar producto 2
```
✅ PERMITIDO - foliosGenerados=0, se puede regenerar

Resultado:
- Producto 1: foliosGenerados=10 (IMPRESO)
- Producto 2: foliosGenerados=10 (GENERADO)  ← AHORA SÍ
- Producto 3: foliosGenerados=10 (IMPRESO)
```

**Paso 5:** Intentar regenerar producto 1 (que ya tiene folios=10)
```
❌ BLOQUEADO - Producto 1 ya tiene folios generados > 0

Error: "No se pueden regenerar marbetes para los productos que ya tienen folios: [1]. 
Solo se pueden generar productos sin folios existentes en este período/almacén."
```

---

## 🔐 Regla de Negocio Respetada

✅ **Cada producto tiene un único LabelRequest por período/almacén**
- No hay duplicidad de solicitudes
- No hay conflictos de secuencia de folios

✅ **Los folios son secuenciales globales por período**
- Producto 1: 100-110
- Producto 2: 111-120
- Producto 4: 131-140 (producto 3 ya está, por eso saltan a 4)

✅ **No se pueden regenerar productos ya existentes**
- Evita duplicidad de marbetes
- Mantiene integridad de conteos

---

## 🛠️ Archivos Modificados

- `LabelGenerationService.java` - Nueva lógica de validación por producto

---

## 📝 Nota Importante

**Si intentas generar un producto que ya existe:**
- El sistema rechazará la solicitud con error 400 BAD REQUEST
- Mensaje claro indicando cuáles productos ya existen
- Solo puedes generar productos nuevos en ese período/almacén

**Para regenerar un producto específico:**
- Usar endpoint de reimpresión (si existe producto IMPRESO)
- O crear nuevo período si necesitas reiniciar desde cero

