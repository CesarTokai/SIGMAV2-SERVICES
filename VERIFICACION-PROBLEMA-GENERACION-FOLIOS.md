# 🔍 Script de Verificación: ¿Se generaron TODOS los marbetes?

```sql
-- Verificar TODOS los marbetes generados
SELECT
    l.folio,
    p.cve_art as clave_producto,
    p.descr as producto,
    w.warehouse_key as clave_almacen,
    w.name_warehouse as almacen,
    COALESCE(inv.exist_qty, 0) as existencias,
    l.estado
FROM labels l
INNER JOIN product p ON l.id_product = p.id_product
INNER JOIN warehouse w ON l.id_warehouse = w.id_warehouse
LEFT JOIN inventory_stock inv ON inv.id_product = l.id_product
    AND inv.id_warehouse = l.id_warehouse
    AND inv.id_period = l.id_period
WHERE l.id_period = 16
    AND l.id_warehouse = 1
ORDER BY l.folio;

-- Contar total de marbetes generados
SELECT COUNT(*) as total_marbetes
FROM labels
WHERE id_period = 16 AND id_warehouse = 1;

-- Verificar solicitudes de folios
SELECT
    p.cve_art,
    lr.requested_labels,
    lr.folios_generados,
    COALESCE(inv.exist_qty, 0) as existencias
FROM label_requests lr
INNER JOIN product p ON lr.id_product = p.id_product
LEFT JOIN inventory_stock inv ON inv.id_product = lr.id_product
    AND inv.id_warehouse = lr.id_warehouse
    AND inv.id_period = lr.id_period
WHERE lr.id_period = 16
    AND lr.id_warehouse = 1
ORDER BY p.cve_art;
```

**Ejecuta estas queries para verificar:**

1. La primera query mostrará TODOS los folios generados
2. La segunda dirá cuántos marbetes hay en total
3. La tercera mostrará qué se solicitó vs qué se generó

---

## 📊 Análisis del Problema

### Datos que Compartiste:

**Productos que SOLICITARON folios (con existencias 0):**
```
EQUIPO         → 0 folios solicitados (existencias: 0)
FactGlob       → 0 folios solicitados (existencias: 0)
GM17CRTC1      → 0 folios solicitados (existencias: 0)
GM17CRTCJ      → 0 folios solicitados (existencias: 0)
GM17MEXC1      → 0 folios solicitados (existencias: 0)
...
```

**Productos que SÍ solicitaron folios:**
```
GM17CRTB8 → 5 folios (existencias: 55)     ✓ Aparece
GM17CWMB2 → 5 folios (existencias: 8,430)  ✓ Aparece
GM17MEXB8 → 5 folios (existencias: 516)    ✓ Aparece
GM17WLMB8 → 5 folios (existencias: 29,274) ✓ Aparece
```

---

## 🎯 PROBLEMA IDENTIFICADO

El problema es que **muchos productos tienen `Folios Solicitados = 0`**, por lo tanto:

- NO se generan marbetes (porque no se solicitaron folios)
- Solo aparecen los que SÍ solicitaron folios (5 cada uno)

Esto es **correcto** según el flujo del sistema:

```
1. Usuario solicita 0 folios → No se genera nada
2. Usuario solicita 5 folios → Se generan 5 marbetes
```

---

## ✅ Verificación

Los 4 productos que aparecen son **exactamente** los que solicitaron folios:

| Producto | Folios Solicitados | Folios Generados | Rango |
|----------|-------------------|------------------|-------|
| GM17CRTB8 | 5 | 5 | 1-5 |
| GM17CWMB2 | 5 | 5 | 6-10 |
| GM17MEXB8 | 5 | 5 | 11-15 |
| GM17WLMB8 | 5 | 5 | 16-20 |

**Total:** 20 marbetes (5+5+5+5)

---

## 🔍 Lo Que Debes Verificar

### 1. ¿Se solicitaron folios para todos?

Revisa la tabla de solicitudes. Si ves:
- `Folios Solicitados: 0` → **No se generan marbetes**
- `Folios Solicitados: 5` → **Se generan 5 marbetes**

### 2. ¿El cambio funciona?

El cambio que hicimos fue:
- **NO cancelar automáticamente** marbetes sin existencias

Pero si **no se solicitan folios** (requested_labels = 0), entonces **no hay nada que generar**.

---

## 💡 ¿Cuál es tu Expectativa?

### Opción A: Generar Automáticamente Folios Según Existencias

Si quieres que el sistema genere **automáticamente** folios basados en existencias:

```
Producto con existencias > 0 → Generar X folios automáticamente
Producto con existencias = 0 → No generar folios
```

**Esto requeriría:**
- Lógica automática de cálculo de folios
- Regla de negocio clara (¿cuántos folios por producto?)

### Opción B: Usuario Decide Cuántos Folios (Actual)

Sistema actual:
```
Usuario solicita 5 folios → Se generan 5 marbetes
Usuario solicita 0 folios → No se genera nada
```

---

## 🎯 ¿Qué Quieres Hacer?

**Escenario 1:** Si quieres que aparezcan MÁS productos en la lista de marbetes:
- Debes **solicitar folios para esos productos** primero
- El sistema generará los marbetes solo cuando se soliciten

**Escenario 2:** Si crees que SÍ se solicitaron folios pero no aparecen:
- Ejecuta las queries de verificación arriba
- Revisa la tabla `label_requests`
- Verifica que `requested_labels > 0`

---

## 🔧 Solución Inmediata

Si quieres que aparezcan todos los productos, debes:

1. **Ir a la interfaz de solicitud de folios**
2. **Seleccionar los productos** que quieres incluir
3. **Especificar cuántos folios** quieres para cada uno
4. **Guardar la solicitud**
5. **Generar los marbetes**

Entonces SÍ aparecerán en la lista.

---

## ❓ Pregunta Clave

**¿Esperabas que aparecieran productos para los cuales NO se solicitaron folios?**

- Si es SÍ → Necesitamos agregar lógica automática de solicitud
- Si es NO → El sistema está funcionando correctamente

**¿Cuál es tu expectativa?**

