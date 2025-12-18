# 🔴 ERROR: "El marbete no pertenece al periodo/almacén especificado"

## ❓ Problema Reportado

Al intentar consultar un marbete para conteo, se recibe el error:

```
POST http://localhost:8080/api/sigmav2/labels/for-count 500 (Internal Server Error)

Error: El marbete no pertenece al periodo/almacén especificado
```

---

## 🔍 Análisis del Problema

### Datos del Frontend (Tabla Summary):

```
Folio  Producto     Almacén      Existencias  Rango Folios   Impreso
1      FactGlob     Almacén 24   0            246 - 246      Sí
5      X-TARIMAS    Almacén 24   0            247 - 251      Sí
```

### Consulta que se Está Haciendo:

```javascript
// Frontend intenta buscar folio 5
{
  folio: 5,
  periodId: 20,
  warehouseId: 420
}
```

### 🔴 ERROR IDENTIFICADO:

**El frontend está mostrando el número de REGISTRO (1, 5) en lugar del FOLIO REAL (246-251)**

---

## 🎯 Causa Raíz

### API `/labels/summary` Retorna:

```json
{
  "productId": 9433,
  "claveProducto": "X-TARIMAS",
  "nombreProducto": "PALLET DE MADERA",
  "primerFolio": 247,      // ← FOLIO REAL
  "ultimoFolio": 251,      // ← FOLIO REAL
  "folios": [247, 248, 249, 250, 251],  // ← FOLIOS REALES
  "foliosSolicitados": 5,
  "foliosExistentes": 5
}
```

### Frontend está mostrando incorrectamente:

- **Columna "Folio"**: Muestra un número secuencial de registro (1, 2, 3...)
- **Columna "Rango Folios"**: Muestra correctamente (246-246, 247-251)

### Cuando el usuario hace clic en el folio "5":

```javascript
// ❌ INCORRECTO: Frontend envía el número de registro
buscarMarbetePorFolio(5);  // Folio 5 no existe en BD

// ✅ CORRECTO: Debería enviar el folio real
buscarMarbetePorFolio(247); // Primer folio del rango 247-251
```

---

## 🔧 Solución

### Opción 1: Ocultar la Columna "Folio" (RECOMENDADO)

La columna "Folio" en el summary **NO tiene sentido** porque:
- Un producto puede tener **múltiples folios** (ejemplo: 247, 248, 249, 250, 251)
- Mostrar un solo número confunde al usuario
- Ya existe "Rango Folios" que es más útil

**Acción**: Eliminar o no mostrar la columna "Folio" en la tabla de summary.

---

### Opción 2: Mostrar el Primer Folio del Rango

Si aún quieres mostrar un folio individual, usa `primerFolio`:

```vue
<!-- ImpresionMarbetes.vue o similar -->
<template>
  <table>
    <tr v-for="item in marbetes" :key="item.productId">
      <td>{{ item.primerFolio || '-' }}</td>  <!-- ✅ CORRECTO -->
      <td>{{ item.claveProducto }}</td>
      <td>{{ item.nombreProducto }}</td>
      <td>{{ item.existencias }}</td>
      <td>{{ formatRangoFolios(item) }}</td>
    </tr>
  </table>
</template>

<script setup>
const formatRangoFolios = (item) => {
  if (!item.primerFolio) return '-';
  if (item.primerFolio === item.ultimoFolio) {
    return item.primerFolio.toString();
  }
  return `${item.primerFolio} - ${item.ultimoFolio}`;
};
</script>
```

---

### Opción 3: Permitir Seleccionar Folio Individual

Si el producto tiene múltiples folios, mostrar un desplegable:

