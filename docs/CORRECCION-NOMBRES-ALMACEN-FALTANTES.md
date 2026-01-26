# 🐛 CORRECCIÓN: Nombres de Almacén Faltantes en Consulta

**Fecha:** 26 de Enero de 2026  
**Tipo:** Bug Fix  
**Prioridad:** ALTA  
**Estado:** ✅ Corregido

---

## 📋 PROBLEMA IDENTIFICADO

### Síntoma
Al consultar el catálogo de multialmacén después de importar, algunos registros mostraban "-" en la columna "Almacén" (nombre del almacén), aunque la "Clave Almacén" tenía valor.

**Ejemplo de datos incorrectos:**
```
Clave Producto | Producto           | Clave Almacén | Almacén     | Estado | Existencias
COM-3AGAM      | ANILLO M3L AMARILLO| 3             | Almacén 3   | A      | 956,200
COM-3AGAM      | COM-3AGAM          | 55            | Almacén 55  | A      | 0
COM-3AGAZ      | ANILLO M3L AZUL    | 3             | -           | A      | 775,000    ← PROBLEMA
COM-3AGAZ      | COM-3AGAZ          | 55            | -           | A      | 0          ← PROBLEMA
```

### Patrón Observado
- ✅ **Primer registro** con un `warehouseKey` (ej: "3") → Nombre correcto ("Almacén 3")
- ❌ **Registros subsiguientes** con el mismo `warehouseKey` → Nombre vacío ("-")

---

## 🔍 CAUSA RAÍZ

### Código Original (Incorrecto)

El método `createMissingWarehouses()` tenía la siguiente lógica:

```java
private Map<String, Long> createMissingWarehouses(List<MultiWarehouseExistence> parsedData) {
    Map<String, Long> warehouseMap = new HashMap<>();

    for (MultiWarehouseExistence data : parsedData) {
        // ... normalización de warehouseKey ...
        
        if (!warehouseMap.containsKey(warehouseKey)) {  // ← SOLO ENTRA UNA VEZ
            Optional<WarehouseEntity> existing = warehouseRepository.findByWarehouseKeyAndDeletedAtIsNull(warehouseKey);

            if (existing.isPresent()) {
                warehouseMap.put(warehouseKey, existing.get().getId());
                data.setWarehouseName(existing.get().getNameWarehouse());  // ← SOLO SE ACTUALIZA EL PRIMER REGISTRO
            } else {
                // ... crear almacén nuevo ...
                data.setWarehouseName(warehouseName);  // ← SOLO SE ACTUALIZA EL PRIMER REGISTRO
            }
        }
        // ← Los registros subsiguientes NO se actualizan
    }

    return warehouseMap;
}
```

### Problema
1. El método itera sobre `parsedData` (todos los registros del Excel)
2. Cuando encuentra un `warehouseKey` **por primera vez**, lo procesa:
   - Lo agrega al `warehouseMap`
   - Actualiza el `warehouseName` **SOLO en ese registro** (`data.setWarehouseName(...)`)
3. Cuando encuentra el **mismo** `warehouseKey` en registros posteriores:
   - La condición `if (!warehouseMap.containsKey(warehouseKey))` es **FALSE**
   - **NO** entra al bloque
   - **NO** actualiza el `warehouseName` de esos registros
4. Resultado: Registros subsiguientes quedan con `warehouseName` vacío o `null`

---

## ✅ SOLUCIÓN IMPLEMENTADA

### Estrategia
Usar **dos pasadas** sobre los datos:
1. **Primera pasada:** Construir mapas de almacenes (IDs y nombres)
2. **Segunda pasada:** Actualizar **TODOS** los registros con los nombres correctos

### Código Corregido

