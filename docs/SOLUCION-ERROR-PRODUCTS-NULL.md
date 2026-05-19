# 🔧 Solución al Error de Validación - "products no debe ser nulo"

**Error:**
```json
{
  "fieldErrors": {
    "products": "no debe ser nulo"
  },
  "success": false,
  "message": "Error de validación en los campos",
  "error": "VALIDATION_ERROR"
}
```

---

## 🔍 Causa del Error

El campo `products` está llegando como `null` o no está siendo enviado correctamente en el request.

---

## ✅ SOLUCIÓN: Formato Correcto del Request

### ❌ INCORRECTO:

```javascript
// Error: Falta el campo "products"
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,
  periodId: 1
  // ❌ Falta "products"
});

// Error: Campo con nombre incorrecto
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,
  periodId: 1,
  product: { ... }  // ❌ Se llama "products" (plural)
});

// Error: products está vacío
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,
  periodId: 1,
  products: []  // ❌ Debe tener al menos 1 producto
});
```

### ✅ CORRECTO:

```javascript
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,
  periodId: 1,
  products: [  // ✅ Array de productos (plural)
    {
      productId: 94,
      labelsToGenerate: 5
    }
  ]
}, {
  responseType: 'blob'  // ✅ Importante para recibir PDF
});
```

---

## 📋 Estructura Completa del Request

### Campos Requeridos:

```typescript
interface GenerateAndPrintRequest {
  warehouseId: number;      // ✅ Requerido
  periodId: number;         // ✅ Requerido
  products: Array<{         // ✅ Requerido (array con al menos 1 elemento)
    productId: number;      // ✅ Requerido
    labelsToGenerate: number; // ✅ Requerido (mínimo 1)
  }>;
}
```

### Ejemplo con Validación:

```javascript
async function generarEImprimirMarbetes(productos) {
  // Validar que haya productos
  if (!productos || productos.length === 0) {
    alert('❌ Debes seleccionar al menos un producto');
    return;
  }

  // Validar formato de productos
  const productosValidos = productos.map(p => ({
    productId: p.productId || p.id,  // Asegurar que tenga productId
    labelsToGenerate: parseInt(p.labelsToGenerate) || 1  // Asegurar que sea número
  }));

  try {
    const response = await axios.post(
      '/api/sigmav2/labels/generate-and-print',
      {
        warehouseId: obtenerAlmacenActual(),
        periodId: obtenerPeriodoActual(),
        products: productosValidos  // ✅ Array de productos validado
      },
      {
        responseType: 'blob'
      }
    );

    // Descargar PDF
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'marbetes.pdf';
    link.click();

    alert('✅ Marbetes generados e impresos exitosamente');

  } catch (error) {
    if (error.response?.data?.fieldErrors) {
      // Error de validación
      const errores = error.response.data.fieldErrors;
      alert('❌ Error de validación: ' + JSON.stringify(errores));
    } else if (error.response?.data?.message) {
      alert('❌ Error: ' + error.response.data.message);
    } else {
      alert('❌ Error inesperado');
    }
    console.error('Error completo:', error);
  }
}
```

---

## 🎯 Ejemplos de Uso Correcto

### Ejemplo 1: Un solo producto

```javascript
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,
  periodId: 1,
  products: [
    {
      productId: 94,
      labelsToGenerate: 1
    }
  ]
}, { responseType: 'blob' });
```

### Ejemplo 2: Múltiples productos

```javascript
await axios.post('/api/sigmav2/labels/generate-and-print', {
  warehouseId: 8,
  periodId: 1,
  products: [
    { productId: 94, labelsToGenerate: 5 },
    { productId: 95, labelsToGenerate: 3 },
    { productId: 96, labelsToGenerate: 10 }
  ]
}, { responseType: 'blob' });
```

### Ejemplo 3: Desde un formulario Vue/React

```javascript
// Vue Component
data() {
  return {
    almacenId: 8,
    periodoId: 1,
    productosSeleccionados: [
      { id: 94, cantidad: 5 },
      { id: 95, cantidad: 3 }
    ]
  }
},
methods: {
  async imprimirMarbetes() {
    try {
      const response = await axios.post(
        '/api/sigmav2/labels/generate-and-print',
        {
          warehouseId: this.almacenId,
          periodId: this.periodoId,
          products: this.productosSeleccionados.map(p => ({
            productId: p.id,
            labelsToGenerate: p.cantidad
          }))
        },
        { responseType: 'blob' }
      );

      // Descargar PDF
      const blob = new Blob([response.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      window.open(url);

    } catch (error) {
      console.error('Error:', error);
      alert('Error: ' + (error.response?.data?.message || 'Error desconocido'));
    }
  }
}
```

---

## 🐛 Debugging: Verificar el Request

### Console.log antes de enviar:

```javascript
const requestData = {
  warehouseId: 8,
  periodId: 1,
  products: [
    { productId: 94, labelsToGenerate: 5 }
  ]
};

console.log('Request a enviar:', JSON.stringify(requestData, null, 2));

await axios.post('/api/sigmav2/labels/generate-and-print', requestData, {
  responseType: 'blob'
});
```

**Salida esperada:**
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

---

