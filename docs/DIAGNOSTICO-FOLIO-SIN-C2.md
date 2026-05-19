# Diagnóstico: Folio 123 sin Conteo C2

## 🔍 Problema Identificado

Los logs muestran claramente:

```
2026-01-22T16:09:04.621-06:00 ERROR - ❌ No existe un conteo C2 para el folio 123
2026-01-22T16:09:04.622-06:00 WARN  - ❌ Folio no encontrado o sin C2: No existe un conteo C2 para actualizar
2026-01-22T16:09:04.623-06:00 DEBUG - Completed 404 NOT_FOUND
```

**El folio 123 NO tiene un conteo C2 registrado.**

---

## ✅ La API Está Funcionando Correctamente

**Antes:** Error 500 genérico (sin información)  
**Ahora:** Error 404 con mensaje claro: "No existe un conteo C2 para actualizar"

Esto es el **comportamiento esperado** cuando intentas actualizar un conteo que no existe.

---

## 📋 Verificación en Base de Datos

### 1. Verificar si existe el folio

```sql
SELECT * FROM labels WHERE folio = 123;
```

**Resultado esperado:**
- Si retorna datos: el folio existe ✅
- Si está vacío: el folio no existe ❌

### 2. Verificar conteos del folio

```sql
SELECT 
    id_count_event,
    folio,
    count_number,
    counted_value,
    created_at,
    role_at_time
FROM label_count_events 
WHERE folio = 123 
ORDER BY created_at;
```

**Posibles resultados:**

#### Caso A: Sin conteos
```
(Vacío - 0 filas)
```
**Significa:** El folio existe pero nunca se registraron conteos (ni C1 ni C2)

#### Caso B: Solo C1
```
| id | folio | count_number | counted_value | created_at | role |
|----|-------|--------------|---------------|------------|------|
| 1  | 123   | 1            | 10.0          | ...        | ... |
```
**Significa:** Solo se registró C1, falta registrar C2

#### Caso C: C1 y C2
```
| id | folio | count_number | counted_value | created_at | role |
|----|-------|--------------|---------------|------------|------|
| 1  | 123   | 1            | 10.0          | ...        | ... |
| 2  | 123   | 2            | 15.0          | ...        | ... |
```
**Significa:** Ambos conteos existen, se puede actualizar C2 ✅

---

## 🔧 Solución Según el Caso

### Si NO existe C2 (Casos A y B)

**Primero debes REGISTRAR el C2:**

```bash
POST /api/sigmav2/labels/count/c2
Content-Type: application/json

{
  "folio": 123,
  "countedValue": 1237,
  "periodId": 16,
  "warehouseId": 369
}
```

**Luego podrás ACTUALIZAR el C2:**

```bash
PUT /api/sigmav2/labels/counts/c2
Content-Type: application/json

{
  "folio": 123,
  "countedValue": 1250,
  "observaciones": "Valor corregido"
}
```

---

## 🎯 Flujo Correcto

### 1️⃣ Registrar C1 (Primera vez)

```typescript
POST /api/sigmav2/labels/count/c1
{
  "folio": 123,
  "countedValue": 10,
  "periodId": 16,
  "warehouseId": 369
}
```

### 2️⃣ Registrar C2 (Primera vez)

```typescript
POST /api/sigmav2/labels/count/c2
{
  "folio": 123,
  "countedValue": 15,
  "periodId": 16,
  "warehouseId": 369
}
```

### 3️⃣ Actualizar C1 (Si es necesario corregir)

```typescript
PUT /api/sigmav2/labels/counts/c1
{
  "folio": 123,
  "countedValue": 12,
  "observaciones": "Corrección de conteo"
}
```

### 4️⃣ Actualizar C2 (Si es necesario corregir)

```typescript
PUT /api/sigmav2/labels/counts/c2
{
  "folio": 123,
  "countedValue": 17,
  "observaciones": "Corrección de conteo"
}
```

---

## 🐛 Posible Problema en el Frontend

Parece que tu frontend está usando **PUT** (actualizar) cuando debería usar **POST** (registrar).

### ❌ Incorrecto (lo que estás haciendo ahora)

```typescript
// Intentando ACTUALIZAR un C2 que no existe
const actualizarC2 = async () => {
  await axios.put('/labels/counts/c2', {
    folio: 123,
    countedValue: 1237
  });
};
```

### ✅ Correcto (lo que deberías hacer)

```typescript
// Primero verificar si existe C2
const manejarC2 = async (folio: number, valor: number) => {
  try {
    // Intentar obtener información del marbete
    const label = await getLabelForCount(folio, periodId, warehouseId);
    
    if (label.c2Value === null) {
      // NO existe C2, usar POST (registrar)
      console.log('Registrando C2 por primera vez...');
      await axios.post('/labels/count/c2', {
        folio,
        countedValue: valor,
        periodId,
        warehouseId
      });
      showSuccess('Conteo C2 registrado correctamente');
      
    } else {
      // SÍ existe C2, usar PUT (actualizar)
      console.log('Actualizando C2 existente...');
      await axios.put('/labels/counts/c2', {
        folio,
        countedValue: valor,
        observaciones: 'Valor corregido'
      });
      showSuccess('Conteo C2 actualizado correctamente');
    }
    
  } catch (error) {
    handleError(error);
  }
};
```

---

## 🔍 Verificar el Estado del Folio 123