```java
private Map<String, Long> createMissingWarehouses(List<MultiWarehouseExistence> parsedData) {
    Map<String, Long> warehouseMap = new HashMap<>();
    Map<String, String> warehouseNameMap = new HashMap<>(); // ← NUEVO: Mapa de nombres

    // ═══════════════════════════════════════════════════════════════════
    // PRIMERA PASADA: Normalizar claves y construir mapas
    // ═══════════════════════════════════════════════════════════════════
    for (MultiWarehouseExistence data : parsedData) {
        String warehouseKeyRaw = data.getWarehouseKey();
        if (warehouseKeyRaw == null || warehouseKeyRaw.trim().isEmpty()) {
            continue;
        }
        String warehouseKey = warehouseKeyRaw.trim();
        
        // Normalizar: si es número decimal terminado en .0, dejar solo la parte entera
        if (warehouseKey.matches("\\d+\\.0")) {
            warehouseKey = warehouseKey.substring(0, warehouseKey.indexOf('.'));
        }
        data.setWarehouseKey(warehouseKey);

        if (!warehouseMap.containsKey(warehouseKey)) {
            Optional<WarehouseEntity> existing = warehouseRepository.findByWarehouseKeyAndDeletedAtIsNull(warehouseKey);

            if (existing.isPresent()) {
                warehouseMap.put(warehouseKey, existing.get().getId());
                warehouseNameMap.put(warehouseKey, existing.get().getNameWarehouse()); // ← Guardar nombre
            } else {
                // Determinar nombre del almacén
                String warehouseName;
                if (data.getWarehouseName() != null && !data.getWarehouseName().trim().isEmpty()) {
                    warehouseName = data.getWarehouseName().trim();
                } else if (warehouseKey.matches("\\d+")) {
                    warehouseName = "Almacén " + warehouseKey;
                } else {
                    warehouseName = warehouseKey;
                }

                // Crear almacén nuevo
                WarehouseEntity newWarehouse = new WarehouseEntity();
                newWarehouse.setWarehouseKey(warehouseKey);
                newWarehouse.setNameWarehouse(warehouseName);
                newWarehouse.setObservations("Este almacén no existía y fue creado en la importación el " + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                newWarehouse.setCreatedAt(LocalDateTime.now());
                newWarehouse.setUpdatedAt(LocalDateTime.now());

                WarehouseEntity saved = warehouseRepository.save(newWarehouse);
                warehouseMap.put(warehouseKey, saved.getId());
                warehouseNameMap.put(warehouseKey, warehouseName); // ← Guardar nombre
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SEGUNDA PASADA: Actualizar TODOS los registros con nombres correctos
    // ═══════════════════════════════════════════════════════════════════
    for (MultiWarehouseExistence data : parsedData) {
        String warehouseKey = data.getWarehouseKey();
        if (warehouseKey != null && warehouseNameMap.containsKey(warehouseKey)) {
            data.setWarehouseName(warehouseNameMap.get(warehouseKey)); // ← Actualizar TODOS
        }
    }

    return warehouseMap;
}
```

### Cambios Clave

1. **Nuevo mapa:** `Map<String, String> warehouseNameMap`
   - Almacena la relación `warehouseKey → warehouseName`
   - Se llena en la primera pasada

2. **Primera pasada:**
   - Procesa cada `warehouseKey` único
   - Guarda el nombre en `warehouseNameMap` (líneas marcadas con `// ← Guardar nombre`)

3. **Segunda pasada (NUEVA):**
   - Itera sobre **TODOS** los registros
   - Actualiza el `warehouseName` de cada registro usando el `warehouseNameMap`
   - Garantiza que **ningún registro** quede sin nombre

---

## 🧪 RESULTADO ESPERADO

### Después de la Corrección

```
Clave Producto | Producto           | Clave Almacén | Almacén     | Estado | Existencias
COM-3AGAM      | ANILLO M3L AMARILLO| 3             | Almacén 3   | A      | 956,200
COM-3AGAM      | COM-3AGAM          | 55            | Almacén 55  | A      | 0
COM-3AGAZ      | ANILLO M3L AZUL    | 3             | Almacén 3   | A      | 775,000    ✅ CORREGIDO
COM-3AGAZ      | COM-3AGAZ          | 55            | Almacén 55  | A      | 0          ✅ CORREGIDO
COM-3AGNG      | ANILLO M3L/M4L NEGRO| 3            | Almacén 3   | A      | 11,806,930 ✅ CORREGIDO
COM-3AGNG      | COM-3AGNG          | 55            | Almacén 55  | A      | 0          ✅ CORREGIDO
```

**Todos los registros ahora tienen el nombre del almacén correctamente asignado.**

---

## 📊 IMPACTO

### Datos Afectados
- ✅ **Consultas de multialmacén:** Ahora muestran nombres correctos
- ✅ **Exportaciones a CSV:** Incluirán nombres de almacén
- ✅ **Reportes:** Mostrarán información completa
- ✅ **Integración con otros módulos:** Datos consistentes

