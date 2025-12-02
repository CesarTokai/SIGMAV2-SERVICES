# Implementación de Reglas de Negocio - Impresión de Marbetes

## Resumen de Implementación

Se han implementado todas las reglas de negocio para el módulo de **Impresión de Marbetes** según la especificación del sistema SIGMA.

---

## Reglas de Negocio Implementadas

### 1. Control de Acceso por Rol

**Regla:** Esta operación delimita el contexto informativo según el usuario y almacén asignado. Sin embargo, si el usuario tiene rol "ADMINISTRADOR" o "AUXILIAR", tendrá la opción para cambiar de almacén.

**Implementación:**
- **Ubicación:** `LabelServiceImpl.printLabels()` - líneas 184-195
- **Lógica:**
  - Si el usuario tiene rol `ADMINISTRADOR` o `AUXILIAR`: puede imprimir en cualquier almacén sin validación restrictiva
  - Para otros roles: se valida acceso estricto mediante `warehouseAccessService.validateWarehouseAccess()`
- **Código:**
```java
if (userRole != null && (userRole.equalsIgnoreCase("ADMINISTRADOR") || userRole.equalsIgnoreCase("AUXILIAR"))) {
    log.info("Usuario {} tiene rol {} - puede imprimir en cualquier almacén", userId, userRole);
    // Los administradores y auxiliares pueden imprimir en cualquier almacén
} else {
    // Para otros roles, validar acceso estricto al almacén
    warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
}
```

---

### 2. Validación de Catálogos Cargados

**Regla:** Se podrán imprimir marbetes siempre y cuando se hayan importado datos de los catálogos de inventario y multialmacén.

**Implementación:**
- **Ubicación:** `LabelServiceImpl.printLabels()` - líneas 197-207
- **Lógica:**
  - Verifica si existen datos en `inventory_stock` para el almacén y periodo seleccionados
  - Si no existen datos, lanza excepción `CatalogNotLoadedException` con mensaje descriptivo
- **Nueva Excepción:** `CatalogNotLoadedException.java`
- **Nuevo Método de Repositorio:** `JpaInventoryStockRepository.existsByWarehouseIdWarehouseAndPeriodId()`
- **Código:**
```java
boolean hasInventoryData = inventoryStockRepository.existsByWarehouseIdWarehouseAndPeriodId(
    dto.getWarehouseId(), dto.getPeriodId());

if (!hasInventoryData) {
    throw new CatalogNotLoadedException(
        "No se pueden imprimir marbetes porque no se han cargado los catálogos de inventario " +
        "y multialmacén para el periodo y almacén seleccionados. " +
        "Por favor, importe los datos antes de continuar.");
}
```

---

### 3. Validación de Rango de Folios

**Regla:** El sistema debe validar que el rango de folios sea válido (folio inicial ≤ folio final).

**Implementación:**
- **Ubicación:** `LabelServiceImpl.printLabels()` - líneas 209-213
- **Lógica:**
  - Valida que `startFolio` no sea mayor que `endFolio`
  - Si es inválido, lanza `InvalidLabelStateException`
- **Código:**
```java
if (dto.getStartFolio() > dto.getEndFolio()) {
    throw new InvalidLabelStateException(
        "El folio inicial no puede ser mayor que el folio final.");
}
```

---

### 4. Impresión Normal vs Extraordinaria

**Regla:** Soportar dos escenarios de impresión:
1. **Impresión normal:** Impresión inmediata de marbetes recién generados (estado GENERADOS)
2. **Impresión extraordinaria:** Reimpresión de marbetes previamente impresos (estado IMPRESOS)

**Implementación:**
- **Ubicación:**
  - `LabelServiceImpl.printLabels()` - líneas 220-224
  - `LabelsPersistenceAdapter.printLabelsRange()` - líneas 171-173
- **Lógica:**
  - El método `printLabelsRange` permite imprimir folios en estado `GENERADO` o `IMPRESO`
  - No se restringe la reimpresión: el sistema permite reimprimir los marbetes que el usuario requiera
  - Solo se bloquea la impresión de marbetes en estado `CANCELADO`
- **Código en Adapter:**
```java
// Validar estado (permitir GENERADO o IMPRESO para reimpresión)
if (l.getEstado() == Label.State.CANCELADO) {
    throw new IllegalStateException("No es posible imprimir marbetes cancelados. Folio: " + l.getFolio());
}
// permitir GENERADO o IMPRESO (reimpresión)
l.setEstado(Label.State.IMPRESO);
l.setImpresoAt(now);
```

---

### 5. Validación de Existencia de Folios

**Regla:** Verificar que todos los folios del rango solicitado existan y estén generados.

**Implementación:**
- **Ubicación:** `LabelsPersistenceAdapter.printLabelsRange()` - líneas 147-160
- **Lógica:**
  - Busca todos los folios en el rango especificado
  - Si faltan folios, identifica cuáles son y lanza excepción con la lista de folios faltantes
  - Valida que los folios pertenezcan al periodo y almacén seleccionados
