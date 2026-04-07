# Validación Única de Clave de Almacén (warehouse_key)

## Problema Reportado
Al intentar crear o actualizar un almacén, la API no validaba correctamente cuando la clave (`warehouse_key`) ya existía. El DTO de actualización tampoco permitía cambiar la clave del almacén.

## Solución Implementada

### 1. **WarehouseUpdateDTO - Agregar Campo warehouse_key**
**Archivo:** `modules/warehouse/adapter/web/dto/WarehouseUpdateDTO.java`

Se agregó el campo `warehouseKey` (opcional) para permitir cambios en la clave durante actualización:
```java
@Size(min = 1, max = 20, message = "La clave del almacén debe tener entre 1 y 20 caracteres")
private String warehouseKey;
```

**Nota:** Campo opcional (sin @NotBlank) para mantener compatibilidad con clientes que solo actualizan nombre/observaciones.

### 2. **WarehouseServiceImpl - Validación en Update**
**Archivo:** `modules/warehouse/application/service/WarehouseServiceImpl.java`

Lógica mejorada en `updateWarehouse()`:
- Si `warehouseKey` viene en el DTO y es diferente de la clave actual → valida unicidad
- Usa `existsByWarehouseKeyAndIdNotAndDeletedAtIsNull()` para excluir el almacén actual
- Si ya existe otra clave con ese valor → lanza `IllegalArgumentException`

**Pseudocódigo:**
```
if (warehouseKey != null AND warehouseKey != currentKey):
    if existe otro almacén activo con esa clave (excluyendo ID actual):
        throw IllegalArgumentException("La clave de almacén ya existe")
```

### 3. **Corrección de Validadores en DTOs**

#### WarehouseCreateDTO
```java
@NotBlank(message = "La clave del almacén es requerida")
@Size(min = 1, max = 20, message = "La clave del almacén debe tener entre 1 y 20 caracteres")
private String warehouseKey;
```

#### WarehouseUpdateDTO
```java
@Size(min = 1, max = 20, message = "La clave del almacén debe tener entre 1 y 20 caracteres")
private String warehouseKey;
```

**Cambios:**
- Min de 3 a **1** caracteres (permite claves de un solo dígito: "1", "2", etc.)
- Mensaje de error correcto (antes decía "El nombre debe tener..." para la clave)

## Flujo de Validación Completo

### Crear Almacén (POST /api/sigmav2/warehouses)
```
1. Validación DTO → warehouseKey: 1-20 caracteres, requerido
2. Servicio normaliza clave → toUpperCase() + trim()
3. Verifica: ¿existe clave activa? → NO → crear
4. Verifica: ¿existe clave activa? → SÍ → error 400
```

### Actualizar Almacén (PUT /api/sigmav2/warehouses/{id})
```
1. Validación DTO → warehouseKey: 1-20 caracteres, OPCIONAL
2. Si warehouseKey viene en DTO:
   a) Normaliza: toUpperCase() + trim()
   b) ¿cambió vs actual? 
      → NO: ignora validación
      → SÍ: verifica unicidad (excluye ID actual)
         - Existe: error 400
         - No existe: actualiza
3. Si warehouseKey NO viene: no se modifica
```

## Respuestas API

### Crear con clave duplicada
```json
{
  "success": false,
  "message": "La clave de almacén ya existe: 1",
  "statusCode": 400
}
```

### Actualizar con clave duplicada
```json
{
  "success": false,
  "message": "La clave de almacén ya existe: 55",
  "statusCode": 400
}
```

### Validación de DTO (clave fuera de rango)
```json
{
  "success": false,
  "fieldErrors": {
    "warehouseKey": "La clave del almacén debe tener entre 1 y 20 caracteres"
  },
  "error": "VALIDATION_ERROR"
}
```

## Casos de Uso Cubiertos

| Caso | Método | DTO | Resultado |
|------|--------|-----|-----------|
| Crear almacén clave "1" | POST | `{warehouseKey:"1", nameWarehouse:"Almacén 1"}` | ✅ OK |
| Crear almacén clave "1" (ya existe) | POST | `{warehouseKey:"1", nameWarehouse:"Otro"}` | ❌ Error 400 |
| Actualizar nombre (no tocar clave) | PUT | `{nameWarehouse:"Nuevo nombre"}` | ✅ OK |
| Cambiar clave a una disponible | PUT | `{warehouseKey:"2", nameWarehouse:"..."}` | ✅ OK |
| Cambiar clave a existente | PUT | `{warehouseKey:"3"}` | ❌ Error 400 |
| Clave vacía al crear | POST | `{warehouseKey:"", nameWarehouse:"..."}` | ❌ Validación DTO |

## Validación en Base de Datos

La tabla `warehouse` mantiene restricciones únicas en BD:
```sql
UNIQUE KEY `uk_warehouse_key` (`warehouse_key`)
```

Los registros eliminados (soft-delete) se archivan renombrando la clave con timestamp y ID:
```
Original: "1"
Eliminado: "1_DEL_20260407102104_8"
```

Esto permite reutilizar claves después de eliminar almacenes antiguos.

## Testing Recomendado

1. **Crear almacén** con clave "1"
2. **Intentar crear** otro con clave "1" → debe fallar (400)
3. **Actualizar** el primero (cambiar nombre) sin incluir warehouseKey → debe funcionar
4. **Actualizar** para cambiar clave a "2" → debe funcionar
5. **Intentar actualizar** otro almacén con clave "2" → debe fallar (400)
6. **Eliminar** almacén y **crear** uno nuevo con clave "1" → debe funcionar

---
**Fecha:** 2026-04-07  
**Modificado en:** `WarehouseServiceImpl`, `WarehouseCreateDTO`, `WarehouseUpdateDTO`