```vue
<template>
  <div v-if="item.folios && item.folios.length > 1">
    <select @change="seleccionarFolio($event, item)">
      <option value="">Seleccione folio...</option>
      <option
        v-for="folio in item.folios"
        :key="folio"
        :value="folio">
        Folio {{ folio }}
      </option>
    </select>
  </div>
  <div v-else>
    {{ item.primerFolio }}
  </div>
</template>

<script setup>
const seleccionarFolio = (event, item) => {
  const folioSeleccionado = parseInt(event.target.value);
  buscarMarbetePorFolio(folioSeleccionado);
};

const buscarMarbetePorFolio = async (folio) => {
  try {
    const response = await api.post('/labels/for-count', {
      folio: folio,  // ✅ FOLIO REAL (247, 248, etc.)
      periodId: selectedPeriod.value,
      warehouseId: selectedWarehouse.value
    });
    // ...procesar respuesta
  } catch (error) {
    console.error('Error:', error);
  }
};
</script>
```

---

## 🔍 Verificación en Backend

El método `getLabelForCount` funciona correctamente:

```java
public LabelForCountDTO getLabelForCount(Long folio, Long periodId, Long warehouseId, ...) {
    // Buscar el marbete por folio (Primary Key)
    Label label = jpaLabelRepository.findById(folio)
        .orElseThrow(() -> new LabelNotFoundException("Marbete con folio " + folio + " no encontrado"));

    // Validar que pertenece al periodo y almacén especificado
    if (!label.getPeriodId().equals(periodId) || !label.getWarehouseId().equals(warehouseId)) {
        throw new InvalidLabelStateException("El marbete no pertenece al periodo/almacén especificado");
    }

    // ...resto del código
}
```

### ✅ Funcionamiento Correcto:

```javascript
// ✅ CORRECTO: Buscar folio 247 (existe en BD)
POST /labels/for-count
{
  "folio": 247,
  "periodId": 20,
  "warehouseId": 420
}
// → Respuesta: 200 OK (datos del marbete)

// ❌ INCORRECTO: Buscar folio 5 (no existe en BD)
POST /labels/for-count
{
  "folio": 5,
  "periodId": 20,
  "warehouseId": 420
}
// → Error: 500 "El marbete no pertenece al periodo/almacén especificado"
```

---

## 📊 Estructura de Datos Correcta

### Base de Datos:

```sql
-- Tabla: labels
folio  | product_id | warehouse_id | period_id | estado
-------|------------|--------------|-----------|--------
246    | 9796       | 420          | 20        | IMPRESO
247    | 9433       | 420          | 20        | IMPRESO
248    | 9433       | 420          | 20        | IMPRESO
249    | 9433       | 420          | 20        | IMPRESO
250    | 9433       | 420          | 20        | IMPRESO
251    | 9433       | 420          | 20        | IMPRESO
```

### API Summary Response:

```json
[
  {
    "productId": 9796,
    "claveProducto": "FactGlob",
    "primerFolio": 246,
    "ultimoFolio": 246,
    "folios": [246],
    "foliosExistentes": 1
  },
  {
    "productId": 9433,
    "claveProducto": "X-TARIMAS",
    "primerFolio": 247,
    "ultimoFolio": 251,
    "folios": [247, 248, 249, 250, 251],
    "foliosExistentes": 5
  }
]
```

---

## 🎯 Solución Inmediata (Frontend)

### Archivo: `ConteoMarbetes.vue` (o similar)

**Cambio en el método de búsqueda:**

```javascript
// ❌ ANTES (INCORRECTO)
const buscarMarbetePorFolio = async () => {
  try {
    const response = await api.post('/labels/for-count', {
      folio: folioIngresado.value,  // Usuario ingresa "5"
      periodId: selectedPeriod.value,
      warehouseId: selectedWarehouse.value
    });
  } catch (error) {
    console.error('Error:', error);
  }
};

// ✅ DESPUÉS (CORRECTO)
const buscarMarbetePorFolio = async () => {
  // Validar que el folio sea un número válido
  const folioNumero = parseInt(folioIngresado.value);

  if (isNaN(folioNumero) || folioNumero <= 0) {
    ElMessage.error('Ingrese un folio válido');
    return;
  }

  try {
    const response = await api.post('/labels/for-count', {
      folio: folioNumero,  // ✅ Folio REAL (246, 247, etc.)
      periodId: selectedPeriod.value,
      warehouseId: selectedWarehouse.value
    });

    marbeteActual.value = response.data;
    ElMessage.success(`Marbete ${folioNumero} encontrado`);

  } catch (error) {
    if (error.response?.status === 500) {
      ElMessage.error(
        `El folio ${folioNumero} no existe o no pertenece al periodo/almacén seleccionado`
      );
    } else {
      ElMessage.error('Error al buscar marbete');
    }
    console.error('Error:', error);
  }
};
```

