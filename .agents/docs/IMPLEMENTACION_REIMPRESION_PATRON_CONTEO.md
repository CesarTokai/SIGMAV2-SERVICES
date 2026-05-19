# ✅ Implementación: Reimpresión de Marbetes con Patrón de Conteo

## 📋 Resumen de Cambios

Se ha **refactorizado completamente ReimpresionMarbetes.vue** para utilizar el **mismo patrón de búsqueda y consulta que ConteoMarbetes.vue**.

### 🔄 Cambios Principales

#### 1. **Interfaz actualizada** (MarbeteReimpresion)
```typescript
// ❌ ANTES (propiedades específicas para reimpresión)
interface MarbeteReimpresion {
  estado: string;
  fechaImpresion: string;
  reimpresionesAnteriores: number;
}

// ✅ AHORA (propiedades consistentes con ConteoMarbetes)
interface MarbeteReimpresion {
  conteo1: number | null;
  conteo2: number | null;
  diferencia: number | null;
  estado: string;
  cancelado: boolean;
}
```

#### 2. **Endpoint unificado**
```typescript
// ❌ ANTES
const response = await axiosConfiguration.doPost('/api/sigmav2/labels/impresos', body);

// ✅ AHORA (mismo endpoint que ConteoMarbetes)
const response = await axiosConfiguration.doPost('/labels/for-count', body);
```

#### 3. **Watchers reemplazados por Vue watch**
```typescript
// ❌ ANTES (handlers manuales)
const handlePeriodoChange = (newId: number | null) => { ... }
const handleAlmacenChange = (newId: number | null) => { ... }

// ✅ AHORA (watchers Vue estándar como en ConteoMarbetes)
watch(selectedPeriodoId, (newId) => { ... })
watch(selectedAlmacenId, (newId) => { ... })
```

---

## 🎯 Flujo de Funcionamiento

### **Paso 1: Seleccionar Período y Almacén**
```
Usuario abre ReimpresionMarbetes.vue
        ↓
Selecciona Período (cargado del store si existe)
        ↓
Selecciona Almacén (auto-selecciona el primero si existe)
```

### **Paso 2: Buscar Marbete por Folio**
```
Usuario ingresa folio (ej: 195)
        ↓
Presiona Enter o click en "Buscar"
        ↓
API POST /labels/for-count
  {
    "folio": 195,
    "periodId": 7,
    "warehouseId": 218
  }
        ↓
Validaciones:
  1️⃣ ¿Marbete existe? → Si no → Error "No encontrado"
  2️⃣ ¿Estado = IMPRESO? → Si no → Error "Estado inválido"
  3️⃣ ¿Cancelado? → Si sí → Error "Cancelado"
        ↓
✅ Marbete cargado → Mostrar información
```

### **Paso 3: Información Mostrada**
Igual que en ConteoMarbetes:
- ✅ Folio
- ✅ Producto
- ✅ Clave Producto
- ✅ Almacén
- ✅ Estado (IMPRESO)
- ✅ Existencias esperadas

### **Paso 4: Reimprimir**
```
Usuario hace click en "Reimprimir Marbete"
        ↓
Confirmación modal
        ↓
POST /labels/print
  {
    "periodId": 7,
    "warehouseId": 218,
    "folios": [195],
    "forceReprint": true  ← Parámetro clave para reimpresión
  }
        ↓
API retorna PDF
        ↓
Descarga automática: reimpresion_folio_195_timestamp.pdf
        ↓
✅ Éxito → Limpiar formulario
```

---

## 🔐 Validaciones de Seguridad

### En la Búsqueda (buscarMarbetePorFolio)
```typescript
✅ Validación 1: Folio ingresado
if (!raw) → ToastError("Ingresa un folio")

✅ Validación 2: Período y Almacén seleccionados
if (!selectedPeriodo || !selectedAlmacen) → ToastError("Selecciona período y almacén")

✅ Validación 3: Estado = IMPRESO
if (response.data.estado !== 'IMPRESO') → ToastError("Solo se pueden reimprimir IMPRESOS")

✅ Validación 4: No cancelado
if (response.data.cancelado) → ToastError("Marbete cancelado")
```

### En la Reimpresión (reimprimirMarbete)
```typescript
✅ Validación 1: Marbete seleccionado
if (!marbeteActual) → ToastError("Busca un marbete primero")

✅ Validación 2: Período y Almacén
if (!selectedPeriodo || !selectedAlmacen) → ToastError("Selecciona período y almacén")

✅ Validación 3: Confirmación usuario
Swal.fire() → Usuario debe confirmar

✅ Validación 4: API valida forceReprint=true
Backend valida que folios estén en estado IMPRESO
```

---

## 📊 Comparativa: Conteo vs Reimpresión

