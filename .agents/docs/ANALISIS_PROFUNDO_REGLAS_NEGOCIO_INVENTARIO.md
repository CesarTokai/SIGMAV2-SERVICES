# 📊 ANÁLISIS PROFUNDO - Reglas de Negocio & Módulo de Inventario
**Elaborado:** 2026-04-01 | **Versión:** 1.0

---

## 🎯 RESUMEN EJECUTIVO

SIGMAV2-APP es un sistema de **gestión de etiquetas/marbetes de inventario** con flujo de trabajo multirol:
1. **Admin**: Solicita + Genera + Imprime marbetes
2. **Almacenista**: Genera + Imprime + Contea (inventario)
3. **Auxiliar**: Captura conteos
4. **Auxiliar de Conteo**: Verifica conteos

El módulo de **inventario** está integrado en el flujo de **Conteo de Marbetes**, donde se registran dos conteos (C1 y C2) para cada producto/almacén.

---

## 🔄 FLUJO DE VIDA DEL MARBETE (Ciclo Completo)

```
┌─────────────────────────────────────────────────────────────┐
│                    CICLO DE MARBETE                         │
└─────────────────────────────────────────────────────────────┘

FASE 1: SOLICITUD (Admin/Almacenista)
   ├─ Selecciona período + almacén
   ├─ Ingresa cantidad de folios por producto
   └─ Estado: SOLICITADO

FASE 2: GENERACIÓN (Admin/Almacenista) 
   ├─ Genera folios secuenciales (ej: 1001-1050)
   ├─ Asigna a productos específicos
   ├─ Almacena en BD
   └─ Estado: GENERADO

FASE 3: IMPRESIÓN (Admin/Almacenista)
   ├─ Busca folios por rango o individual
   ├─ Genera PDF del marbete
   ├─ Marca como IMPRESO en BD
   └─ Estado: IMPRESO ← AQUÍ COMIENZA EL INVENTARIO

FASE 4: CONTEO (Almacenista/Auxiliar/Auxiliar Conteo) ⭐ INVENTARIO
   ├─ Busca marbete por folio
   ├─ Registra C1 (primer conteo físico)
   ├─ Registra C2 (segundo conteo de verificación)
   ├─ Calcula diferencia: |C1 - C2|
   └─ Estado: COMPLETO o ERROR (si diferencia > umbral)

FASE 5: REIMPRESIÓN (Extraordinaria)
   ├─ Solo si está IMPRESO
   ├─ No puede regenerarse
   ├─ Descarga PDF nuevamente
   └─ Estado: IMPRESO (sin cambios)

FASE 6: CANCELACIÓN (Extraordinaria)
   ├─ Solo antes de C2
   ├─ Registra motivo
   ├─ Irreversible
   └─ Estado: CANCELADO

┌─────────────────────────────────────────────────────────────┐
│ BLOQUEOS IMPLEMENTADOS:                                     │
│ ❌ No se puede generar 2x si existe IMPRESO               │
│ ❌ No se puede reimprimir si no es IMPRESO                 │
│ ❌ No se puede contar si está CANCELADO                    │
│ ✅ Se pueden registrar C1 y C2 múltiples veces (actualizaciones)
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 MÓDULO DE INVENTARIO (Conteo de Marbetes)

### 🎯 Propósito
**Registrar y verificar existencias físicas vs existencias esperadas**

### 📊 Datos Principales

```typescript
interface MarbeteInventario {
  // Identificación
  id: number;
  folio: number;                // 1001-9999 (secuencial)
  
  // Producto
  claveProducto: string;        // "PROD-001"
  producto: string;             // "Laptop Dell"
  
  // Almacén
  claveAlmacen: string;         // "ALM-01"
  almacen: string;              // "Almacén Central"
  
  // INVENTARIO - Datos Críticos
  existenciasEsperadas: number; // Stock teórico
  
  // CONTEOS (El corazón del inventario)
  conteo1: number | null;       // Primer conteo (físico)
  conteo2: number | null;       // Segundo conteo (verificación)
  diferencia: number | null;    // |C1 - C2| (variancia)
  
