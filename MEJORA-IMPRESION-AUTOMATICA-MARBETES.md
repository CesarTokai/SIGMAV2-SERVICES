# Mejora: Impresión Automática de Marbetes

## Problema Identificado

El sistema anterior requería que el usuario especificara manualmente un rango de folios (inicio-fin) para imprimir marbetes. Esto causaba múltiples problemas:

### Problemas del Sistema Anterior:
1. **Rangos inconsistentes**: Se podía imprimir del folio 1 al 10, luego del 8 al 15, causando duplicados
2. **Folios omitidos**: Imprimir del 1 al 5 y luego del 7 al 10 dejaba el folio 6 sin imprimir
3. **Reimpresiones accidentales**: No había validación clara para prevenir reimpresiones
4. **Orden no garantizado**: Los folios podían imprimirse en desorden
5. **Complejidad innecesaria**: El usuario debía conocer los rangos exactos de folios disponibles

## Solución Implementada

Se rediseñó el sistema de impresión para que sea **automático y continuo**, eliminando la necesidad de especificar rangos manuales.

### Cambios Realizados

#### 1. Modificación del DTO (`PrintRequestDTO`)

**ANTES:**
```json
{
  "periodId": 16,
  "warehouseId": 369,
  "startFolio": 1,    // ❌ Requería especificar inicio
  "endFolio": 10      // ❌ Requería especificar fin
}
```

**AHORA:**
```json
{
  "periodId": 16,
  "warehouseId": 369
}
```

**Nuevas Opciones Disponibles:**

- **Impresión Automática** (más común):
```json
{
  "periodId": 16,
  "warehouseId": 369
}
```
Imprime TODOS los marbetes pendientes (estado GENERADO) del periodo y almacén.

- **Impresión por Producto**:
```json
{
  "periodId": 16,
  "warehouseId": 369,
  "productId": 123
}
```
Imprime solo marbetes pendientes de un producto específico.

- **Reimpresión Selectiva**:
```json
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [5, 10, 15],
  "forceReprint": true
}
```
Reimprime folios específicos (requiere `forceReprint: true` si ya están impresos).

#### 2. Lógica de Impresión Mejorada (`LabelServiceImpl`)

**Características:**

✅ **Automática**: El sistema busca automáticamente los marbetes pendientes
✅ **Ordenada**: Los marbetes se ordenan por folio antes de imprimir
✅ **Segura**: Valida que no estén cancelados
✅ **Controlada**: Previene reimpresiones accidentales
✅ **Flexible**: Permite reimpresión intencional con flag explícito

**Flujo de Impresión:**

1. **Modo Automático** (sin lista de folios):
   - Busca todos los marbetes con estado `GENERADO`
   - Filtra por periodo y almacén
   - Opcionalmente filtra por producto
   - Ordena por folio
   - Imprime en orden secuencial

2. **Modo Selectivo** (con lista de folios):
   - Valida que cada folio exista
   - Verifica que no estén cancelados
   - Si ya están impresos, requiere `forceReprint: true`
   - Imprime solo los folios especificados

#### 3. Nuevos Métodos en Repositorio

Se agregaron métodos para soportar la búsqueda automática:

```java
// Buscar marbetes pendientes por periodo y almacén
List<Label> findPendingLabelsByPeriodAndWarehouse(Long periodId, Long warehouseId);

// Buscar marbetes pendientes por periodo, almacén y producto
List<Label> findPendingLabelsByPeriodWarehouseAndProduct(Long periodId, Long warehouseId, Long productId);

// Buscar marbete específico
Optional<Label> findByFolioAndPeriodAndWarehouse(Long folio, Long periodId, Long warehouseId);
```

#### 4. Nombre de Archivo PDF Mejorado

**ANTES:** `marbetes_1_10.pdf`
**AHORA:** `marbetes_P16_A369_20251216_115430.pdf`

Formato: `marbetes_P{periodo}_A{almacen}_{timestamp}.pdf`

## Ejemplos de Uso

### Caso 1: Impresión Normal (Primer Lote)

**Solicitud:**
```http
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369
}
```

**Resultado:**
- Se imprimen automáticamente todos los folios pendientes (ej: 1-50)
- Se marcan como IMPRESOS
- Se genera PDF con nombre descriptivo
- Se registra en `label_print`

### Caso 2: Impresión de Producto Específico

**Solicitud:**
```http
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369,
  "productId": 123
}
```

**Resultado:**
- Solo imprime marbetes pendientes del producto 123
- Útil para impresiones parciales organizadas por producto

### Caso 3: Reimpresión por Daño/Pérdida