| Aspecto | ConteoMarbetes | ReimpresionMarbetes |
|---------|-----------------|---------------------|
| **Endpoint búsqueda** | `/labels/for-count` | `/labels/for-count` ✅ |
| **Interfaz datos** | MarbeteConteo | MarbeteReimpresion (unificada) ✅ |
| **Validación estado** | Cualquier estado | Solo IMPRESO ✅ |
| **Acción principal** | Guardar conteos | Reimprimir PDF ✅ |
| **Confirmación** | No (automático) | Sí (modal) ✅ |
| **Descarga PDF** | No | Sí ✅ |

---

## 🚀 Casos de Uso

### **Caso 1: Reimpresión Normal**
```
Usuario: "Necesito reimprimir el folio 195"
1. Selecciona Período 7, Almacén 218
2. Ingresa folio: 195
3. Sistema busca → Marbete IMPRESO encontrado ✅
4. Usuario hace click "Reimprimir"
5. Descarga PDF y termina
```

### **Caso 2: Marbete No Impreso**
```
Usuario: "Intento reimprimir folio 190 pero está GENERADO"
1. Selecciona Período 7, Almacén 218
2. Ingresa folio: 190
3. Sistema busca → Marbete GENERADO ❌
4. Error: "Solo se pueden reimprimir IMPRESOS"
5. Usuario debe primero imprimir en ImpresionMarbetes.vue
```

### **Caso 3: Marbete Cancelado**
```
Usuario: "Intento reimprimir folio 197 pero está CANCELADO"
1. Selecciona Período 7, Almacén 218
2. Ingresa folio: 197
3. Sistema busca → Marbete CANCELADO ❌
4. Error: "Marbete Cancelado - No puede ser reimpreso"
```

### **Caso 4: Sin Período/Almacén**
```
Usuario: "Intento buscar pero no seleccioné período"
1. Deja campos período/almacén vacíos
2. Ingresa folio: 195
3. Click "Buscar"
4. Error: "Selecciona período y almacén primero"
```

---

## 📝 Notas Técnicas

### **Backend - Validación del endpoint `/labels/for-count`**

El endpoint ya existe y es usado por ConteoMarbetes. Ahora también lo usa ReimpresionMarbetes.

```java
// En LabelServiceImpl
@PostMapping("/labels/for-count")
public LabelDTO getLabelForCount(
    @RequestBody LabelSearchRequest request) {
  
  // Busca marbete por folio/periodo/almacén
  // Retorna estado actual (GENERADO, IMPRESO, CONTADO, CANCELADO)
  // NO filtra por estado
  
  return labelService.findByFolioAndPeriodAndWarehouse(
    request.getFolio(),
    request.getPeriodId(),
    request.getWarehouseId()
  );
}
```

### **Backend - Validación del endpoint `/labels/print`**

Cuando se llama con `forceReprint=true`:
```java
// En LabelServiceImpl.printLabels()
if (dto.isForceReprint()) {
  // Rama extraordinaria
  if (folios sin estado IMPRESO) {
    throw new InvalidLabelStateException(
      "No está IMPRESO y no puede reimprimirse"
    );
  }
  // Procede con reimpresión
}
```

---

## ✅ Testing Checklist

- [ ] **Búsqueda por folio** - Marbete encontrado ✅
- [ ] **Búsqueda fallida** - Folio no existe ❌
- [ ] **Búsqueda GENERADO** - Rechazo correcto ❌
- [ ] **Búsqueda CANCELADO** - Rechazo correcto ❌
- [ ] **Reimpresión exitosa** - PDF descargado ✅
- [ ] **Sin período** - Error validación ❌
- [ ] **Sin almacén** - Error validación ❌
- [ ] **Confirmación modal** - Aparece antes de reimprimir ✅
- [ ] **Limpieza de formulario** - Después de reimprimir ✅
- [ ] **Atajos de teclado** - Alt+F, Alt+L funcionan ✅

---

## 🎨 UX/UI Mejorada

### **Pantalla ContedeoBefore (antigua)**
- Entrada manual de folios
- Búsqueda específica para estados IMPRESO

### **Pantalla Reimpresión After (nueva)**
- ✅ Mismo patrón visual que ConteoMarbetes
- ✅ Búsqueda unificada con el mismo endpoint
- ✅ Interfaz consistente
- ✅ Validaciones claras en cada paso
- ✅ Mensajes amigables al usuario
- ✅ Atajos de teclado incluidos

---

## 📚 Documentación Relacionada

- `APIS_CONSUMIDAS_ALMACENISTA.md` - Endpoints consumidos
- `IMPLEMENTACION_REIMPRESION_MARBETES.md` - Documentación original de reimpresión
- `FASES_3_4_IMPLEMENTACION_COMPLETADA.md` - Contexto de implementación


