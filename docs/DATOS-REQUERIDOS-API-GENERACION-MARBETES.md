# 📋 Datos Requeridos por la API de Generación de Marbetes

**Fecha:** 2025-12-29  
**API:** `POST /api/sigmav2/labels/generate/batch`

---

## 🎯 Datos que Pide la API

La API requiere **3 campos principales** en el cuerpo de la petición:

### 📦 Estructura del Request:

```json
{
  "warehouseId": 8,          // ✅ REQUERIDO: ID del almacén
  "periodId": 1,             // ✅ REQUERIDO: ID del periodo
  "products": [              // ✅ REQUERIDO: Array de productos (mínimo 1)
    {
      "productId": 94,       // ✅ REQUERIDO: ID del producto
      "labelsToGenerate": 5  // ✅ REQUERIDO: Cantidad de marbetes (mínimo 1)
    }
  ]
}
```

---

## 📝 Descripción de Cada Campo

### 1. `warehouseId` (Long)
- **Descripción:** ID del almacén donde se generarán los marbetes
- **Tipo:** Número entero
- **Requerido:** ✅ Sí
- **Validación:** No puede ser `null`
- **Ejemplo:** `8`, `10`, `14`

### 2. `periodId` (Long)
- **Descripción:** ID del periodo de inventario
- **Tipo:** Número entero
- **Requerido:** ✅ Sí
- **Validación:** No puede ser `null`
- **Ejemplo:** `1`, `2`, `3`

### 3. `products` (Array)
- **Descripción:** Lista de productos para los cuales generar marbetes
- **Tipo:** Array de objetos
- **Requerido:** ✅ Sí
- **Validación:** 
  - No puede ser `null`
  - No puede estar vacío
  - Debe tener al menos 1 producto
- **Estructura de cada producto:**
  - `productId` (Long) - ID del producto ✅ Requerido
  - `labelsToGenerate` (Integer) - Cantidad de marbetes ✅ Requerido (mínimo 1)

---

## ✅ Ejemplos Válidos

### Ejemplo 1: Un solo producto
```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": [
    {
      "productId": 94,
      "labelsToGenerate": 5
    }
  ]
}
```

### Ejemplo 2: Múltiples productos
```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": [
    {
      "productId": 94,
      "labelsToGenerate": 5
    },
    {
      "productId": 95,
      "labelsToGenerate": 3
    },
    {
      "productId": 96,
      "labelsToGenerate": 10
    }
  ]
}
```

### Ejemplo 3: Generar solo 1 marbete
```json
{
  "warehouseId": 10,
  "periodId": 2,
  "products": [
    {
      "productId": 150,
      "labelsToGenerate": 1
    }
  ]
}
```

---

## ❌ Ejemplos Inválidos (Con Errores)

### Error 1: Falta el campo `products`
```json
{
  "warehouseId": 8,
  "periodId": 1
  // ❌ ERROR: Falta "products"
}
```
**Error:** `"products": "no debe ser nulo"`

### Error 2: `products` está vacío
```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": []  // ❌ ERROR: Array vacío
}
```
**Error:** `"products": "no debe estar vacío"`

### Error 3: Falta `productId` o `labelsToGenerate`
```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": [
    {
      "productId": 94
      // ❌ ERROR: Falta "labelsToGenerate"
    }
  ]
}
```
**Error:** `"labelsToGenerate": "no debe ser nulo"`

### Error 4: `labelsToGenerate` es 0 o negativo
```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": [
    {
      "productId": 94,
      "labelsToGenerate": 0  // ❌ ERROR: Debe ser al menos 1
    }
  ]
}
```
**Error:** `"labelsToGenerate": "debe ser mayor o igual que 1"`

### Error 5: Falta `warehouseId` o `periodId`
```json
{
  "periodId": 1,
  // ❌ ERROR: Falta "warehouseId"
  "products": [
    {
      "productId": 94,
      "labelsToGenerate": 5
    }
  ]
}
```
**Error:** `"warehouseId": "no debe ser nulo"`

---

## 🔍 Validaciones Aplicadas

### Validaciones del DTO:

```java
public class GenerateBatchListDTO {
    @NotNull  // ✅ No puede ser null
    private Long warehouseId;

    @NotNull  // ✅ No puede ser null
    private Long periodId;

    @NotNull  // ✅ No puede ser null
    private List<ProductBatchDTO> products;

    public static class ProductBatchDTO {
        @NotNull  // ✅ No puede ser null
        private Long productId;
        
        @NotNull  // ✅ No puede ser null
        @Min(1)   // ✅ Debe ser al menos 1
        private Integer labelsToGenerate;
    }
}
```

