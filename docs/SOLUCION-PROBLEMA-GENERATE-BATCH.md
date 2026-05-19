# 🔧 Solución: Problema con generateBatchList

## 📋 Problema Identificado

Estás usando la API `/api/sigmav2/labels/generate/batch` con estos datos:

```json
{
  "warehouseId": 8,
  "periodId": 1,
  "products": [
    {
      "productId": 94,
      "labelsToGenerate": 1
    }
  ]
}
```

**Marbetes generados en:** Almacén 8  
**Intentas imprimir desde:** Almacén 10

**Resultado:**
```json
{
  "count": 0,
  "periodId": 1,
  "warehouseId": 10,
  "warehouseName": "Almacén 1",
  "periodName": "2025-12-01"
}
```

---

## 🔍 Análisis del Código

### Función `generateBatchList()`:

```java
public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
    warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
    
    for (ProductBatchDTO product : dto.getProducts()) {
        try {
            GenerateBatchDTO single = new GenerateBatchDTO();
            single.setProductId(product.getProductId());
            single.setWarehouseId(dto.getWarehouseId());  // ⭐ USA warehouseId del DTO
            single.setPeriodId(dto.getPeriodId());
            single.setLabelsToGenerate(product.getLabelsToGenerate());
            
            // Llama a generateBatch() para cada producto
            this.generateBatch(single, userId, userRole);
        } catch (Exception e) {
            log.error("Error generando marbetes para producto {}: {}", 
                product.getProductId(), e.getMessage());
        }
    }
}
```

### Función `generateBatch()`:

```java
public GenerateBatchResponseDTO generateBatch(GenerateBatchDTO dto, Long userId, String userRole) {
    // Buscar solicitud existente
    Optional<LabelRequest> opt = persistence.findByProductWarehousePeriod(
        dto.getProductId(), dto.getWarehouseId(), dto.getPeriodId()
    );
    
    if (opt.isEmpty()) {
        // ❌ ERROR: No existe solicitud previa
        throw new LabelNotFoundException(
            "No existe una solicitud para el producto/almacén/periodo."
        );
    }
    
    // ... genera marbetes ...
}
```

---

## ⚠️ Problema Principal

**La API `/generate/batch` REQUIERE que primero hayas llamado a `/request`** para cada producto. Si no existe la solicitud previa, falla silenciosamente.

### Flujo INCORRECTO (tu caso actual):

```
1. ❌ Llamas a /generate/batch directamente
   └─> Intenta generar sin solicitud previa
   └─> Error: "No existe una solicitud para el producto/almacén/periodo"
   └─> El error se captura y se ignora (log.error)
   └─> NO se generan marbetes
   
2. ✅ Llamas a /pending-print-count en almacén 10
   └─> count: 0 (porque los marbetes están en almacén 8, o no se generaron)
```

### Flujo CORRECTO:

```
1. ✅ Llamas a /request para cada producto
   └─> Crea solicitud en label_requests
   
2. ✅ Llamas a /generate/batch
   └─> Genera marbetes basándose en las solicitudes
   
3. ✅ Llamas a /pending-print-count en el MISMO almacén
   └─> count: X (donde X > 0)
   
4. ✅ Llamas a /print en el MISMO almacén
   └─> Genera PDF e imprime
```

---

## ✅ Solución 1: Usar el Flujo Correcto

### Opción A: Frontend debe llamar a `/request` primero

```javascript
// PASO 1: Solicitar folios para cada producto
for (const product of productos) {
  await axios.post('/api/sigmav2/labels/request', {
    productId: product.productId,
    warehouseId: almacenSeleccionado,  // ⚠️ MISMO almacén
    periodId: periodoActual,
    requestedLabels: product.labelsToGenerate
  });
}

// PASO 2: Generar marbetes en lote
await axios.post('/api/sigmav2/labels/generate/batch', {
  warehouseId: almacenSeleccionado,  // ⚠️ MISMO almacén
  periodId: periodoActual,
  products: productos.map(p => ({
    productId: p.productId,
    labelsToGenerate: p.labelsToGenerate
  }))
});

// PASO 3: Verificar pendientes en el MISMO almacén
const count = await axios.post('/api/sigmav2/labels/pending-print-count', {
  periodId: periodoActual,
  warehouseId: almacenSeleccionado  // ⚠️ MISMO almacén
});

// PASO 4: Si count > 0, imprimir
if (count.data.count > 0) {
  await axios.post('/api/sigmav2/labels/print', {
    periodId: periodoActual,
    warehouseId: almacenSeleccionado  // ⚠️ MISMO almacén
  });
}
```