---

## 📝 Tabla de Summary - Configuración Recomendada

### ✅ RECOMENDADO:

```vue
<el-table :data="marbetes">
  <!-- ❌ ELIMINAR esta columna -->
  <!-- <el-table-column prop="folio" label="Folio" /> -->

  <el-table-column prop="claveProducto" label="Clave Producto" />
  <el-table-column prop="nombreProducto" label="Producto" />
  <el-table-column prop="claveAlmacen" label="Clave Almacén" />
  <el-table-column prop="nombreAlmacen" label="Almacén" />
  <el-table-column prop="existencias" label="Existencias" />
  <el-table-column prop="foliosExistentes" label="Cant. Folios" />

  <!-- ✅ USAR esta columna -->
  <el-table-column label="Rango Folios">
    <template #default="{ row }">
      <span v-if="!row.primerFolio">-</span>
      <span v-else-if="row.primerFolio === row.ultimoFolio">
        {{ row.primerFolio }}
      </span>
      <span v-else>
        {{ row.primerFolio }} - {{ row.ultimoFolio }}
      </span>
    </template>
  </el-table-column>

  <el-table-column prop="impreso" label="Impreso">
    <template #default="{ row }">
      {{ row.impreso ? 'Sí' : 'No' }}
    </template>
  </el-table-column>

  <el-table-column prop="fechaImpresion" label="Fecha Impresión" />
</el-table>
```

---

## 🧪 Prueba Rápida

### Paso 1: Verificar folios reales en BD

```sql
SELECT folio, product_id, warehouse_id, period_id, estado
FROM labels
WHERE warehouse_id = 420 AND period_id = 20
ORDER BY folio;
```

### Paso 2: Probar con folios reales

```bash
# ✅ Folio 246 (existe)
curl -X POST http://localhost:8080/api/sigmav2/labels/for-count \
  -H "Content-Type: application/json" \
  -d '{"folio": 246, "periodId": 20, "warehouseId": 420}'

# ✅ Folio 247 (existe)
curl -X POST http://localhost:8080/api/sigmav2/labels/for-count \
  -H "Content-Type: application/json" \
  -d '{"folio": 247, "periodId": 20, "warehouseId": 420}'

# ❌ Folio 5 (no existe)
curl -X POST http://localhost:8080/api/sigmav2/labels/for-count \
  -H "Content-Type: application/json" \
  -d '{"folio": 5, "periodId": 20, "warehouseId": 420}'
```

---

## ✅ Resumen

| Problema | Causa | Solución |
|----------|-------|----------|
| Error 500 al buscar folio | Frontend envía número de registro (1, 5) en lugar de folio real (246, 247) | Usar `primerFolio` o `folios[]` del API |
| Columna "Folio" confusa | Muestra índice de array, no folio real | Eliminar o usar `primerFolio` |
| Usuario no sabe qué folio ingresar | No hay indicación clara | Mostrar rango de folios disponibles |

---

**Fecha**: 2025-12-18
**Estado**: ✅ Problema Identificado - Requiere corrección en Frontend
**Archivos Afectados**:
- Frontend: `ImpresionMarbetes.vue`, `ConteoMarbetes.vue`
- Backend: ✅ Funcionando correctamente

**Acción Requerida**: Actualizar frontend para usar folios reales de la API

