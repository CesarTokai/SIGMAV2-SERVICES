# PROBLEMA IDENTIFICADO Y SOLUCIÓN — generate/batch

## 🔴 EL PROBLEMA

El endpoint `/api/sigmav2/labels/generate/batch` rechazaba las solicitudes porque la colección de Postman estaba usando un nombre de campo incorrecto.

### Detalles del error:

**Colección de Postman (SIGMAV2-COLLECTION.yaml) — INCORRECTO:**
```json
{
  "periodId": 1,
  "warehouseId": 14,
  "products": [
    { "productId": 123, "requestedLabels": 10 },     ← ❌ INCORRECTO
    { "productId": 124, "requestedLabels": 5 }
  ]
}
```

**DTO del Backend (GenerateBatchListDTO.java) — CORRECTO:**
```java
public static class ProductBatchDTO {
    @NotNull
    private Long productId;
    @NotNull
    @Min(1)
    private Integer labelsToGenerate;              // ✅ Nombre correcto
}
```

### Resultado:
- **Campo enviado:** `requestedLabels`
- **Campo esperado:** `labelsToGenerate`
- **Serialización:** El campo no se mapea → `labelsToGenerate` queda `null`
- **Validación:** Spring rechaza porque `@NotNull` falla
- **Error HTTP:** 400 Bad Request + mensaje de validación

---

## ✅ LA SOLUCIÓN

Actualizar **SIGMAV2-COLLECTION.yaml** para usar el nombre correcto:

### ✏️ Cambio 1: Endpoint /generate/batch
**Líneas 3587-3589:**
```yaml
# ANTES (❌ INCORRECTO):
"products": [
  { "productId": 123, "requestedLabels": 10 },
  { "productId": 124, "requestedLabels": 5 },
  { "productId": 125, "requestedLabels": 15 }
]

# DESPUÉS (✅ CORRECTO):
"products": [
  { "productId": 123, "labelsToGenerate": 10 },
  { "productId": 124, "labelsToGenerate": 5 },
  { "productId": 125, "labelsToGenerate": 15 }
]
```

### ✏️ Cambio 2: Endpoint /generate-and-print
**Líneas 3623-3624:**
```yaml
# ANTES (❌ INCORRECTO):
"products": [
  { "productId": 123, "requestedLabels": 10 }
]

# DESPUÉS (✅ CORRECTO):
"products": [
  { "productId": 123, "labelsToGenerate": 10 }
]
```

---

## 📝 NOTAS IMPORTANTES

### Diferencia entre endpoints

| Endpoint | Campo | Propósito |
|----------|-------|----------|
| **POST /request** | `requestedLabels` | Solicita folios para UN solo producto |
| **POST /generate/batch** | `labelsToGenerate` | Genera marbetes para MÚLTIPLES productos |
| **POST /generate-and-print** | `labelsToGenerate` | Genera + imprime para MÚLTIPLES productos |

El field `requestedLabels` en `/request` es correcto porque se refiere a la solicitud de folios. En los endpoints `/generate/batch` y `/generate-and-print`, se usa `labelsToGenerate` porque son números de marbetes a generar.

---

## 🔍 VALIDACIÓN

El DTO `GenerateBatchListDTO.ProductBatchDTO` requiere:

```java
@Getter
@Setter
public static class ProductBatchDTO {
    @NotNull
    private Long productId;              // ← ID del producto (requerido)
    @NotNull
    @Min(1)
    private Integer labelsToGenerate;    // ← Cantidad de marbetes (requerido, > 0)
}
```

**Validaciones ejecutadas:**
1. ✅ `productId` es NOT NULL
2. ✅ `labelsToGenerate` es NOT NULL
3. ✅ `labelsToGenerate` >= 1
4. ✅ El producto existe en el catálogo
5. ✅ El producto no tiene marbetes generados previos en el mismo período/almacén

---

## 📋 CAMBIOS REALIZADOS

**Archivo modificado:** `SIGMAV2-COLLECTION.yaml`

- ✅ Actualizado ejemplo en `/generate/batch` (3 productos)
- ✅ Actualizado ejemplo en `/generate-and-print` (1 producto)

**Cómo probar:**

```bash
# 1. Importar colección en Postman/Insomnia
# 2. Ejecutar: "3. Generar Lote Masivo (generate/batch)"
# 3. Respuesta esperada: 200 OK (sin errores de validación)
```

---

## 🎯 IMPACTO

- ✅ **Frontend:** Ahora debe enviar `labelsToGenerate` en lugar de `requestedLabels`
- ✅ **Backend:** Sin cambios necesarios (DTO ya era correcto)
- ✅ **Colección Postman:** Corregida y funcional

---

**Autor:** GitHub Copilot  
**Fecha:** 2026-03-19  
**Versión:** 1.0.0