---

## ✅ Solución 2: Modificar el Backend para Crear Solicitudes Automáticamente

Modificar `generateBatchList()` para que cree las solicitudes automáticamente si no existen:

```java
@Override
@Transactional
public void generateBatchList(GenerateBatchListDTO dto, Long userId, String userRole) {
    log.info("Generando marbetes en lote para {} productos", dto.getProducts().size());
    
    warehouseAccessService.validateWarehouseAccess(userId, dto.getWarehouseId(), userRole);
    
    for (ProductBatchDTO product : dto.getProducts()) {
        try {
            // ✅ CREAR SOLICITUD AUTOMÁTICAMENTE SI NO EXISTE
            Optional<LabelRequest> existingRequest = persistence.findByProductWarehousePeriod(
                product.getProductId(),
                dto.getWarehouseId(),
                dto.getPeriodId()
            );
            
            if (existingRequest.isEmpty()) {
                // Crear solicitud automáticamente
                log.info("Creando solicitud automática para producto {}", product.getProductId());
                
                LabelRequestDTO requestDTO = new LabelRequestDTO();
                requestDTO.setProductId(product.getProductId());
                requestDTO.setWarehouseId(dto.getWarehouseId());
                requestDTO.setPeriodId(dto.getPeriodId());
                requestDTO.setRequestedLabels(product.getLabelsToGenerate());
                
                this.requestLabels(requestDTO, userId, userRole);
            }
            
            // Generar marbetes
            GenerateBatchDTO single = new GenerateBatchDTO();
            single.setProductId(product.getProductId());
            single.setWarehouseId(dto.getWarehouseId());
            single.setPeriodId(dto.getPeriodId());
            single.setLabelsToGenerate(product.getLabelsToGenerate());
            
            this.generateBatch(single, userId, userRole);
            
            log.info("Marbetes generados exitosamente para producto {}", product.getProductId());
            
        } catch (Exception e) {
            log.error("Error generando marbetes para producto {}: {}", 
                product.getProductId(), e.getMessage(), e);
            // Considerar lanzar la excepción en lugar de solo loggear
            throw new RuntimeException(
                "Error generando marbetes para producto " + product.getProductId() + ": " + e.getMessage()
            );
        }
    }
}
```

---

## ✅ Solución 3: Verificar Consistencia de Almacenes

### Problema Detectado:

- **Generas en:** `warehouseId: 8`
- **Imprimes en:** `warehouseId: 10`

### Verificación en Frontend:

```javascript
// Guardar el almacén donde se generaron los marbetes
let almacenDeGeneracion = null;

async function generarMarbetes() {
  const almacenSeleccionado = obtenerAlmacenSeleccionado();
  
  await axios.post('/api/sigmav2/labels/generate/batch', {
    warehouseId: almacenSeleccionado,
    periodId: periodoActual,
    products: productos
  });
  
  // ⭐ Guardar para usar en impresión
  almacenDeGeneracion = almacenSeleccionado;
}

async function imprimirMarbetes() {
  // ⚠️ VERIFICAR que sea el mismo almacén
  const almacenParaImprimir = obtenerAlmacenSeleccionado();
  
  if (almacenDeGeneracion !== almacenParaImprimir) {
    alert(`⚠️ Error: Los marbetes fueron generados en el almacén ${almacenDeGeneracion}, 
           pero intentas imprimir del almacén ${almacenParaImprimir}`);
    return;
  }
  
  // Verificar pendientes
  const count = await axios.post('/api/sigmav2/labels/pending-print-count', {
    periodId: periodoActual,
    warehouseId: almacenParaImprimir
  });
  
  if (count.data.count === 0) {
    alert('No hay marbetes pendientes en este almacén');
    return;
  }
  
  // Imprimir
  await axios.post('/api/sigmav2/labels/print', {
    periodId: periodoActual,
    warehouseId: almacenParaImprimir
  });
}
```