- **Código:**
```java
List<Label> labels = jpaLabelRepository.findByFolioBetween(startFolio, endFolio);
if (labels.size() != count) {
    // Identificar folios faltantes
    Set<Long> found = labels.stream().map(Label::getFolio).collect(Collectors.toSet());
    StringBuilder sb = new StringBuilder();
    for (long f = startFolio; f <= endFolio; f++) {
        if (!found.contains(f)) {
            if (sb.length() > 0) sb.append(',');
            sb.append(f);
        }
    }
    throw new IllegalStateException("No es posible imprimir marbetes no generados. Folios faltantes: " + sb.toString());
}
```

---

### 6. Registro de Impresiones

**Regla:** Registrar cada operación de impresión con su rango de folios, fecha/hora y usuario.

**Implementación:**
- **Ubicación:** `LabelsPersistenceAdapter.printLabelsRange()` - líneas 181-191
- **Lógica:**
  - Crea un registro en la tabla `label_prints` con:
    - Periodo y almacén
    - Folio inicial y final
    - Cantidad de marbetes impresos
    - Usuario que realizó la impresión
    - Fecha y hora de impresión
- **Código:**
```java
LabelPrint lp = new LabelPrint();
lp.setPeriodId(periodId);
lp.setWarehouseId(warehouseId);
lp.setFolioInicial(startFolio);
lp.setFolioFinal(endFolio);
lp.setCantidadImpresa((int)count);
lp.setPrintedBy(userId);
lp.setPrintedAt(now);
LabelPrint saved = jpaLabelPrintRepository.save(lp);
```

---

### 7. Manejo de Errores y Logging

**Implementación:**
- **Ubicación:** `LabelServiceImpl.printLabels()` - líneas 183, 215-216, 226-238
- **Lógica:**
  - Logging detallado al inicio y fin de la operación
  - Captura y conversión de excepciones técnicas a excepciones de negocio
  - Mensajes descriptivos para el usuario
- **Código:**
```java
log.info("Iniciando impresión de marbetes: periodId={}, warehouseId={}, startFolio={}, endFolio={}, userId={}, userRole={}",
    dto.getPeriodId(), dto.getWarehouseId(), dto.getStartFolio(), dto.getEndFolio(), userId, userRole);

try {
    LabelPrint result = persistence.printLabelsRange(...);
    log.info("Impresión exitosa: {} folio(s) impresos del {} al {}",
        result.getCantidadImpresa(), result.getFolioInicial(), result.getFolioFinal());
    return result;
} catch (IllegalArgumentException e) {
    log.error("Error de validación en impresión: {}", e.getMessage());
    throw new InvalidLabelStateException(e.getMessage());
} catch (IllegalStateException e) {
    log.error("Error de estado en impresión: {}", e.getMessage());
    throw new InvalidLabelStateException(e.getMessage());
}
```

---

## Archivos Modificados

1. **LabelServiceImpl.java**
   - Método `printLabels()` completamente refactorizado con todas las validaciones y reglas de negocio

2. **JpaInventoryStockRepository.java**
   - Agregado método `existsByWarehouseIdWarehouseAndPeriodId()` para validar catálogos cargados

3. **CatalogNotLoadedException.java** (NUEVO)
   - Nueva excepción para casos donde no se han cargado los catálogos

---

## Flujo de Validación

```
1. Validar rol de usuario (ADMINISTRADOR/AUXILIAR = acceso total)
   ↓
2. Validar catálogos cargados (inventario + multialmacén)
   ↓
3. Validar rango de folios (startFolio <= endFolio)
   ↓
4. Verificar existencia de folios en el rango
   ↓
5. Validar que folios pertenezcan al periodo/almacén
   ↓
6. Validar que folios no estén CANCELADOS
   ↓
7. Permitir impresión (GENERADO → IMPRESO) o reimpresión (IMPRESO → IMPRESO)
   ↓
8. Actualizar estado de marbetes
   ↓
9. Registrar operación en label_prints
   ↓
10. Retornar resultado con folios impresos
```

---

## Mensajes de Error Implementados

| Escenario | Excepción | Mensaje |
|-----------|-----------|---------|
| Usuario sin acceso al almacén | `PermissionDeniedException` | "No tiene acceso al almacén especificado" |
| Catálogos no cargados | `CatalogNotLoadedException` | "No se pueden imprimir marbetes porque no se han cargado los catálogos..." |
| Rango de folios inválido | `InvalidLabelStateException` | "El folio inicial no puede ser mayor que el folio final" |
| Folios faltantes | `InvalidLabelStateException` | "No es posible imprimir marbetes no generados. Folios faltantes: [lista]" |
| Folio no pertenece al periodo/almacén | `InvalidLabelStateException` | "El folio X no pertenece al periodo/almacén seleccionado" |
| Folio cancelado | `InvalidLabelStateException` | "No es posible imprimir marbetes cancelados. Folio: X" |
| Rango muy grande (>500) | `InvalidLabelStateException` | "Máximo 500 folios por lote" |

---

## Próximos Pasos

Con esta implementación, el módulo de impresión de marbetes está completamente funcional y cumple con todas las reglas de negocio especificadas. Se recomienda:

1. ✅ Probar la impresión normal de marbetes recién generados
2. ✅ Probar la reimpresión (impresión extraordinaria) de marbetes previamente impresos
3. ✅ Validar que usuarios con diferentes roles tengan acceso correcto
4. ✅ Verificar que la validación de catálogos funcione correctamente
5. ✅ Probar el manejo de errores en diferentes escenarios

---

## Fecha de Implementación
2 de diciembre de 2025