  // Estados
  estado: string;               // GENERADO, IMPRESO, CONTADO, CANCELADO
  cancelado: boolean;           // Flag de cancelación
}
```

### 🔢 Reglas de Negocio del Inventario

#### **Regla 1: Secuencia de Conteo (CRÍTICA)**
```
ANTES:  ❌ Permitía registrar C2 sin C1
AHORA:  ✅ Bloquea C2 si C1 es null
        
Validación:
if (conteo1 === null) {
  ToastError("Registra C1 primero");
  return;
}
```

#### **Regla 2: Cálculo de Diferencia (AUTOMÁTICO)**
```javascript
diferencia = Math.abs(conteo1 - conteo2);

Ejemplos:
- C1: 100, C2: 100 → Diferencia: 0 ✅ (Sin variancia)
- C1: 100, C2: 98  → Diferencia: 2 ⚠️ (Variancia menor)
- C1: 100, C2: 80  → Diferencia: 20 🔴 (Variancia mayor)
```

#### **Regla 3: Estados del Marbete**
```
PENDIENTE      → Sin conteos (C1=null, C2=null)
PARCIAL        → Solo C1 registrado
COMPLETO       → Ambos conteos registrados ✅
ERROR          → Variancia > umbral (si existe)
CANCELADO      → Marcado manualmente
```

#### **Regla 4: Validación de Marbete Cancelado**
```typescript
if (marbete.cancelado === true) {
  ToastError("Este marbete está cancelado");
  ToastError("No se puede registrar conteo");
  return; // BLOQUEA completamente
}
```

#### **Regla 5: Actualización vs Creación**
```typescript
// Si C1 existe → ACTUALIZAR
if (marbete.conteo1 !== null) {
  PUT /labels/counts/c1  // Actualizar
}

// Si C1 no existe → CREAR
else {
  POST /labels/counts/c1  // Crear
}
```

#### **Regla 6: Visibilidad de Conteos Pre-cargados**
```
Al cargar marbete con C1 registrado:
  Input C1: "3,700" ← VISIBLE con formato
  Input C2: (vacío) ← Enfocado aquí
  
Usuario VE el primer conteo mientras registra el segundo
→ Facilita verificación visual
→ Previene errores de captura
```

#### **Regla 7: Formato con Miles**
```javascript
// Backend almacena:
conteo1: 3700

// Frontend muestra:
Input: "3,700"  // Con separador
Cálculo: 3700 - 3698 = 2

// Usuario ve consistencia visual
```

---

## 🏪 ALMACENES Y PERÍODO (Contexto)

### **Período (Ciclo de Inventario)**
```json
{
  "id": 7,
  "date": "2026-02-28",
  "comments": "Inventario Mensual Febrero",
  "state": "ACTIVO"
}
```

**Reglas:**
- ✅ Un período = UN ciclo de inventario
- ✅ Período ACTIVO = Permitir conteos
- ❌ Período CERRADO = Bloquear nuevas operaciones
- ✅ Varios almacenes pueden contar en mismo período

### **Almacén (Ubicación Física)**
```json
{
  "id": 218,
  "clave": "ALM-01",
  "nombre": "Almacén Central",
  "activo": true
}
```

**Reglas:**
- ✅ User ALMACENISTA → Solo su almacén asignado
- ✅ User ADMIN → Todos los almacenes
- ✅ Cada marbete = Período + Almacén específico
- ❌ No puede mezclar almacenes en un conteo

---

## 🔐 SEGURIDAD POR ROL

### **ADMIN** (Gerente General)
```
✅ Ver todos los almacenes
✅ Generar marbetes
✅ Imprimir marbetes
✅ Registrar conteos
✅ Cancelar marbetes
✅ Ver reportes completos
```

### **ALMACENISTA** (Gerente de Almacén)
```
✅ Ver SOLO su almacén asignado
✅ Generar marbetes (su almacén)
✅ Imprimir marbetes (su almacén)
✅ Registrar conteos (su almacén)
✅ Cancelar marbetes (su almacén)
❌ Ver otros almacenes
❌ Cambiar almacén de marbete
```

### **AUXILIAR** (Capturista)
```
✅ Registrar conteo C1
✅ Actualizar conteo C1
❌ Registrar C2 (solo Almacenista/Auxiliar Conteo)
❌ Cancelar
❌ Ver reportes
```

### **AUXILIAR DE CONTEO** (Verificador)
```
✅ Registrar conteo C2 (verificación)
✅ Ver discrepancias C1 vs C2
❌ Crear nuevo marbete
❌ Cancelar
```

---

## 📊 FLUJO DE CONTEO (DETALLADO) - ⭐ INVENTARIO

### **Paso 1: Seleccionar Período**
```
Usuario abre: ConteoMarbetes.vue
        ↓