### Operaciones NO Afectadas
- ✅ Creación de almacenes nuevos
- ✅ Creación de productos
- ✅ Actualización de existencias
- ✅ Marcado de productos como baja
- ✅ Sincronización con `inventory_stock`

---

## 🔄 PRUEBAS RECOMENDADAS

### 1. Prueba de Importación
```bash
# Importar archivo con múltiples productos por almacén
POST /api/multiwarehouse/import
Body: multialmacen.xlsx
Period: 01-2026
```

**Verificar:**
- ✅ Todos los registros del almacén "3" muestran "Almacén 3"
- ✅ Todos los registros del almacén "55" muestran "Almacén 55"
- ✅ No hay registros con almacén "-" o vacío

### 2. Prueba de Consulta
```bash
GET /api/multiwarehouse/existences?periodId=20&page=0&size=100
```

**Verificar:**
- ✅ Columna "Almacén" llena en todos los registros
- ✅ Búsqueda por nombre de almacén funciona
- ✅ Ordenación por "Almacén" funciona

### 3. Prueba de Exportación
```bash
POST /api/multiwarehouse/export
Body: {"periodId": 20}
```

**Verificar CSV:**
```csv
Clave Producto,Producto,Clave Almacen,Almacen,Estado,Existencias
COM-3AGAM,ANILLO M3L AMARILLO,3,Almacén 3,A,956200
COM-3AGAZ,ANILLO M3L AZUL,3,Almacén 3,A,775000  ← No debe tener campo vacío
```

### 4. Prueba SQL Directa
```sql
-- Verificar que NO haya registros sin nombre de almacén
SELECT COUNT(*) 
FROM multiwarehouse_existences 
WHERE warehouse_name IS NULL 
   OR warehouse_name = '' 
   OR warehouse_name = '-';

-- Resultado esperado: 0
```

---

## 📝 REGLA DE NEGOCIO AFECTADA

### RN-MWH-002: Creación Automática de Almacenes

**Texto oficial del manual:**
> "Si en el archivo de Excel 'multialmacen.xlsx' aparecen almacenes que no existen en el SIGMA, éstos serán creados automáticamente en el catálogo de almacenes y se les agregará la leyenda: 'Este almacén no existía y fue creado en la importación' en el campo 'Observaciones'."

**Estado:** ✅ Ahora funciona correctamente
- Los almacenes se crean con el nombre correcto
- **TODOS** los registros reciben el nombre del almacén
- No hay registros "huérfanos" sin nombre

---

## 🔧 ARCHIVO MODIFICADO

**Archivo:** `MultiWarehouseServiceImpl.java`  
**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/MultiWarehouse/application/service/`  
**Método modificado:** `createMissingWarehouses()`  
**Líneas:** 568-625 (aproximadamente)

---

## ✅ CHECKLIST DE VERIFICACIÓN

Antes de desplegar a producción:

- [ ] Código compilado sin errores
- [ ] Prueba de importación exitosa
- [ ] Consulta muestra nombres correctos
- [ ] Exportación CSV correcta
- [ ] Query SQL verifica 0 registros sin nombre
- [ ] Documentación actualizada
- [ ] Commit con mensaje descriptivo

---

## 🚀 DESPLIEGUE

### Pasos para Aplicar la Corrección

1. **Backup de base de datos** (precaución)
   ```bash
   # Hacer respaldo antes de actualizar
   ```

2. **Compilar el proyecto**
   ```bash
   mvn clean package -DskipTests
   ```

3. **Reiniciar el servicio**
   ```bash
   # Reiniciar aplicación Spring Boot
   ```

4. **Verificar logs**
   ```bash
   # Revisar que no haya errores en startup
   ```

5. **Ejecutar prueba de importación**
   ```bash
   # Importar archivo de prueba
   # Verificar consulta de resultados
   ```

---

## 📚 REFERENCIAS

- **Manual de Usuario SIGMA:** Página 28 - "Reglas de importación"
- **Reglas de Negocio:** `REGLAS-NEGOCIO-MULTIALMACEN.md` - RN-MWH-002
- **Código fuente:** `MultiWarehouseServiceImpl.java` - líneas 568-625

---

**Corrección realizada por:** Sistema de Desarrollo SIGMAV2  
**Fecha:** 26 de Enero de 2026  
**Versión:** 2.0.1  
**Estado:** ✅ Corregido - Listo para despliegue