---

## 🔍 Diagnóstico: ¿Dónde Están los Marbetes?

### Query SQL para verificar:

```sql
-- Ver marbetes generados por almacén
SELECT 
    warehouse_id,
    estado,
    COUNT(*) as cantidad
FROM labels
WHERE period_id = 1
GROUP BY warehouse_id, estado
ORDER BY warehouse_id, estado;

-- Ver marbetes del producto específico
SELECT 
    folio,
    product_id,
    warehouse_id,
    period_id,
    estado,
    created_at,
    impreso_at
FROM labels
WHERE product_id = 94 
  AND period_id = 1
ORDER BY folio;

-- Ver solicitudes de marbetes
SELECT 
    id,
    product_id,
    warehouse_id,
    period_id,
    requested_labels,
    folios_generados
FROM label_requests
WHERE period_id = 1
  AND product_id = 94;
```

---

## 📊 Checklist de Verificación

- [ ] ¿Llamaste a `/request` antes de `/generate/batch`?
- [ ] ¿El `warehouseId` es el MISMO en todas las llamadas?
- [ ] ¿El `periodId` es el MISMO en todas las llamadas?
- [ ] ¿Verificaste con `/pending-print-count` en el almacén CORRECTO?
- [ ] ¿Los marbetes se generaron exitosamente (sin errores en logs)?
- [ ] ¿El producto existe en el catálogo de inventario?

---

## 🎯 Recomendación Final

**Implementa la Solución 2** (modificar el backend) porque:

1. ✅ Hace el proceso más simple para el frontend
2. ✅ Elimina un paso manual propenso a errores
3. ✅ La API `/generate/batch` se vuelve más intuitiva
4. ✅ Evita inconsistencias entre solicitudes y generaciones

**Y además:**
- Asegúrate de usar el MISMO `warehouseId` en todas las operaciones
- Verifica con queries SQL dónde están los marbetes
- Implementa validación en el frontend para evitar cambios de almacén

---

## 💡 Ejemplo Completo Funcional

```javascript
// ConsultaCaptura.vue o similar

async function generarEImprimirMarbetes() {
  const almacen = 8;  // ⚠️ MISMO en todo el flujo
  const periodo = 1;
  const productos = [
    { productId: 94, labelsToGenerate: 1 }
  ];
  
  try {
    // PASO 1: Generar (con backend modificado, crea solicitudes automáticamente)
    console.log('Generando marbetes...');
    await axios.post('/api/sigmav2/labels/generate/batch', {
      warehouseId: almacen,
      periodId: periodo,
      products: productos
    });
    
    // PASO 2: Verificar pendientes
    console.log('Verificando marbetes pendientes...');
    const countResponse = await axios.post('/api/sigmav2/labels/pending-print-count', {
      periodId: periodo,
      warehouseId: almacen
    });
    
    console.log(`Marbetes pendientes: ${countResponse.data.count}`);
    
    if (countResponse.data.count === 0) {
      alert('❌ No se generaron marbetes. Revisa los logs del backend.');
      return;
    }
    
    // PASO 3: Imprimir
    console.log('Imprimiendo marbetes...');
    const pdfResponse = await axios.post('/api/sigmav2/labels/print', {
      periodId: periodo,
      warehouseId: almacen
    }, {
      responseType: 'blob'
    });
    
    // PASO 4: Descargar PDF
    const blob = new Blob([pdfResponse.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `marbetes_almacen${almacen}.pdf`;
    link.click();
    
    console.log('✅ Impresión completada');
    
  } catch (error) {
    console.error('Error:', error);
    if (error.response && error.response.data) {
      alert(`Error: ${error.response.data.message || error.response.data.error}`);
    }
  }
}
```

---

**Documento generado:** 2025-12-29  
**Problema:** Marbetes generados en almacén incorrecto  
**Solución:** Modificar backend + verificar consistencia de almacenes