Verifica periodoStore (período anterior guardado)
        ↓
Si existe: Carga automáticamente
Si no existe: Carga primer período
```

**Reglas:**
- Solo períodos ACTIVO/ABIERTO
- Período se guarda en store para siguiente sesión
- Compartido entre: Conteo, Reimpresión, Cancelación

### **Paso 2: Seleccionar Almacén**
```
Carga lista según rol:
- ADMIN: Todos los almacenes activos
- ALMACENISTA: Solo el suyo
- AUXILIAR: Mismo que ALMACENISTA
- AUXILIAR_CONTEO: Mismo que ALMACENISTA
```

**Auto-selección:**
- Si solo 1 almacén → Selecciona automático
- Si múltiples → Muestra dropdown

### **Paso 3: Buscar Marbete por Folio**
```
Endpoint: POST /labels/for-count
Request:
{
  "folio": 1001,
  "periodId": 7,
  "warehouseId": 218
}

Response:
{
  "id": 123,
  "folio": 1001,
  "claveProducto": "PROD-001",
  "descripcionProducto": "Laptop",
  "claveAlmacen": "ALM-01",
  "nombreAlmacen": "Almacén Central",
  "existenciasEsperadas": 1250,
  "conteo1": null,           // Sin conteos
  "conteo2": null,
  "diferencia": null,
  "estado": "IMPRESO",       // Debe estar IMPRESO
  "cancelado": false         // No cancelado
}
```

**Validaciones:**
```typescript
✅ Folio ingresado: if (!folio) → Error
✅ Período + Almacén: if (!selectedPeriodo || !selectedAlmacen) → Error
✅ Marbete existe: if (404) → Error "Folio no existe"
✅ Estado IMPRESO: if (estado !== 'IMPRESO') → Error
✅ No cancelado: if (cancelado === true) → Error
```

### **Paso 4: Registrar C1 (Primer Conteo)**
```
Input C1 está: VACÍO ← Enfocado automáticamente

Usuario escribe: 3700
        ↓
