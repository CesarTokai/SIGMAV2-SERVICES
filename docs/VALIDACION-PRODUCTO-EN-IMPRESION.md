# 🔍 Análisis: Validación de Productos en Impresión de Marbetes

**Fecha:** 2026-03-19  
**Pregunta:** ¿Hay validación que bloquee imprimir marbetes generados sin productos?

---

## ✅ RESPUESTA: SÍ, EXISTE VALIDACIÓN (Y ES CORRECTA)

### Ubicación del Código

**Archivo:** `JasperLabelPrintService.java` (líneas 150-154)

```java
ProductEntity product = productsCache.get(label.getProductId());
if (product == null) {
    log.error("CRÍTICO: Producto no encontrado para folio {}: productId={}", 
        label.getFolio(), label.getProductId());
    throw new IllegalStateException(
        String.format("No se puede generar PDF: El folio %d está asociado a un producto inexistente (ID: %d). " +
            "Esto indica datos huérfanos en la base de datos. Por favor, contacte al administrador del sistema.",
            label.getFolio(), label.getProductId()));
}
```

---

## 🎯 ¿Por Qué Existe Esta Validación?

### Razón 1: Integridad de Datos
- El marbete (label) tiene un campo `id_product` que es **obligatorio**
- Si ese producto no existe en la BD, hay **datos huérfanos**
- Esto indica un error grave del sistema

### Razón 2: Plantilla JRXML Requiere Datos del Producto
La plantilla PDF necesita mostrar:
- ✅ Código del producto (`cve_art`)
- ✅ Descripción (`descr`)
- ✅ Unidad de medida (`uni_med`)

Si el producto no existe → ❌ No puede generar PDF válido

### Razón 3: Regla de Negocio Explícita

**Según `README-MARBETES-REGLAS-NEGOCIO.md`:**

```
No se puede imprimir un marbete sin:
1. ✅ Período válido
2. ✅ Almacén válido
3. ✅ Producto válido
4. ✅ Estado correcto (GENERADO o IMPRESO)
```

---

## 📊 Validaciones Completas en Impresión

| Validación | Ubicación | Estado | Crítica |
|-----------|-----------|--------|---------|
| Período existe | `LabelServiceImpl:170` | ✅ Implícita | Sí |
| Almacén existe | `LabelServiceImpl:170` | ✅ Implícita | Sí |
| **Producto existe** | `JasperLabelPrintService:150` | ✅ Explícita | **SÍ** |
| Marbete estado GENERADO | `LabelServiceImpl:175-186` | ✅ Explícita | Sí |
| Marbete estado IMPRESO (extraordinaria) | `LabelServiceImpl:164-170` | ✅ Explícita | Sí |
| Max 500 marbetes | `LabelServiceImpl:189` | ✅ Explícita | No |
| Usuario tiene rol permitido | `LabelsController:68` | ✅ Explícita | Sí |
| Usuario acceso a almacén | `LabelServiceImpl:172` | ✅ Explícita | Sí |

---

## 🔴 Cuándo Se Dispara Este Error

### Escenario 1: Eliminación de Producto Accidental
```
1. Se generan 100 marbetes del producto 5
2. Alguien BORRA el producto 5 de la base de datos
3. Intentas imprimir → ❌ ERROR: Producto no encontrado
```

### Escenario 2: Sincronización Fallida
```
1. Base de datos inconsistente (datos huérfanos)
2. Un folio apunta a productId=999 que no existe
3. Intentas imprimir → ❌ ERROR: Producto inexistente
```

### Escenario 3: Corrupción de Datos
```
1. Fallo en migración de BD
2. Algunos folios tienen id_product=NULL o inválido
3. Intentas imprimir → ❌ ERROR: Datos corrompidos
```

---

## ✅ ¿Es Correcta Esta Validación?

### Sí, por estas razones:

1. **Previene Errores en PDF**
   - La plantilla JRXML necesita datos del producto
   - Sin producto → PDF incompleto/inválido

2. **Detecta Problemas de Integridad**
   - Identifica datos huérfanos
   - Alerta sobre corrupción de BD

