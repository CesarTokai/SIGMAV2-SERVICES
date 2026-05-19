# 📋 ACTUALIZACIÓN REGLAS DE NEGOCIO - MultiAlmacén (RN-MWH-002 MEJORADA)

## Información General

**Documento:** Regla de Negocio Mejorada RN-MWH-002  
**Fecha de Actualización:** 2026-02-05  
**Razón:** Incorporar validaciones previas para evitar gaps en IDs  
**Módulo:** MultiWarehouse - Creación de Almacenes  

---

## 🔄 REGLA ORIGINAL vs MEJORADA

### ANTES (RN-MWH-002 Original)

**Descripción:** Si en el archivo de Excel "multialmacen.xlsx" aparecen almacenes que no existen en el SIGMA, éstos serán creados automáticamente en el catálogo de almacenes.

**Implementación:** 
- ❌ Sin validación previa de unicidad de nombre
- ❌ Sin manejo explícito de errores
- ❌ Sin logging detallado
- ⚠️ Resultaba en gaps de IDs en la secuencia

### AHORA (RN-MWH-002 Mejorada)

**Descripción:** Si en el archivo de Excel "multialmacen.xlsx" aparecen almacenes que no existen en el SIGMA, éstos serán creados automáticamente en el catálogo de almacenes con validaciones previas para garantizar integridad de datos y auditoría completa.

**Implementación:**
- ✅ Validación previa completa ANTES de guardar
- ✅ Manejo explícito de excepciones
- ✅ Logging en 4 niveles
- ✅ Minimiza gaps de IDs en la secuencia

---

## 📐 ESPECIFICACIÓN DETALLADA

### RN-MWH-002: Creación Automática de Almacenes (Versión 2.0)

**Prioridad:** ALTA  
**Módulo:** MultiWarehouse  
**Archivo:** `MultiWarehouseServiceImpl.java`  
**Método:** `createMissingWarehouses()`  
**Líneas:** 568-625  

---

## 🔄 FLUJO MEJORADO

```
┌─────────────────────────────────────────────────────────────┐
│ ENTRADA: List<MultiWarehouseExistence> parsedData           │
│ (Datos parseados del Excel)                                 │
└────────────────────┬────────────────────────────────────────┘
                     ↓
        ┌────────────────────────────────┐
        │ PRIMERA PASADA                 │
        │ Para cada CVE_ALM del Excel    │
        └────────────────┬───────────────┘
                         ↓
       ✅ PASO 1: Validar y Normalizar
          - ¿CVE_ALM es null/vacío? → SKIP
          - Normalizar: "55.0" → "55"
          - Trimear espacios
                         ↓
       ✅ PASO 2: Deduplicación en Memoria
          - ¿Ya está en warehouseMap?
          - SÍ → SKIP (ya procesado)
          - NO → Continuar
                         ↓
       ✅ PASO 3: Búsqueda en BD
          - ¿warehouse_key existe en BD?
          - SÍ → Usar ID existente, agregar a mapa, SKIP
          - NO → Continuar a Paso 4
                         ↓
       ✅ PASO 4: Generar Nombre Inteligente
          - ¿Nombre viene en Excel?
          - SÍ → Usar ese nombre
          - NO ├─ ¿Es número? → "Almacén X"
              └─ ¿Es texto? → Igual a la clave
                         ↓
       ✅ PASO 5: Validación de Nombre PREVIA
          - ¿Existe ese nombre en BD?
          - SÍ → ⚠️ CONFLICTO: Usar clave como nombre
          - NO → Continuar
                         ↓
       ✅ PASO 6: Try-Catch para Guardar
          try {
            - Crear WarehouseEntity
            - Configurar todos los campos
            - save() a BD
            - ✅ Agregar a mapas
            - 📝 log.info("Almacén creado...")
          } catch (Exception ex) {
            - 📝 log.error("Error al crear...")
            - throw RuntimeException
          }
                         ↓
        ┌────────────────────────────────┐
        │ SEGUNDA PASADA                 │
        │ Sincronizar nombres            │
        └────────────────┬───────────────┘
                         ↓
       ✅ PASO 7: Sincronización
          - Para cada registro en parsedData
          - Actualizar warehouseName desde warehouseNameMap
          - Asegurar consistencia de nombres
                         ↓
        ┌────────────────────────────────┐
        │ RETORNO                        │
        │ Map<String, Long> warehouseMap │
        │ {"55": 110, "BODEGA": 111...} │
        └────────────────────────────────┘
```