Validaciones:
- Solo números enteros
- Bloquea: decimales (3700.99), letras (ABC), símbolos (@#$)
- Formatea: "3,700" (visual)
        ↓
Usuario presiona: ENTER o TAB
        ↓
Endpoint: POST /labels/counts/c1
Request:
{
  "folio": 1001,
  "countedValue": 3700
}

Response:
{
  "message": "Primer conteo registrado",
  "folio": 1001,
  "c1Value": 3700
}
        ↓
Estado actualizado: PARCIAL
        ↓
Input C1 ahora muestra: "3,700" ← BLOQUEADO (gris)
Input C2: (vacío) ← Enfocado automáticamente
```

### **Paso 5: Registrar C2 (Segundo Conteo - Verificación)**
```
Input C2 está: VACÍO ← Enfocado automáticamente

Usuario escribe: 3698
        ↓
EN TIEMPO REAL:
- Lee Input C1: 3700 (pre-cargado)
- Lee Input C2: 3698 (escribiendo)
- Calcula diferencia: 3700 - 3698 = 2
- Muestra: "Diferencia: 2"
        ↓
Usuario presiona: ENTER
        ↓
Endpoint: POST /labels/counts/c2
Request:
{
  "folio": 1001,
  "countedValue": 3698
}

Response:
{
  "message": "Segundo conteo registrado",
  "folio": 1001,
  "c1Value": 3700,
  "c2Value": 3698,
  "diferencia": 2
}
        ↓
Estado actualizado: COMPLETO
        ↓
Panel muestra:
  C1: 3,700
  C2: 3,698
  Diferencia: 2 ✅
  Estado: COMPLETO
```

### **Paso 6: Captura Continua**
```
Al guardar C2:
  Formulario se limpia automáticamente
  Input Folio enfocado
  
Usuario puede:
  Folio 1002 [ENTER]
  ↓
  C1: (vacío) ← Enfocado
  Escribe: 5000 [ENTER]
  ↓
  C2: (vacío) ← Enfocado
  Escribe: 5012
  Diferencia: 12
  [ENTER]
  ↓
  Limpieza automática
  Siguiente folio...
```

---

## ❌ BLOQUEOS Y VALIDACIONES (REGLAS CRÍTICAS)

### **Bloqueo 1: No Generar Duplicados**
```javascript
// ANTES (❌ Problema):
User genera productos 1,2,3 → OK
User vuelve → Genera 4,5
Resultado: Se regeneran TODOS (1,2,3,4,5) ❌

// AHORA (✅ Solución):
Backend verifica:
  SELECT * FROM marbetes 
  WHERE periodo=7 AND almacen=218 AND estado='IMPRESO'
  
Si existe IMPRESO:
  THROW "No se pueden generar más marbetes para este período/almacén"
  
Consecuencia:
- Inputs bloqueados (gris, disabled)
- Checkboxes para seleccionar SOLO nuevos
- Backend rechaza si ya hay IMPRESO
```

### **Bloqueo 2: No Contar Cancelados**
```javascript
if (marbete.cancelado === true) {
  ToastError("Marbete Cancelado");
  ToastError("No se puede registrar conteo");
  return;
}

Impacto: Búsqueda rechaza automáticamente
```

### **Bloqueo 3: Secuencia C1 → C2**
```javascript
// Intentar guardar C2 sin C1:
if (conteo1 === null && conteo2 !== null) {
  ToastError("Registra C1 primero");
  return;
}

// Impacto: Input C2 solo se habilita después de C1
```

### **Bloqueo 4: No Reimprimir No-Impresos**
```javascript
// Solo IMPRESO puede reimprimirse:
if (estado !== 'IMPRESO') {
  ToastError("Solo se pueden reimprimir IMPRESOS");
  return;
}

Impacto: ReimpresionMarbetes.vue rechaza GENERADO/CONTADO/CANCELADO
```

### **Bloqueo 5: Acceso por Almacén**
```javascript
// ALMACENISTA solo ve su almacén:
GET /warehouses
  → Filtra por user.warehouseId
  → ADMIN ve todos
  → ALMACENISTA ve 1 solo

if (userWarehouse !== requestedWarehouse) {
  throw FORBIDDEN("No tienes acceso a este almacén");
}
```

---

## 📈 REPORTES Y DISCREPANCIAS

### **Diferencia Aceptable**
```
Después de registrar C1 y C2:
  Diferencia = |C1 - C2|
  
- Diferencia 0-2: ✅ Normal (variancia aceptable)
- Diferencia 3-10: ⚠️ Revisar (posible error manual)
- Diferencia >10: 🔴 Critico (requiere reconteo)

Acción:
  Sistema CALCULA automáticamente
  Frontend MUESTRA en color según umbral
  Backend REGISTRA para auditoría
```

### **Estados Finales por Discrepancia**
```
EXCELENTE:  Diferencia = 0   (Conteos idénticos)
BUENO:      Diferencia 1-5   (Variancia mínima)
REVISAR:    Diferencia 6-20  (Requiere validación)
CRÍTICO:    Diferencia >20   (Puede indicar error)
CANCELADO:  No aplica         (Marbete anulado)
```

---

## 🗄️ MAPEO DE CAMPOS (CONSISTENCIA)

El backend devuelve nombres inconsistentes. Frontend mapea:

```typescript
// Response API → Interface Frontend
productCode         → claveProducto
productName         → producto
descripcionProducto → producto
warehouseKey        → claveAlmacen
warehouseName       → almacen
nombreAlmacen       → almacen
expectedStock       → existenciasEsperadas
existenciasEsperadas→ existenciasEsperadas
count1              → conteo1
c1                  → conteo1
countedValue        → conteo1 o conteo2
status              → estado
cancelled           → cancelado
```

**Patrón:**
```typescript
const mapItem = (item: any) => ({
  claveProducto: String(item.productCode ?? item.claveProducto ?? '').trim(),
  producto: String(
    item.productName ?? 
    item.nombreProducto ?? 
    item.descripcionProducto ?? 
    item.producto ?? ''
  ).trim(),
  conteo1: Number(item.count1 ?? item.c1 ?? null),
  conteo2: Number(item.count2 ?? item.c2 ?? null),
  // ... más campos
});
```

---

## 🔄 ENDPOINTS CLAVE PARA INVENTARIO

### **1. Buscar Marbete para Conteo**
```
POST /labels/for-count
Body:
{
  "folio": number,
  "periodId": number,
  "warehouseId": number
}

Retorna: MarbeteInventario completo
```

### **2. Registrar C1**
```
POST /labels/counts/c1
Body:
{
  "folio": number,
  "countedValue": number
}

Actualiza: marbete.conteo1
```

### **3. Registrar C2**
```
POST /labels/counts/c2
Body:
{
  "folio": number,
  "countedValue": number
}

Actualiza: marbete.conteo2
Calcula: diferencia = |C1 - C2|
```

### **4. Actualizar C2**
```
PUT /labels/counts/c2
Body:
{
  "folio": number,
  "countedValue": number
}

Recalcula: diferencia
```

---

## 🎯 CASOS DE USO REALES

### **Caso 1: Inventario Correcto**
```
Producto: Laptop Dell (folio 1001)
Existencias Esperadas: 100

Conteo 1 (Almacenista):
  Cuenta físicamente: 100
  Registra: 100
  
Conteo 2 (Auxiliar de Conteo):
  Verifica: 100
  Registra: 100
  
Resultado:
  Diferencia: 0 ✅
  Estado: COMPLETO
  Acción: Inventario OK, pasar al siguiente
```

### **Caso 2: Inventario con Variancia Menor**
```
Producto: Mouse Inalámbrico (folio 1050)
Existencias Esperadas: 500

Conteo 1:
  Cuenta: 498
  Registra: 498
  
Conteo 2:
  Verifica: 500
  Registra: 500
  
Resultado:
  Diferencia: 2 ⚠️
  Estado: COMPLETO
  Acción: REVISAR - Posible error en C1 o C2
```

### **Caso 3: Marbete Cancelado**
```
Producto: Teclado (folio 1025)
Estado: CANCELADO (usuario lo canceló antes de contar)

Usuario intenta:
  Busca folio 1025
  
Sistema responde:
  Error: "Marbete cancelado"
  "No se puede registrar conteo"
  
Acción: Usuario debe buscar otro folio o solicitar reactivación
```

### **Caso 4: Actualización de Conteo**
```
Producto: Monitor (folio 1010)

Primer intento:
  C1 registrado: 150
  C2 registrado: 149
  Diferencia: 1
  
Usuario nota error:
  "Revisé y C1 debería ser 152"
  
Actualización:
  Usuario busca folio 1010 de nuevo
  Input C1 muestra: "150" ← Pre-cargado
  Usuario modifica: 152
  Tab → Se actualiza
  
Resultado:
  C1: 152 (actualizado)
  C2: 149 (sin cambios)
  Diferencia: 3 (recalculada)
```

---

## 🚀 PATRÓN DE BÚSQUEDA CON DEBOUNCE

```typescript
// Usuario escribe en buscador
watch(searchQuery, (newQuery) => {
  if (searchDebounceTimeout) clearTimeout(searchDebounceTimeout);
  
  searchDebounceTimeout = setTimeout(() => {
    debouncedSearch.value = normalizeSearchText(newQuery);
    page.value = 0;  // Reset paginación
    loadMarbetes();  // Hacer búsqueda
  }, 500);  // 500ms debounce (NO 300ms)
});

// Normaliza búsqueda:
normalizeSearchText(text) = text
  .trim()                  // Quita espacios extremos
  .toLowerCase()           // A minúscula
  .replace(/\s+/g, ' ');   // Espacios múltiples → 1
```

**Por qué 500ms?**
- 300ms: Demasiado agresivo, genera muchas requests
- 500ms: Balanceado para UX y servidor
- 1000ms: Demasiado lento, mala experiencia

---

## 💾 PERSISTENCIA CON STORE

```typescript
// En periodoStore.ts
const setPeriodo = (p: Periodo) => {
  periodo.value = p;
  localStorage.setItem('selectedPeriodo', JSON.stringify(p));
};

// Cuando usuario vuelve a iniciar sesión:
// 1. Carga localStorage
// 2. SetPeriodo automáticamente
// 3. NO necesita seleccionar de nuevo

Beneficio: Continuidad entre sesiones
```

---

## ✅ CHECKLIST DE VALIDACIONES

**Frontend (ANTES de enviar al backend):**
- ☑️ Período seleccionado
- ☑️ Almacén seleccionado  
- ☑️ Folio ingresado y válido (números)
- ☑️ Folio encontrado
- ☑️ Marbete no cancelado
- ☑️ Estado = IMPRESO (para conteo)
- ☑️ C1 existe si registrando C2
- ☑️ Solo números enteros en conteos
- ☑️ Sin decimales
- ☑️ Sin caracteres especiales

**Backend (DESPUÉS de recibir):**
- ☑️ Token JWT válido
- ☑️ Usuario tiene acceso a ese almacén
- ☑️ Período es ACTIVO
- ☑️ Marbete existe y pertenece a período/almacén
- ☑️ Marbete no está CANCELADO
- ☑️ Valores numéricos válidos
- ☑️ No hay duplicados
- ☑️ Auditoría de cambios

---

## 🎨 INTERFAZ DE USUARIO

### **Elementos Principales**

**Buscador de Folio:**
- Input de número
- Botón "Buscar" o Enter
- Loading spinner durante búsqueda

**Panel de Información:**
- Folio, Producto, Clave
- Almacén, Existencias esperadas
- Estado del marbete

**Inputs de Conteo:**
- C1: Input numérico, pre-cargado si existe
- C2: Input numérico, enfocado después de C1
- Diferencia: Calculated read-only

**Botones:**
- Buscar (después de ingresar folio)
- Guardar (después de cada conteo)
- Limpiar (reset formulario)

### **Estados Visuales**

```
Input BLOQUEADO (C1 ya registrado):
- Fondo: #f5f5f5 (gris)
- Texto: #999 (gris)
- Borde: #ccc
- Cursor: "no-permitido"
- disabled: true

Input ACTIVO (editable):
- Fondo: white
- Texto: #333 (negro)
- Borde: #ccc → #28a745 (verde al focus)
- Cursor: "text"
- disabled: false
- Al escribir: fondo verde suave
```

---

## 📚 DOCUMENTACIÓN RELACIONADA

**Completamente estudiado:**
- ✅ SOLUCION_FINAL_BLOQUEO_FOLIOS.md
- ✅ IMPLEMENTACION_REIMPRESION_PATRON_CONTEO.md
- ✅ REFERENCIA_TECNICA_REIMPRESION_V2.md
- ✅ IMPLEMENTACION_FINAL_CONTEOS.md
- ✅ MARBETES_API_SPEC.md
- ✅ APIS_CONSUMIDAS_ALMACENISTA.md

---

## 🎯 CONCLUSIÓN

**SIGMAV2-APP está diseñado para:**

1. **Control Riguroso**: Bloqueos en cada fase
2. **Trazabilidad**: Todo registrado (auditoría)
3. **Seguridad por Rol**: Cada usuario ve solo su nivel
4. **Inventario Preciso**: 2 conteos + verificación
5. **UX Optimizada**: Enfoque inteligente, pre-carga, validación en tiempo real
6. **Recuperación**: Actualizar conteos si hay error

**Módulo de Inventario (Conteo) es:**
- Core del sistema
- Multirol integrado
- Validado en frontend + backend
- Con persistencia de período
- Y cálculo automático de discrepancias

---

**Estoy listo para trabajar con el módulo de inventario** 🚀


