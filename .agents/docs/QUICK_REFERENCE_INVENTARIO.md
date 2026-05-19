# ⚡ QUICK REFERENCE - Módulo de Inventario (Conteo)

## 📍 Ubicación del Código

```
src/modules/
├── admin/
│   └── views/marbetesAdmin/
│       ├── ConteoMarbetes.vue         ← ADMIN contea
│       ├── ReimpresionMarbetes.vue    ← Reimprime
│       └── CancelacionMarbetes.vue    ← Cancela
├── almacenista/
│   └── views/marbetes/
│       ├── ConteoMarbetes.vue         ← Almacenista contea
│       ├── ImpresionMarbetes.vue      ← Imprime primero
│       └── ConsultaCaptura.vue        ← Solicita + genera
└── auxiliar/
    └── views/marbetes/
        ├── ConteoMarbetes.vue         ← Auxiliar registra C1
        └── ConsultaCaptura.vue        ← Ve solicitudes

Configuración:
- src/config/axiosConfig.ts            ← API con JWT
- src/store/periodoStore.ts            ← Período global
- src/utils/SweetAlert.ts              ← Notificaciones
- src/utils/errorHandlers.ts           ← Errores
```

---

## 🎯 REGLAS CLAVE DEL INVENTARIO

| Regla | ¿Qué es? | ¿Por qué? |
|-------|----------|----------|
| **Secuencia C1→C2** | C2 solo después de C1 | Verificación ordenada |
| **Marbete IMPRESO** | Solo contear IMPRESO | Seguridad: física distribuida |
| **No Cancelados** | Bloquea C1/C2 si cancelado | Integridad de datos |
| **Diferencia Auto** | `\|C1-C2\|` calculada | Detecta discrepancias |
| **Pre-carga C1** | Muestra C1 al buscar | Referencia visual |
| **Bloqueo Duplicados** | No regenerar si IMPRESO | Evita folios duplicados |
| **Almacén + Período** | Contexto de cada marbete | Separación por ubicación/tiempo |

---

## 🔄 FLUJO RÁPIDO (5 pasos)

```
1️⃣  Selecciona Período
    └─ Auto-carga del store

2️⃣  Selecciona Almacén
    └─ Solo tu almacén si ALMACENISTA

3️⃣  Busca Folio
    └─ POST /labels/for-count
    └─ Debe estar IMPRESO + no cancelado

4️⃣  Registra C1
    └─ POST /labels/counts/c1
    └─ Input C1 se bloquea (gris)

5️⃣  Registra C2
    └─ POST /labels/counts/c2
    └─ Calcula diferencia automático
    └─ Limpia formulario → siguiente folio
```

---

## 📡 ENDPOINTS DEL INVENTARIO

```typescript
// Buscar
POST /labels/for-count
Body: { folio, periodId, warehouseId }
Returns: MarbeteInventario | 404

// Registrar C1
POST /labels/counts/c1
Body: { folio, countedValue }
Returns: { message, c1Value }

// Registrar C2
POST /labels/counts/c2
Body: { folio, countedValue }
Returns: { message, c1Value, c2Value, diferencia }

// Actualizar C2
PUT /labels/counts/c2
Body: { folio, countedValue }
Returns: { message, c2Value, diferencia }
```

---

## 🔒 BLOQUEOS (STOP!)

```javascript
// ❌ No contar si CANCELADO
if (marbete.cancelado === true) STOP!

// ❌ No C2 sin C1
if (conteo1 === null && trying_C2) STOP!

// ❌ No contar si no está IMPRESO
if (estado !== 'IMPRESO') STOP!

// ❌ No contar si no seleccionó período/almacén
if (!selectedPeriodo || !selectedAlmacen) STOP!

// ❌ No generar si existe IMPRESO
if (exists_IMPRESO_for_this_period_warehouse) STOP!
```

---

## 📊 INTERFACE DEL MARBETE

```typescript
interface MarbeteInventario {
  id: number;
  folio: number;                      // 1001-9999
  
  claveProducto: string;              // "PROD-001"
  producto: string;                   // "Laptop"
  
  claveAlmacen: string;               // "ALM-01"
  almacen: string;                    // "Almacén Central"
  
  existenciasEsperadas: number;       // 100 (stock teórico)
  
  // INVENTARIO (lo importante)
  conteo1: number | null;             // Primer conteo
  conteo2: number | null;             // Segundo conteo
  diferencia: number | null;          // |C1 - C2|
  
  estado: string;                     // IMPRESO, CONTADO, etc
  cancelado: boolean;                 // true/false
}
```

---

## 🎨 ESTADOS VISUALES

```
BLOQUEADO (Input C1 ya registrado):
┌─────────────────┐
│  3,700          │  ← Gris, disabled, cursor "no-permitido"
└─────────────────┘

ACTIVO (Escribiendo):
┌─────────────────┐
│  3,|___          │  ← Blanco, editable, cursor normal
└─────────────────┘

ENFOCADO (Focus):
┌─────────────────┐
│  3,700          │  ← Borde verde, fondo verde claro
└─────────────────┘
```