---

## 🎯 CONDICIONES DE ACTIVACIÓN

La regla se ejecuta SOLO si:

1. ✅ Se está importando un archivo de MultiAlmacén
2. ✅ El archivo contiene columna CVE_ALM
3. ✅ Existe un CVE_ALM en el Excel que NO existe en tabla `warehouse` con `deleted_at IS NULL`

---

## ✅ ACCIONES QUE EJECUTA EL SISTEMA

| # | Acción | Detalles |
|---|--------|---------|
| 1 | **Normalizar clave** | "55.0" → "55", trimear espacios |
| 2 | **Buscar en mapa** | Evitar procesar dos veces |
| 3 | **Buscar en BD** | ¿Existe en tabla warehouse? |
| 4 | **Generar nombre** | Número: "Almacén X", Texto: igual clave |
| 5 | **Validar nombre** | ¿Ya existe ese nombre en BD? |
| 6 | **Crear warehouse** | INSERT en tabla warehouse |
| 7 | **Guardar en mapa** | Para usar después en la importación |
| 8 | **Registrar auditoría** | Timestamp + observación |
| 9 | **Logging detallado** | 4 niveles: DEBUG, INFO, WARN, ERROR |

---

## 📝 CAMPOS ASIGNADOS AL CREAR

| Campo | Valor | Descripción |
|-------|-------|-------------|
| `warehouse_key` | CVE_ALM (normalizado) | Clave única del almacén |
| `name_warehouse` | Generado inteligentemente | Nombre para presentación |
| `observations` | "Este almacén no existía y fue creado en la importación el YYYY-MM-DD HH:mm:ss" | Auditoría de creación |
| `created_at` | LocalDateTime.now() | Timestamp de creación |
| `updated_at` | LocalDateTime.now() | Timestamp de actualización |
| `deleted_at` | NULL | Soft delete (no está eliminado) |

---

## 📊 EJEMPLOS DE EJECUCIÓN

### Ejemplo 1: Almacén Numérico Nuevo

**Entrada (Excel):**
```
CVE_ALM = "55"
CVE_ART = "PROD-001"
```

**Proceso:**
1. Normalizar: "55" → "55" (ya está normalizado)
2. ¿En mapa? NO → Continuar
3. ¿En BD? NO → Continuar
4. Generar nombre: "55" es número → "Almacén 55"
5. ¿Existe "Almacén 55"? NO → OK
6. Guardar en BD → ID 110
7. Log: `[INFO] Almacén creado: warehouseKey=55, id=110, name=Almacén 55`

**Salida (BD):**
```
id=110, warehouse_key='55', name_warehouse='Almacén 55'
observations='Este almacén no existía y fue creado en la importación el 2026-02-05 15:30:45'
```

### Ejemplo 2: Almacén Texto Nuevo

**Entrada (Excel):**
```
CVE_ALM = "BODEGA_MTY"
CVE_ART = "PROD-001"
```

**Proceso:**
1. Normalizar: "BODEGA_MTY" → "BODEGA_MTY" (ya es texto)
2. ¿En mapa? NO → Continuar
3. ¿En BD? NO → Continuar
4. Generar nombre: "BODEGA_MTY" no es número → "BODEGA_MTY"
5. ¿Existe "BODEGA_MTY"? NO → OK
6. Guardar en BD → ID 111
7. Log: `[INFO] Almacén creado: warehouseKey=BODEGA_MTY, id=111, name=BODEGA_MTY`