Ejecuta esta consulta para ver qué conteos tiene:

```sql
SELECT 
    l.folio,
    l.estado AS estado_marbete,
    l.id_product,
    l.id_warehouse,
    l.id_period,
    
    -- Conteos
    (SELECT counted_value FROM label_count_events 
     WHERE folio = l.folio AND count_number = 1 
     ORDER BY created_at DESC LIMIT 1) AS c1_value,
    
    (SELECT counted_value FROM label_count_events 
     WHERE folio = l.folio AND count_number = 2 
     ORDER BY created_at DESC LIMIT 1) AS c2_value,
    
    (SELECT COUNT(*) FROM label_count_events 
     WHERE folio = l.folio) AS total_eventos
     
FROM labels l
WHERE l.folio = 123;
```

**Resultado esperado:**

```
| folio | estado_marbete | id_product | id_warehouse | id_period | c1_value | c2_value | total_eventos |
|-------|----------------|------------|--------------|-----------|----------|----------|---------------|
| 123   | IMPRESO        | 456        | 369          | 16        | 10.0     | NULL     | 1             |
```

Esto confirma que **solo existe C1, falta registrar C2**.

---

## 💡 Solución Recomendada para el Frontend

### Opción 1: Detectar Automáticamente

```typescript
const guardarConteo = async (
  tipo: 'C1' | 'C2',
  folio: number, 
  valor: number
) => {
  try {
    // Obtener información actual del marbete
    const response = await axios.post('/labels/for-count/list', {
      periodId,
      warehouseId,
      folio
    });
    
    const marbete = response.data[0];
    
    if (!marbete) {
      showError('Folio no encontrado');
      return;
    }
    
    // Decidir entre POST (registrar) o PUT (actualizar)
    const tieneConteo = tipo === 'C1' 
      ? marbete.c1Value !== null 
      : marbete.c2Value !== null;
    
    if (tieneConteo) {
      // Actualizar (PUT)
      await axios.put(`/labels/counts/${tipo.toLowerCase()}`, {
        folio,
        countedValue: valor,
        observaciones: 'Valor actualizado'
      });
      showSuccess(`${tipo} actualizado correctamente`);
      
    } else {
      // Registrar (POST)
      await axios.post(`/labels/count/${tipo.toLowerCase()}`, {
        folio,
        countedValue: valor,
        periodId,
        warehouseId
      });
      showSuccess(`${tipo} registrado correctamente`);
    }
    
  } catch (error) {
    if (error.response?.status === 404) {
      showError('El conteo no existe, use la opción de registrar');
    } else {
      showError(error.response?.data?.message || 'Error al guardar conteo');
    }
  }
};
```

### Opción 2: Botones Separados en la UI

```vue
<template>
  <div>
    <!-- Si no existe C2, mostrar botón de registrar -->
    <button 
      v-if="!marbete.c2Value" 
      @click="registrarC2"
      class="btn-primary"
    >
      Registrar C2
    </button>
    
    <!-- Si existe C2, mostrar botón de actualizar -->
    <button 
      v-else 
      @click="actualizarC2"
      class="btn-secondary"
    >
      Actualizar C2 (actual: {{ marbete.c2Value }})
    </button>
  </div>
</template>

<script>
const registrarC2 = async () => {
  await axios.post('/labels/count/c2', {
    folio: folio.value,
    countedValue: nuevoValor.value,
    periodId: periodId.value,
    warehouseId: warehouseId.value
  });
};

const actualizarC2 = async () => {
  await axios.put('/labels/counts/c2', {
    folio: folio.value,
    countedValue: nuevoValor.value,
    observaciones: observaciones.value
  });
};
</script>
```

---

## ✅ Pasos para Resolver Tu Problema Actual

### 1. Verifica si el folio 123 tiene C2

```sql
SELECT counted_value 
FROM label_count_events 
WHERE folio = 123 AND count_number = 2;
```

### 2. Si NO tiene C2, regístralo primero

```bash
POST http://localhost:8080/api/sigmav2/labels/count/c2
Content-Type: application/json
Authorization: Bearer YOUR_TOKEN

{
  "folio": 123,
  "countedValue": 1237,
  "periodId": 16,
  "warehouseId": 369
}
```

### 3. Después de registrarlo, ya podrás actualizarlo

```bash
PUT http://localhost:8080/api/sigmav2/labels/counts/c2
Content-Type: application/json
Authorization: Bearer YOUR_TOKEN

{
  "folio": 123,
  "countedValue": 1250,
  "observaciones": "Valor corregido"
}
```

---

## 📊 Resumen

| Operación | Endpoint | Método | ¿Cuándo usar? |
|-----------|----------|--------|---------------|
| Registrar C1 | `/count/c1` | POST | Primera vez que se registra C1 |
| Registrar C2 | `/count/c2` | POST | Primera vez que se registra C2 |
| Actualizar C1 | `/counts/c1` | PUT | Para corregir un C1 existente |
| Actualizar C2 | `/counts/c2` | PUT | Para corregir un C2 existente |

---

## 🎉 Conclusión

**Tu API está funcionando perfectamente.** El error 404 es correcto porque estás intentando actualizar un conteo que no existe.

**Solución:** Primero registra el C2 con POST, luego podrás actualizarlo con PUT.

El problema está en el **frontend**, que debe distinguir entre registrar (POST) y actualizar (PUT).
