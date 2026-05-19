# 📋 Reglas de Negocio y Validaciones — Módulo de Marbetes

> Extraído del código fuente: `LabelServiceImpl`, `LabelGenerationService`, `LabelCountService`
> Última revisión: 2026-04-14

---

## 🔄 Estados de un Marbete

```
GENERADO ──── impresión ────► IMPRESO ──── conteos C1 y C2 ────► (completo)
    │                             │
    └──── cancelación ────────────┴────► CANCELADO
                                              │
                                         reactivación
                                         (si existencias > 0)
                                              │
                                           GENERADO  (nuevamente)
```

---

## 1️⃣ Generación de Marbetes

| # | Regla | Resultado |
|---|-------|-----------|
| G1 | Producto **ya tiene folios generados** en ese período/almacén | Se **salta** ese producto (warning en log), continúa con los demás |
| G2 | **Todos** los productos de la lista ya tienen folios | ❌ Error — operación bloqueada completamente |
| G3 | Producto **no existe en catálogo** (no fue importado) | ❌ Error — lista completa bloqueada |
| G4 | Usuario sin acceso al almacén indicado | ❌ Error de permisos |
| G5 | LabelRequest existente sin folios generados | ✅ Se reutiliza y se generan los folios |
| G6 | Sin LabelRequest previo | ✅ Se crea uno nuevo automáticamente |

---

## 2️⃣ Impresión de Marbetes

| # | Regla | Resultado |
|---|-------|-----------|
| I1 | Marbete en estado **GENERADO** | ✅ Puede imprimirse → cambia a **IMPRESO** |
| I2 | Marbete en estado **IMPRESO** (impresión normal) | ❌ Error — ya fue impreso |
| I3 | Marbete en estado **CANCELADO** | ❌ Error — no se puede imprimir |
| I4 | Más de **500 marbetes** en una sola operación | ❌ Error — límite máximo superado |
| I5 | Impresión normal sin marbetes pendientes | ❌ Error — no hay marbetes en estado GENERADO |
| I6 | Folios solicitados que no existen en BD | ❌ Error — `LabelNotFoundException` |
| I7 | PDF generado vacío o nulo | ❌ Error técnico |
| I8 | Rol de usuario nulo o vacío | ❌ Error de permisos |
| I9 | Usuario sin acceso al almacén | ❌ Error de permisos |

### Reimpresión Extraordinaria

| # | Regla | Resultado |
|---|-------|-----------|
| RE1 | Requiere lista de **folios específicos** obligatoriamente | ❌ Error si no se envían folios |
| RE2 | Los folios deben estar en estado **IMPRESO** | ❌ Error si alguno no lo está |
| RE3 | Máximo **500 marbetes** por operación | ❌ Error si supera el límite |

### Reimpresión Simple (individual)

| # | Regla | Resultado |
|---|-------|-----------|
| RS1 | Marbete debe estar en estado **IMPRESO** | ❌ Error si está GENERADO o CANCELADO |
| RS2 | No cambia el estado del marbete (solo registra reimpresión) | ✅ Comportamiento esperado |

---

## 3️⃣ Conteo C1 (Primer Conteo Físico)

| # | Regla | Resultado |
|---|-------|-----------|
| C1-1 | Marbete debe estar en estado **IMPRESO** | ❌ Error si está GENERADO |
| C1-2 | Marbete **CANCELADO** | ❌ No se puede registrar conteo |
| C1-3 | C1 **ya registrado** para ese folio | ❌ `DuplicateCountException` |
| C1-4 | Ya existe C2 pero **no existe C1** (secuencia rota) | ❌ `CountSequenceException` |
| C1-5 | El folio no pertenece al **período enviado** | ❌ Error con nombre del período |
| C1-6 | El folio no pertenece al **almacén enviado** (excepto `AUXILIAR_DE_CONTEO`) | ❌ Error con nombre del almacén |
| C1-7 | El folio no existe en el sistema | ❌ `LabelNotFoundException` |
| C1-8 | Rol no permitido | ❌ Solo: ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO |

---

## 4️⃣ Conteo C2 (Segundo Conteo Físico)

