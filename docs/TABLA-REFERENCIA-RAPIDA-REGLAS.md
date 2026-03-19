# 📊 TABLA DE REFERENCIA RÁPIDA - REGLAS DE NEGOCIO

## 🔍 BÚSQUEDA RÁPIDA POR OPERACIÓN

### SOLICITAR FOLIOS
```
┌─────────────────────────────────────────────────────────────────┐
│ ¿QUÉ PUEDE HACER?                                               │
├─────────────────────────────────────────────────────────────────┤
│ ✅ Ingresar cantidad > 0                                        │
│ ✅ Buscar/ordenar sin modificar BD                              │
│ ✅ Cambiar cantidad varias veces                                │
│ ✅ Poner cantidad = 0 para cancelar (si NO se generó)          │
│ ✅ Los datos se guardan automáticamente                         │
│                                                                  │
│ ❌ NO si hay marbetes generados sin imprimir                   │
│ ❌ NO cantidad decimal (solo enteros)                           │
│ ❌ NO cantidad negativa                                         │
└─────────────────────────────────────────────────────────────────┘

UBICACIÓN: /api/sigmav2/labels/request [POST]
ARCHIVO: LabelServiceImpl.requestLabels()
```

---

### GENERAR MARBETES
```
┌─────────────────────────────────────────────────────────────────┐
│ ¿QUÉ PUEDE HACER?                                               │
├─────────────────────────────────────────────────────────────────┤
│ ✅ Generar marbetes con cantidad > 0                            │
│ ✅ Generar múltiples productos en una llamada                   │
│ ✅ Folios se asignan secuencialmente                            │
│ ✅ Si genera 10, después 15 → folios 1-10 y 11-25              │
│ ✅ Reutilizar LabelRequest si foliosGenerados = 0              │
│                                                                  │
│ ❌ NO si producto YA tiene folios > 0                           │
│ ❌ NO si producto NO existe en BD                               │
│ ❌ NO si rol NO es ADMIN/AUXILIAR/ALMACENISTA                  │
│ ❌ NO si NO tiene acceso a almacén                              │
└─────────────────────────────────────────────────────────────────┘

UBICACIÓN: /api/sigmav2/labels/generate [POST]
ARCHIVO: LabelGenerationService.generateBatchList()

EJEMPLO:
- Período: 7, Almacén: 5, Producto: 10, Cantidad: 50
- Resultado: Marbetes 1-50 con estado GENERADO
- Rango folios: [1, 50]
```

---

### IMPRIMIR
```
┌─────────────────────────────────────────────────────────────────┐
│ ¿QUÉ PUEDE HACER?                                               │
├─────────────────────────────────────────────────────────────────┤
│ ✅ Imprimir todos los marbetes de período/almacén              │
│ ✅ Imprimir marbetes específicos por folio                      │
│ ✅ Imprimir por producto                                        │
│ ✅ Hasta 500 marbetes por impresión                             │
│ ✅ Reimprimir marbetes ya IMPRESOS (extraordinaria)            │
│ ✅ Generar PDF válido                                           │
│                                                                  │
│ ❌ NO si marbete NO está GENERADO                               │
│ ❌ NO si producto NO existe                                     │
│ ❌ NO si almacén NO existe                                      │
│ ❌ NO más de 500 marbetes                                       │
│ ❌ NO si rol NO es ADMIN/AUXILIAR/ALMACENISTA                  │
│ ❌ NO sin acceso a almacén                                      │
└─────────────────────────────────────────────────────────────────┘

UBICACIÓN: /api/sigmav2/labels/print [POST]
ARCHIVO: LabelServiceImpl.printLabels()

RESULTADO: PDF válido con todos los marbetes
CAMBIO: Marbetes pasan de GENERADO → IMPRESO
```

---

### CONTEO C1
```
┌─────────────────────────────────────────────────────────────────┐
│ ¿QUÉ PUEDE HACER?                                               │
├─────────────────────────────────────────────────────────────────┤
│ ROLES PERMITIDOS:                                               │
│   ✅ ADMINISTRADOR                                              │
│   ✅ AUXILIAR                                                   │
│   ✅ ALMACENISTA                                                │
│   ✅ AUXILIAR_DE_CONTEO                                         │
│                                                                  │
│ ✅ Registrar cantidad física observada                          │
│ ✅ Cambiar C1 si aún no registra C2                             │
│ ✅ Ver marbete en estado IMPRESO                                │
│                                                                  │
│ ❌ NO si marbete NO está IMPRESO                                │
│ ❌ NO si ya registró C1 (duplicado)                             │
│ ❌ NO si ya existe C2                                           │
│ ❌ NO si marbete está CANCELADO                                 │
│ ❌ NO sin acceso a almacén                                      │
└─────────────────────────────────────────────────────────────────┘

UBICACIÓN: /api/sigmav2/labels/counts/c1 [POST]
ARCHIVO: LabelServiceImpl.registerCountC1()

SECUENCIA: Solo después de IMPRIMIR
```

---