---

## ⌨️ ATAJOS DE TECLADO

| Atajo | Acción |
|-------|--------|
| **Enter** | Buscar folio / Guardar conteo |
| **Tab** | Siguiente input |
| **Alt + F** | Enfoca folio + limpia |
| **Alt + L** | Limpia todo |
| **Esc** | Limpia formulario |

---

## 🔢 CÁLCULOS

```
Diferencia = Math.abs(C1 - C2)

Ejemplo 1: C1=100, C2=100
  Diferencia = 0 ✅ (Perfecto)

Ejemplo 2: C1=100, C2=98
  Diferencia = 2 ⚠️ (Revisar)

Ejemplo 3: C1=1250, C2=1248
  Diferencia = 2 ⚠️ (OK si % < 1%)

Umbral típico:
  0-2    → ✅ Normal
  3-10   → ⚠️ Revisar
  >10    → 🔴 Crítico
```

---

## 🗺️ MAPEO DE CAMPOS

```javascript
// Backend → Frontend
{
  productCode         → claveProducto
  productName         → producto
  descripcionProducto → producto
  warehouseKey        → claveAlmacen
  warehouseName       → almacen
  nombreAlmacen       → almacen
  expectedStock       → existenciasEsperadas
  count1              → conteo1
  c1                  → conteo1
  countedValue        → conteo1/2
  status              → estado
  cancelled           → cancelado
}
```

---

## 👥 PERMISOS POR ROL

| Rol | Período | Almacén | C1 | C2 | Cancelar |
|-----|---------|---------|----|----|----------|
| ADMIN | Ver todos | Ver todos | ✅ | ✅ | ✅ |
| ALMACENISTA | Ver activos | Solo suyo | ✅ | ✅ | ✅ |
| AUXILIAR | Ver activos | Solo suyo | ✅ | ❌ | ❌ |
| AUXILIAR_CONTEO | Ver activos | Solo suyo | ❌ | ✅ | ❌ |

---

## 🚨 ERRORES COMUNES

```javascript
// ❌ Intentar C2 sin C1
Error: "Registra C1 primero"

// ❌ Buscar marbete CANCELADO
Error: "Marbete Cancelado - No puede ser contado"

// ❌ Buscar marbete GENERADO (no impreso)
Error: "Solo se pueden contar IMPRESOS"

// ❌ Escribir decimales
Error: "Solo números enteros"

// ❌ Sin período seleccionado
Error: "Selecciona período primero"

// ❌ Sin almacén seleccionado
Error: "Selecciona almacén primero"

// ❌ Folio no existe
Error: "Folio no encontrado"
```

---

## 💡 TIPS RÁPIDOS

1. **Pre-carga visual**: Si C1 ya existe, se muestra en input
2. **Enfoque inteligente**: Después de C1, enfoca automático en C2
3. **Diferencia real-time**: Mientras escribes C2, ve la diferencia
4. **Formato consistente**: "3,700" siempre con separador
5. **Limpieza automática**: Después de guardar C2, limpia todo
6. **Navegación sin mouse**: Enter/Tab/Alt+F/Alt+L todo funciona
7. **Período persistente**: Se guarda en localStorage entre sesiones
8. **Búsqueda debounced**: 500ms espera antes de buscar

---

## 📍 ARCHIVOS A EDITAR

Si necesitas cambios en inventario:

```
ConteoMarbetes.vue
├─ Lógica búsqueda    → buscarMarbete()
├─ Registrar C1       → guardarConteo1()
├─ Registrar C2       → guardarConteo2()
├─ Pre-carga          → mapItem()
├─ Validaciones       → múltiples checks
└─ UI                 → template

ReimpresionMarbetes.vue (mismo patrón)
└─ Usa /labels/for-count endpoint igual

periodoStore.ts
└─ setPeriodo(p)      → Guardar período seleccionado
```

---

## ✅ CHECKLIST ANTES DE CAMBIOS

- [ ] ¿Validaste período + almacén?
- [ ] ¿Verificaste que marbete está IMPRESO?
- [ ] ¿Comprobaste que no está cancelado?
- [ ] ¿Mappeaste campos correctamente?
- [ ] ¿Formateaste números con miles?
- [ ] ¿Bloqueaste inputs ya registrados?
- [ ] ¿Enfocaste inteligentemente?
- [ ] ¿Calculaste diferencia automática?
- [ ] ¿Limpiaste formulario después?
- [ ] ¿Testaste en 2+ navegadores?

---

## 🔗 REFERENCIAS RÁPIDAS

- API Spec: `MARBETES_API_SPEC.md`
- Endpoints: `APIS_CONSUMIDAS_ALMACENISTA.md`
- Bloqueos: `SOLUCION_FINAL_BLOQUEO_FOLIOS.md`
- Conteos: `IMPLEMENTACION_FINAL_CONTEOS.md`
- Reimpresión: `IMPLEMENTACION_REIMPRESION_PATRON_CONTEO.md`

---

**Versión:** 1.0 | **Última actualización:** 2026-04-01