3. **Mejora la Experiencia**
   - Error claro y descriptivo
   - Instruye contactar al administrador
   - No genera PDF "roto"

4. **Cumple Reglas de Negocio**
   - No se pueden imprimir marbetes sin producto válido
   - Mantiene consistencia de datos

---

## 📋 Carga de Datos (PreCondición)

Para que **nunca** ocurra este error:

### ✅ Asegurar en Generación de Marbetes:
```java
// En LabelGenerationService.generateBatchList()
// Antes de crear Labels, validar que el producto existe:

for (ProductBatchDTO product : dto.getProducts()) {
    // ✅ VERIFICAR: El producto existe
    Optional<ProductEntity> productExists = 
        productRepository.findById(product.getProductId());
    
    if (productExists.isEmpty()) {
        throw new ProductNotFoundException(
            "No se puede generar marbetes: Producto " + 
            product.getProductId() + " no existe");
    }
    // ... continuar con generación
}
```

---

## 🛠️ ¿Qué Hacer Si Recibes Este Error?

### ⚠️ MEJOR: Prevenir el Error en Generación

**NUEVA VALIDACIÓN AGREGADA** (LabelGenerationService.java)

Ahora se valida que los productos existen CUANDO GENERAS marbetes, no cuando imprimes:

```java
// En generateBatchList()
List<Long> productosNoEncontrados = new ArrayList<>();
for (ProductBatchDTO product : dto.getProducts()) {
    if (!productRepository.existsById(product.getProductId())) {
        productosNoEncontrados.add(product.getProductId());
    }
}

if (!productosNoEncontrados.isEmpty()) {
    throw new InvalidLabelStateException(
        "No se pueden generar marbetes: Los siguientes productos no existen: " + 
        productosNoEncontrados);
}
```

**Ventajas:**
- ✅ Error más temprano (en generación, no en impresión)
- ✅ Previene crear marbetes huérfanos
- ✅ Mensaje claro que indica cuál producto falta
- ✅ Facilita debugging

---

### Opción 1: Verificar Integridad de BD
```sql
-- Ver marbetes huérfanos
SELECT l.folio, l.id_product, l.id_warehouse, l.id_period
FROM labels l
LEFT JOIN products p ON l.id_product = p.id_product
WHERE p.id_product IS NULL;

-- Mostrar productos y sus marbetes
SELECT p.id_product, COUNT(l.folio) as total_marbetes
FROM products p
LEFT JOIN labels l ON p.id_product = l.id_product
GROUP BY p.id_product
ORDER BY total_marbetes DESC;
```

### Opción 2: Restaurar Datos
```sql
-- Si existen productos buscados, restaurar desde backup
-- O recarga el catálogo desde Excel
```

### Opción 3: Contactar Administrador
- Indica los folios que generan error
- Proporciona el período y almacén
- Incluye el ID del producto inexistente

---

## 📝 Recomendación

**✅ VALIDACIÓN DOBLE - MEJOR PRÁCTICA**

1. **Primera línea:** Validación en GENERACIÓN (NUEVA)
   - Previene crear marbetes sin producto
   - Error más temprano y claro
   - Archivo: `LabelGenerationService.java`

2. **Segunda línea:** Validación en IMPRESIÓN (EXISTENTE)
   - Previene PDF defectuosos
   - Detecta datos huérfanos
   - Archivo: `JasperLabelPrintService.java`

**Flujo Seguro:**
```
Generar → ❌ Valida productos en catálogo
           ✅ Genera marbetes con productos válidos
           ↓
Imprimir → ❌ Valida productos no nulos
           ✅ Genera PDF completo
```

---

## 📚 Referencias

- `JasperLabelPrintService.java:150-154` - Validación de impresión
- `LabelGenerationService.java:77-90` - **NUEVA: Validación de generación**
- `LabelServiceImpl.printLabels()` - Lógica de impresión
- `README-MARBETES-REGLAS-NEGOCIO.md` - Reglas de negocio
- `IMPLEMENTACION-REGLAS-NEGOCIO-MARBETES.md` - Validaciones