| # | Regla | Resultado |
|---|-------|-----------|
| C2-1 | **C1 debe existir previo** al C2 | ❌ `CountSequenceException` |
| C2-2 | Marbete debe estar en estado **IMPRESO** | ❌ Error si no está impreso |
| C2-3 | Marbete **CANCELADO** | ❌ No se puede registrar conteo |
| C2-4 | C2 **ya registrado** para ese folio | ❌ `DuplicateCountException` |
| C2-5 | Mismas validaciones de folio, período y almacén que C1 | ❌ Igual que C1-5, C1-6, C1-7 |
| C2-6 | Rol no permitido | ❌ Solo: ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO |

---

## 5️⃣ Actualización de Conteos (C1 o C2)

| # | Regla | Resultado |
|---|-------|-----------|
| A1 | El conteo a actualizar **no existe** | ❌ `LabelNotFoundException` |
| A2 | Marbete en estado **CANCELADO** | ❌ No se puede actualizar |
| A3 | Marbete **no está IMPRESO** | ❌ No se puede actualizar |
| A4 | Guarda el valor anterior en `previousValue` | ✅ Historial preservado |
| A5 | Registra en historial: valor nuevo, valor anterior, usuario, timestamp | ✅ Auditoría completa |

---

## 6️⃣ Cancelación de Marbete

| # | Regla | Resultado |
|---|-------|-----------|
| CA1 | Campo `folio` es **obligatorio** | ❌ Error si es nulo |
| CA2 | Folio no existe en BD | ❌ `LabelNotFoundException` |
| CA3 | Marbete **ya está CANCELADO** | ❌ `LabelAlreadyCancelledException` |
| CA4 | LabelRequest con **0 folios solicitados** | ❌ No se puede cancelar |
| CA5 | Usuario sin acceso al almacén del marbete | ❌ Error de permisos |
| CA6 | Al cancelar se guarda: existencias al momento, conteos C1/C2 registrados, motivo, usuario, timestamp | ✅ Registro histórico completo |
| CA7 | Motivo de cancelación es opcional (default: `"Cancelado manualmente"`) | ✅ |

---

## 7️⃣ Reactivación de Marbete Cancelado

| # | Regla | Resultado |
|---|-------|-----------|
| R1 | Solo aplica si `existenciasActuales > 0` | Condición requerida |
| R2 | Solo si el marbete **no fue reactivado antes** | ❌ Ignorado si ya fue reactivado |
| R3 | Al reactivar → marbete vuelve a estado **GENERADO** | ✅ Listo para reimprimir |
| R4 | Se registra: usuario que reactivó, fecha de reactivación | ✅ Auditoría |

---

## ⚠️ Alertas del Sistema (no bloquean, solo informan)

| Situación | Mensaje |
|-----------|---------|
| C1 y C2 **coinciden exactamente** | `"Conteos C1 y C2 coinciden (sin diferencia)"` |
| Diferencia entre C1 y C2 **mayor al 10%** | `"Diferencia significativa detectada (>10%)"` |
| Estado IMPRESO pero **sin registros de impresión** en BD | `"Inconsistencia en base de datos detectada"` |

---

## 🔑 Control de Acceso por Rol

| Rol | Generación | Impresión | Conteo C1/C2 | Cancelación | Todos los almacenes |
|-----|:----------:|:---------:|:------------:|:-----------:|:-------------------:|
| `ADMINISTRADOR` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `AUXILIAR` | ✅ | ✅ | ✅ | ✅ | ❌ (solo asignado) |
| `ALMACENISTA` | ❌ | ✅ | ✅ | ✅ | ❌ (solo asignado) |
| `AUXILIAR_DE_CONTEO` | ❌ | ❌ | ✅ | ❌ | ✅ (sin restricción) |

> **Nota:** El acceso por almacén se valida contra la tabla `user_warehouse_assignments`.
> `AUXILIAR_DE_CONTEO` es la única excepción: puede registrar conteos en cualquier almacén sin estar asignado.

---

## 📊 Resumen del Ciclo de Vida

```
1. generateBatchList()     → Crea marbetes en estado GENERADO
2. printLabels()           → Cambia estado a IMPRESO, genera PDF
3. registerCountC1()       → Registra primer conteo físico
4. registerCountC2()       → Registra segundo conteo físico
5. (opcional) updateC1/C2  → Corrige conteos ya registrados
6. (opcional) cancelLabel  → Cancela marbete (cualquier estado)
7. (opcional) updateCancelledStock → Reactiva si hay existencias
```

---

*Fuente: `LabelServiceImpl.java`, `LabelGenerationService.java`, `LabelCountService.java`*