**Solicitud:**
```http
POST /api/sigmav2/labels/print
{
  "periodId": 16,
  "warehouseId": 369,
  "folios": [25, 26, 27],
  "forceReprint": true
}
```

**Resultado:**
- Reimprime solo los folios 25, 26 y 27
- Requiere flag `forceReprint` porque ya están impresos
- Se registra la reimpresión en `label_print`

## Reglas de Negocio

### Validaciones Automáticas:

1. ✅ **Catálogos cargados**: Verifica que existan datos de inventario
2. ✅ **Acceso al almacén**: Valida permisos según rol
3. ✅ **Marbetes no cancelados**: No permite imprimir cancelados
4. ✅ **Estado correcto**: Solo imprime GENERADOS (o IMPRESOS con forceReprint)
5. ✅ **Orden secuencial**: Siempre ordena por folio antes de imprimir

### Prevención de Errores:

- ❌ No permite rangos manuales (eliminado)
- ❌ No permite folios duplicados
- ❌ No permite saltos de folios
- ❌ No permite desorden en impresión
- ❌ No permite reimpresión sin autorización explícita

## Ventajas del Nuevo Sistema

| Aspecto | Sistema Anterior | Sistema Nuevo |
|---------|-----------------|---------------|
| **Facilidad de uso** | Usuario debe conocer rangos | Completamente automático |
| **Errores humanos** | Frecuentes (rangos incorrectos) | Eliminados |
| **Consistencia** | No garantizada | Siempre ordenado |
| **Folios omitidos** | Posible | Imposible |
| **Duplicados** | Posible | Prevenido |
| **Reimpresiones** | Sin control | Controlado con flag |
| **Trazabilidad** | Parcial | Completa |

## Migración

### Para Usuarios del Sistema:

**ANTES (necesitaban conocer los folios):**
```
1. Verificar qué folios existen
2. Calcular rango inicio-fin
3. Especificar manualmente
4. Esperar impresión
```

**AHORA (completamente automático):**
```
1. Seleccionar periodo y almacén
2. Click en "Imprimir"
3. El sistema imprime todo lo pendiente
```

### Para Desarrolladores Frontend:

**Cambio en la API:**
```javascript
// ANTES
const printRequest = {
  periodId: 16,
  warehouseId: 369,
  startFolio: 1,    // ❌ Ya no es necesario
  endFolio: 50      // ❌ Ya no es necesario
};

// AHORA
const printRequest = {
  periodId: 16,
  warehouseId: 369
  // ✅ El sistema encuentra automáticamente los folios pendientes
};
```

**Para reimpresión:**
```javascript
const reprintRequest = {
  periodId: 16,
  warehouseId: 369,
  folios: [25, 26, 27],        // Solo para reimpresión
  forceReprint: true            // Obligatorio para reimprimir
};
```

## Impacto en el Foliado Continuo

El foliado continuo ahora funciona perfectamente:

### Ejemplo Práctico:

**Almacén 1:**
- 5A + 5B + 5N = 15 marbetes generados (folios 1-15)
- Primera impresión: Se imprimen automáticamente folios 1-15

**Almacén 2:**
- 2W + 2T = 4 marbetes generados (folios 16-19)
- Segunda impresión: Se imprimen automáticamente folios 16-19

**Resultado:**
- ✅ Secuencia perfecta: 1, 2, 3, ... 19
- ✅ Sin huecos
- ✅ Sin duplicados
- ✅ Sin intervención manual

## Archivos Modificados

1. **PrintRequestDTO.java**: Eliminados startFolio/endFolio, agregados campos opcionales
2. **LabelServiceImpl.java**: Nueva lógica de impresión automática
3. **LabelsPersistenceAdapter.java**: Nuevos métodos de búsqueda
4. **JpaLabelRepository.java**: Nuevo método de consulta
5. **LabelsController.java**: Nombre de archivo PDF mejorado

## Testing

### Casos de Prueba Recomendados:

1. ✅ Imprimir todos los pendientes de un almacén
2. ✅ Imprimir marbetes de un producto específico
3. ✅ Intentar imprimir sin marbetes pendientes (debe fallar)
4. ✅ Intentar reimprimir sin forceReprint (debe fallar)
5. ✅ Reimprimir con forceReprint (debe funcionar)
6. ✅ Verificar orden secuencial en PDF
7. ✅ Validar nombre de archivo generado

## Conclusión

Esta mejora elimina completamente la complejidad y errores asociados con la especificación manual de rangos de folios. El sistema ahora es:

- ✅ Más simple de usar
- ✅ Más robusto
- ✅ Más seguro
- ✅ Más consistente
- ✅ Más fácil de mantener

El foliado continuo funciona correctamente sin intervención manual, garantizando la integridad de la secuencia de marbetes.