---

## 💻 Código de Ejemplo - Frontend

### JavaScript/Vue/React:

```javascript
async function generarMarbetes() {
  try {
    const response = await axios.post(
      '/api/sigmav2/labels/generate/batch',
      {
        warehouseId: 8,        // ID del almacén actual
        periodId: 1,           // ID del periodo actual
        products: [
          {
            productId: 94,     // ID del producto
            labelsToGenerate: 5 // Cantidad de marbetes
          }
        ]
      }
    );
    
    console.log('✅ Marbetes generados exitosamente');
    
  } catch (error) {
    if (error.response?.data?.fieldErrors) {
      // Errores de validación
      console.error('Errores de validación:', error.response.data.fieldErrors);
    } else {
      console.error('Error:', error.response?.data?.message);
    }
  }
}
```

### Con TypeScript (Tipado):

```typescript
interface GenerateBatchRequest {
  warehouseId: number;
  periodId: number;
  products: Array<{
    productId: number;
    labelsToGenerate: number; // Mínimo 1
  }>;
}

async function generarMarbetes(request: GenerateBatchRequest) {
  try {
    const response = await axios.post<void>(
      '/api/sigmav2/labels/generate/batch',
      request
    );
    console.log('✅ Éxito');
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

// Uso:
generarMarbetes({
  warehouseId: 8,
  periodId: 1,
  products: [
    { productId: 94, labelsToGenerate: 5 }
  ]
});
```

---

## 🎯 Resumen Rápido

### ¿Qué datos necesito enviar?

1. **`warehouseId`** - ¿En qué almacén?
2. **`periodId`** - ¿En qué periodo?
3. **`products`** - ¿Para qué productos y cuántos marbetes?

### Mínimo requerido:

```json
{
  "warehouseId": [número],
  "periodId": [número],
  "products": [
    {
      "productId": [número],
      "labelsToGenerate": [número >= 1]
    }
  ]
}
```

---

## 📊 Tabla de Campos

| Campo | Tipo | Requerido | Validación | Ejemplo |
|-------|------|-----------|------------|---------|
| `warehouseId` | Long | ✅ Sí | No null | `8` |
| `periodId` | Long | ✅ Sí | No null | `1` |
| `products` | Array | ✅ Sí | No null, no vacío | `[...]` |
| `products[].productId` | Long | ✅ Sí | No null | `94` |
| `products[].labelsToGenerate` | Integer | ✅ Sí | No null, >= 1 | `5` |

---

## 🔧 Troubleshooting

### Error: "products no debe ser nulo"
**Causa:** No estás enviando el campo `products`  
**Solución:** Agrega el campo `products` con al menos 1 producto

### Error: "labelsToGenerate debe ser mayor o igual que 1"
**Causa:** Estás enviando 0 o un número negativo  
**Solución:** Usa un número >= 1

### Error: "warehouseId no debe ser nulo"
**Causa:** Falta el campo `warehouseId`  
**Solución:** Agrega el campo con el ID del almacén

### Error: "periodId no debe ser nulo"
**Causa:** Falta el campo `periodId`  
**Solución:** Agrega el campo con el ID del periodo

---

## 🎉 Ejemplo Completo Funcional

```javascript
// Datos de ejemplo
const almacenId = 8;
const periodoId = 1;
const productosSeleccionados = [
  { id: 94, cantidad: 5 },
  { id: 95, cantidad: 3 }
];

// Construir el request
const requestData = {
  warehouseId: almacenId,
  periodId: periodoId,
  products: productosSeleccionados.map(p => ({
    productId: p.id,
    labelsToGenerate: p.cantidad
  }))
};

// Enviar a la API
async function generarMarbetes() {
  try {
    console.log('Enviando:', JSON.stringify(requestData, null, 2));
    
    const response = await axios.post(
      '/api/sigmav2/labels/generate/batch',
      requestData
    );
    
    alert('✅ Marbetes generados exitosamente');
    
  } catch (error) {
    if (error.response?.data?.fieldErrors) {
      const errores = Object.entries(error.response.data.fieldErrors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join('\n');
      alert('❌ Errores de validación:\n' + errores);
    } else {
      alert('❌ Error: ' + (error.response?.data?.message || 'Error desconocido'));
    }
  }
}

generarMarbetes();
```

---

**Documento generado:** 2025-12-29  
**API:** `POST /api/sigmav2/labels/generate/batch`  
**Campos requeridos:** 3 (warehouseId, periodId, products)  
**Estado:** ✅ DOCUMENTADO COMPLETAMENTE