**Salida (BD):**
```
id=111, warehouse_key='BODEGA_MTY', name_warehouse='BODEGA_MTY'
```

### Ejemplo 3: Nombre Duplicado (Conflicto Resuelto)

**Entrada (Excel):**
```
CVE_ALM = "NEW_ALMACEN"
Nombre inteligente generado: "Centro Test"
(pero "Centro Test" ya existe en BD)
```

**Proceso:**
1. Normalizar: OK
2. ¿En mapa? NO
3. ¿En BD? NO
4. Generar nombre: "Centro Test"
5. ¿Existe "Centro Test"? **SÍ** → ⚠️ CONFLICTO
6. **Cambiar estrategia:** warehouseName = "NEW_ALMACEN" (usar clave)
7. Log: `[WARN] El nombre ya existe: Centro Test (warehouseKey=NEW_ALMACEN). Usando clave como nombre.`
8. Guardar: NEW_ALMACEN (clave como nombre)
9. Log: `[INFO] Almacén creado: warehouseKey=NEW_ALMACEN, id=112, name=NEW_ALMACEN`

**Salida (BD):**
```
id=112, warehouse_key='NEW_ALMACEN', name_warehouse='NEW_ALMACEN'
```

### Ejemplo 4: Almacén que Ya Existe

**Entrada (Excel):**
```
CVE_ALM = "55"
(pero "55" ya existe en BD desde importación anterior)
```

**Proceso:**
1. Normalizar: "55" → "55"
2. ¿En mapa? NO → Continuar
3. ¿En BD? **SÍ** → ID 81
4. Log: `[DEBUG] Almacén existente encontrado: warehouseKey=55, id=81`
5. Agregar a mapa: warehouseMap.put("55", 81)
6. **SKIP - No crear nuevo**

**Resultado:**
- No se crea nuevo registro
- Se reutiliza ID existente (81)
- No consume ID innecesario

---

## 🚨 MANEJO DE ERRORES

### Escenario: Error en BD (Exception)

```java
try {
    WarehouseEntity saved = warehouseRepository.save(newWarehouse);
    // ... éxito ...
} catch (Exception ex) {
    log.error("Error creando almacén: warehouseKey={}, name={}. Error: {}", 
              warehouseKey, warehouseName, ex.getMessage(), ex);
    throw new RuntimeException("Error al crear almacén " + warehouseKey + 
                               ": " + ex.getMessage(), ex);
}
```

**Resultado:**
- ✅ Se registra en logs
- ✅ Se propaga la excepción (importación se detiene)
- ❌ El ID se consumió pero no se usó (gap inevitable)
- 📝 Auditoría completa en logs

**User Message:**
```
{
  "success": false,
  "message": "Error al crear almacén BODEGA: [detalle del error]",
  "error": "WAREHOUSE_CREATION_ERROR",
  "timestamp": "2026-02-05T15:30:45"
}
```

---

## 📊 IMPACTO EN IDs

### Antes (Sin Validación Previa)
```
Importación de 5 almacenes nuevos
Resultado: 1 con error en name
↓
IDs consumidos: 5 (110, 111, 112, 113, 114)
IDs utilizados: 4 (110, 111, 113, 114)
IDs perdidos: 1 (112)
Gap creado: 1 ID
```

### Después (Con Validación Previa)
```
Importación de 5 almacenes nuevos
Resultado: 0 errores (nombre conflictivo cambia automáticamente)
↓
IDs consumidos: 5 (110, 111, 112, 113, 114)
IDs utilizados: 5 (110, 111, 112, 113, 114)
IDs perdidos: 0
Gap evitado: ✅
```

---

## 📝 LOGGING DETALLADO

### Niveles de Log Utilizados