### CONTEO C2
```
┌─────────────────────────────────────────────────────────────────┐
│ ¿QUIÉN PUEDE HACER?                                             │
├─────────────────────────────────────────────────────────────────┤
│ SOLO: AUXILIAR_DE_CONTEO                                        │
│ (Este es el ÚNICO rol que puede registrar C2)                   │
│                                                                  │
│ ✅ Registrar cantidad de verificación                           │
│ ✅ Ver marbete con C1 registrado                                │
│ ✅ Confirmar o discrepancia con C1                              │
│                                                                  │
│ ❌ NO ADMIN (rechazado)                                         │
│ ❌ NO AUXILIAR (rechazado)                                      │
│ ❌ NO ALMACENISTA (rechazado)                                   │
│ ❌ NO si NO existe C1                                           │
│ ❌ NO si ya registró C2 (duplicado)                             │
│ ❌ NO si marbete NO está IMPRESO                                │
│ ❌ NO si marbete está CANCELADO                                 │
└─────────────────────────────────────────────────────────────────┘

UBICACIÓN: /api/sigmav2/labels/counts/c2 [POST]
ARCHIVO: LabelServiceImpl.registerCountC2()

SECUENCIA: SOLO después de C1 registrado
```

---

## 🎯 DECISIÓN RÁPIDA - ¿PUEDO HACER ESTO?

### "¿Puedo generar marbetes?"

**Respuesta rápida:**
```
┌─────────────────────────────────────────────┐
│ 1. ¿Tengo solicitud con cantidad > 0?       │
│    NO → Primero solicita folios             │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 2. ¿El producto ya tiene folios?            │
│    SÍ → NO, está bloqueado                  │
│    NO → Continúa                            │
├─────────────────────────────────────────────┤
│ 3. ¿El producto existe en catálogo?         │
│    NO → Error: importa catálogo primero     │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 4. ¿Mi rol es ADMIN/AUX/ALMACENISTA?       │
│    NO → Acceso denegado                     │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 5. ¿Tengo acceso a este almacén?           │
│    NO → Acceso denegado                     │
│    SÍ → ✅ PUEDES GENERAR                   │
└─────────────────────────────────────────────┘
```

### "¿Puedo imprimir marbetes?"

**Respuesta rápida:**
```
┌─────────────────────────────────────────────┐
│ 1. ¿Los marbetes están en GENERADO?         │
│    NO → Primero deben generarse             │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 2. ¿Tengo max 500 marbetes?                │
│    NO (> 500) → Divide en lotes             │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 3. ¿El producto existe en BD?              │
│    NO → Reimpórta catálogo                  │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 4. ¿El almacén existe en BD?               │
│    NO → Reimpórta almacenes                 │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 5. ¿Mi rol es ADMIN/AUX/ALMACENISTA?       │
│    NO → Acceso denegado                     │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 6. ¿Tengo acceso a este almacén?           │
│    NO → Acceso denegado                     │
│    SÍ → ✅ PUEDES IMPRIMIR                  │
└─────────────────────────────────────────────┘
```

### "¿Puedo registrar conteo C2?"

**Respuesta rápida:**
```
┌─────────────────────────────────────────────┐
│ 1. ¿Mi rol es AUXILIAR_DE_CONTEO?          │
│    NO → ❌ SOLO ese rol puede              │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 2. ¿Existe C1 registrado?                  │
│    NO → Primero registra C1                 │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 3. ¿El marbete está IMPRESO?               │
│    NO → Debe estar IMPRESO primero          │
│    SÍ → Continúa                            │
├─────────────────────────────────────────────┤
│ 4. ¿El marbete NO está CANCELADO?          │
│    SÍ (está cancelado) → Rechazado          │
│    NO → Continúa                            │
├─────────────────────────────────────────────┤
│ 5. ¿Tengo acceso a este almacén?           │
│    NO → Acceso denegado                     │
│    SÍ → ✅ PUEDES REGISTRAR C2              │
└─────────────────────────────────────────────┘
```

---

## 📍 ESTADOS DE MARBETE Y FLUJO

```
┌──────────────────────────────────────────────────────────────┐
│                     CICLO DE VIDA                            │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  GENERADO  ──[IMPRIMIR]──>  IMPRESO  ──[CANCELAR]──>  CANCELADO
│     ↑                           │                           ↑
│     │                           │                           │
│     └───────[CANCELAR]──────────┘                           │
│                                                               │
│  GENERADO → IMPRESO: Normal (C1, C2)                         │
│  IMPRESO → CANCELADO: Marbete anulado                        │
│  GENERADO → CANCELADO: Directo                               │
│                                                               │
└──────────────────────────────────────────────────────────────┘

REGLA: Los conteos C1 y C2 solo se pueden registrar en IMPRESO
```

---

## 🔐 SEGURIDAD POR ALMACÉN

```
ADMIN / AUXILIAR
└─ Pueden trabajar en CUALQUIER almacén
   └─ Sin restricción de asignación

ALMACENISTA
└─ Solo trabaja en almacenes ASIGNADOS
   └─ Validación por tabla: user_warehouse_assignments

AUXILIAR_DE_CONTEO
└─ Solo trabaja en almacenes asignados (si existen)
   └─ Validación por tabla: user_warehouse_assignments
```

---

## 📝 NOTAS IMPORTANTES

1. **Folios son únicos por período:** No se reutilizan, son secuenciales globales
2. **No hay vuelta atrás:** Una vez IMPRESO, no puede volver a GENERADO
3. **C2 requiere C1:** Siempre debe existir C1 antes de C2
4. **Auditoría completa:** Cada operación registra usuario y timestamp
5. **Validación en múltiples niveles:** BD, BD, aplicación... protección triple