## ⚠️ Errores Comunes

### Error 1: Campo "product" en singular
```javascript
// ❌ MAL
products: { productId: 94, labelsToGenerate: 5 }

// ✅ BIEN
products: [{ productId: 94, labelsToGenerate: 5 }]
```

### Error 2: Array vacío
```javascript
// ❌ MAL
products: []

// ✅ BIEN
products: [{ productId: 94, labelsToGenerate: 1 }]
```

### Error 3: Campo products null o undefined
```javascript
// ❌ MAL
{
  warehouseId: 8,
  periodId: 1
  // products está faltando
}

// ✅ BIEN
{
  warehouseId: 8,
  periodId: 1,
  products: [{ productId: 94, labelsToGenerate: 1 }]
}
```

### Error 4: labelsToGenerate = 0
```javascript
// ❌ MAL
products: [{ productId: 94, labelsToGenerate: 0 }]

// ✅ BIEN
products: [{ productId: 94, labelsToGenerate: 1 }]
```

---

## 🔍 Validaciones del Backend

El DTO valida:

```java
@NotNull
private List<ProductBatchDTO> products;  // No puede ser null

public static class ProductBatchDTO {
    @NotNull
    private Long productId;  // No puede ser null
    
    @NotNull
    @Min(1)
    private Integer labelsToGenerate;  // No puede ser null, mínimo 1
}
```

**Reglas:**
- ✅ `products` debe ser un array (no null)
- ✅ `products` debe tener al menos 1 elemento
- ✅ Cada producto debe tener `productId`
- ✅ Cada producto debe tener `labelsToGenerate >= 1`

---

## 📱 Ejemplo Completo con Vue

```vue
<template>
  <div>
    <h2>Generar e Imprimir Marbetes</h2>
    
    <!-- Selector de productos -->
    <div v-for="(producto, index) in productos" :key="index">
      <input 
        v-model.number="producto.productId" 
        placeholder="ID Producto"
      />
      <input 
        v-model.number="producto.labelsToGenerate" 
        placeholder="Cantidad"
        min="1"
      />
      <button @click="eliminarProducto(index)">Eliminar</button>
    </div>

    <button @click="agregarProducto">+ Agregar Producto</button>
    <button @click="generarEImprimir">Generar e Imprimir</button>
  </div>
</template>

<script>
export default {
  data() {
    return {
      almacenId: 8,
      periodoId: 1,
      productos: [
        { productId: null, labelsToGenerate: 1 }
      ]
    }
  },
  methods: {
    agregarProducto() {
      this.productos.push({ productId: null, labelsToGenerate: 1 });
    },
    eliminarProducto(index) {
      this.productos.splice(index, 1);
    },
    async generarEImprimir() {
      // Validar que todos los productos tengan ID
      const productosValidos = this.productos.filter(p => p.productId);
      
      if (productosValidos.length === 0) {
        alert('❌ Debes agregar al menos un producto válido');
        return;
      }

      try {
        console.log('Enviando request:', {
          warehouseId: this.almacenId,
          periodId: this.periodoId,
          products: productosValidos
        });

        const response = await this.$axios.post(
          '/api/sigmav2/labels/generate-and-print',
          {
            warehouseId: this.almacenId,
            periodId: this.periodoId,
            products: productosValidos.map(p => ({
              productId: parseInt(p.productId),
              labelsToGenerate: parseInt(p.labelsToGenerate) || 1
            }))
          },
          { responseType: 'blob' }
        );

        // Descargar PDF
        const blob = new Blob([response.data], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `marbetes_${Date.now()}.pdf`;
        link.click();

        this.$swal('✅ Éxito', 'Marbetes generados e impresos', 'success');

      } catch (error) {
        console.error('Error:', error);
        
        if (error.response?.data?.fieldErrors) {
          const errores = Object.entries(error.response.data.fieldErrors)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join('\n');
          this.$swal('❌ Error de Validación', errores, 'error');
        } else {
          this.$swal('❌ Error', error.response?.data?.message || 'Error desconocido', 'error');
        }
      }
    }
  }
}
</script>
```

---

## ✅ Checklist de Verificación

Antes de enviar el request, verifica:

- [ ] El campo `products` está presente
- [ ] `products` es un array (no un objeto)
- [ ] `products` tiene al menos 1 elemento
- [ ] Cada producto tiene `productId` (número)
- [ ] Cada producto tiene `labelsToGenerate` (número >= 1)
- [ ] `warehouseId` está presente (número)
- [ ] `periodId` está presente (número)
- [ ] La configuración incluye `responseType: 'blob'`

---

## 🎯 Resumen

**El error se debe a:**
1. El campo `products` está null/undefined
2. O el campo `products` es un objeto en lugar de array
3. O el campo `products` está vacío

**La solución:**
```javascript
// ✅ FORMATO CORRECTO
{
  warehouseId: 8,
  periodId: 1,
  products: [  // Array con al menos 1 elemento
    {
      productId: 94,
      labelsToGenerate: 5
    }
  ]
}
```

---

**Documento generado:** 2025-12-29  
**Problema:** Validación "products no debe ser nulo"  
**Solución:** Asegurar que `products` sea un array no vacío