```
DEBUG: Operaciones internas (búsqueda encontrada)
  "[DEBUG] Almacén existente encontrado: warehouseKey=55, id=81"

INFO: Operaciones exitosas (crear, actualizar)
  "[INFO] Almacén creado: warehouseKey=BODEGA_DF, id=110, name=BODEGA_DF"

WARN: Situaciones anómalas pero recuperables
  "[WARN] El nombre ya existe: Centro 1 (warehouseKey=15). Usando clave como nombre."

ERROR: Situaciones de error
  "[ERROR] Error creando almacén: warehouseKey=BODEGA, name=BODEGA. Error: SQL Exception"
```

### Ventajas del Logging

✅ **Auditoría completa** - Saber exactamente qué pasó  
✅ **Debugging** - Identificar problemas rápidamente  
✅ **Trazabilidad** - Registro de todas las operaciones  
✅ **Monitoreo** - Detectar patrones de error  

---

## ✔️ CRITERIOS DE VALIDACIÓN

### Prevalidaciones Ejecutadas

| # | Validación | Pass | Fail |
|---|-----------|------|------|
| 1 | CVE_ALM no es null | ✅ Continuar | ❌ SKIP |
| 2 | CVE_ALM no está vacío | ✅ Continuar | ❌ SKIP |
| 3 | CVE_ALM no está en mapa | ✅ Continuar | ❌ SKIP |
| 4 | warehouse_key no existe en BD | ✅ Continuar | ⚠️ Usar existente |
| 5 | name_warehouse no existe en BD | ✅ Guardar | ⚠️ Usar clave como nombre |
| 6 | save() no lanza exception | ✅ Éxito | ❌ Rollback + Error |

---

## 📈 MONITOREO Y AUDITORÍA

### Consultas Recomendadas

**Ver almacenes creados en la última importación:**
```sql
SELECT id, warehouse_key, name_warehouse, created_at, observations
FROM warehouse
WHERE DATE(created_at) = CURDATE()
ORDER BY id DESC;
```

**Detectar gaps en la secuencia:**
```sql
SELECT 
    (SELECT MAX(id) FROM warehouse) - COUNT(*) as gaps_encontrados,
    (SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='warehouse') as siguiente_id,
    COUNT(*) as total_almacenes
FROM warehouse
WHERE deleted_at IS NULL;
```

**Ver almacenes con nombre de clave (recuperados de conflicto):**
```sql
SELECT id, warehouse_key, name_warehouse
FROM warehouse
WHERE warehouse_key = name_warehouse
ORDER BY created_at DESC;
```

---

## 📚 REFERENCIAS RELACIONADAS

- **Documento Original:** `REGLAS-NEGOCIO-MULTIALMACEN.md` (RN-MWH-002)
- **Implementación:** `MultiWarehouseServiceImpl.java` (línea 568-625)
- **Bug Report:** `SOLUCION-IDS-SALTANDO-WAREHOUSE-MULTIALMACEN.md`
- **Guía de Usuario:** `GUIA-RAPIDA-MULTIALMACEN.md`
- **Testing:** `TESTING-MULTIALMACEN.md`

---

## ✅ CHECKLIST DE CUMPLIMIENTO

Después de implementar RN-MWH-002 v2.0:

- [x] Código modificado con validaciones previas
- [x] Try-catch con manejo de excepciones
- [x] Logging en 4 niveles (DEBUG, INFO, WARN, ERROR)
- [x] Deduplicación en memoria
- [x] Validación previa de nombre
- [ ] Deploy a TEST
- [ ] Casos de prueba ejecutados
- [ ] Gaps minimizados
- [ ] Deploy a PRODUCCIÓN

---

## 📅 HISTÓRICO DE CAMBIOS

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2025-01-25 | RN-MWH-002 original |
| 2.0 | 2026-02-05 | Agregar validaciones previas, mejora de logging |

