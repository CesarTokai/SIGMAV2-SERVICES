# ✅ SOLUCIÓN: API /summary Solo Muestra Algunos Productos

**Problema:** La API `/summary` solo devuelve 10 productos cuando hay muchos más.

**Causa:** **PAGINACIÓN** - El endpoint está paginado por defecto.

---

## 🎯 Explicación

El método `getLabelSummary()` implementa paginación:

```java
// Parámetros de paginación
int start = dto.getPage() * dto.getSize();  // page * size
int end = Math.min(start + dto.getSize(), totalFiltered);

// Ejemplo:
// page = 0, size = 10 → muestra productos 0-9 (10 productos)
// page = 1, size = 10 → muestra productos 10-19 (siguiente página)
```

---

## ✅ SOLUCIONES

### Solución 1: Aumentar el Tamaño de Página (Recomendado)

**Request:**
```json
POST /api/sigmav2/labels/summary
{
  "periodId": 16,
  "warehouseId": 1,
  "page": 0,
  "size": 1000,        // ← AUMENTAR ESTO
  "searchText": "",
  "sortBy": "claveProducto",
  "sortDirection": "ASC"
}
```

**Esto devolverá hasta 1000 productos en una sola llamada.**

---

### Solución 2: Obtener Todas las Páginas

Si hay más de 1000 productos, necesitas hacer múltiples llamadas:

```javascript
async function getAllProducts(periodId, warehouseId) {
  const allProducts = [];
  let page = 0;
  const size = 100;
  let hasMore = true;

  while (hasMore) {
    const response = await fetch('/api/sigmav2/labels/summary', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        periodId: periodId,
        warehouseId: warehouseId,
        page: page,
        size: size,
        searchText: "",
        sortBy: "claveProducto",
        sortDirection: "ASC"
      })
    });

    const products = await response.json();

    if (products.length === 0) {
      hasMore = false;
    } else {
      allProducts.push(...products);
      page++;
    }
  }

  return allProducts;
}
```

---

### Solución 3: Modificar el Endpoint (Opción Backend)

Si quieres que el endpoint devuelva TODOS los productos sin paginación cuando no se especifique `size`, puedo modificar el código.

---

## 🔍 Verificación

Para ver cuántos productos hay en total:

```sql
-- Contar productos en inventario para periodo 16, almacén 1
SELECT COUNT(DISTINCT p.id_product) as total_productos
FROM product p
WHERE EXISTS (
    SELECT 1 FROM inventory_stock inv
    WHERE inv.id_product = p.id_product
    AND inv.id_warehouse = 1
    AND inv.id_period = 16
);
```

---

## 📊 Parámetros del Request

El DTO `LabelSummaryRequestDTO` acepta:

| Parámetro | Tipo | Defecto | Descripción |
|-----------|------|---------|-------------|
| `periodId` | Long | Último periodo | ID del periodo |
| `warehouseId` | Long | Primer almacén | ID del almacén |
| `page` | Integer | 0 | Número de página (inicia en 0) |
| `size` | Integer | 10 | Tamaño de página |
| `searchText` | String | "" | Texto de búsqueda |
| `sortBy` | String | "claveProducto" | Campo para ordenar |
| `sortDirection` | String | "ASC" | ASC o DESC |

---

## 💡 Ejemplo Completo

### Request para Ver TODOS:

```json
POST /api/sigmav2/labels/summary
{
  "periodId": 16,
  "warehouseId": 1,
  "page": 0,
  "size": 10000,
  "searchText": "",
  "sortBy": "claveProducto",
  "sortDirection": "ASC"
}
```

**Esto devolverá todos los productos del almacén 1 en el periodo 16.**

---

## 🎯 Tu Caso Específico

Según los datos que compartiste, tienes muchos productos pero solo ves 10 en la respuesta.

**Productos visibles:**
- EQUIPO
- FactGlob
- GM17CRTB8
- GM17CRTC1
- GM17CRTCJ
- GM17CWMB2
- GM17MEXB8
- GM17MEXC1
- GM17MEXCJ
- GM17WLMB8

**Esto sugiere:** `page=0, size=10` (primera página de 10 productos)

**Solución:** Cambiar `size` a 1000 o más.

---

## ✅ Implementación Frontend

### React/TypeScript

```typescript
const [products, setProducts] = useState([]);

useEffect(() => {
  async function loadAllProducts() {
    const response = await fetch('/api/sigmav2/labels/summary', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        periodId: selectedPeriod,
        warehouseId: selectedWarehouse,
        page: 0,
        size: 10000,  // ← TODOS
        searchText: "",
        sortBy: "claveProducto",
        sortDirection: "ASC"
      })
    });

    const data = await response.json();
    setProducts(data);
  }

  loadAllProducts();
}, [selectedPeriod, selectedWarehouse]);
```

---

## 📝 Resumen

**Problema:** Solo ves 10 productos
**Causa:** Paginación con `size=10` por defecto
**Solución:** Aumentar `size` a 1000 o más

**El sistema está funcionando correctamente**, solo necesitas ajustar los parámetros del request.

---

**Fecha:** 2025-12-16
**Estado:** ✅ Problema Identificado - Solución Disponible

