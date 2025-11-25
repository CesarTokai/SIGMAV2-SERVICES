# Corrección del Sistema de MultiAlmacén según Reglas de Negocio

## Fecha: 2025-01-25

## Resumen de Cambios

Se han realizado correcciones importantes en el módulo de MultiAlmacén para asegurar que el sistema cumpla con las reglas de negocio documentadas y las especificaciones del catálogo.

---

## Problema Identificado

El sistema anterior tenía inconsistencias entre:
1. La documentación de reglas de negocio
2. El formato del archivo Excel de importación
3. La estructura de la base de datos
4. La lógica de búsqueda y consultas

### Discrepancias encontradas:

**Del Excel multialmacen.xlsx:**
- `CVE_ALM` → Representa la **clave del almacén** (warehouse_key)
- `CVE_ART` → Representa la **clave del producto** (productCode)
- `DESCR` → Descripción del producto (viene del catálogo de inventario)
- `STATUS` → Estado del producto (A=Alta, B=Baja)
- `EXIST` → Existencias del producto

**Consulta esperada debe mostrar:**
- Clave producto (CVE_ART)
- Producto/Descripción (DESCR del inventario)
- Clave Almacén (warehouse_key de la tabla warehouse)
- Almacén (name_warehouse de la tabla warehouse)
- Estado (STATUS)
- Existencias (EXIST)

---

## Cambios Realizados

### 1. Modelo de Datos (`MultiWarehouseExistence.java`)

**Agregado:**
```java
private String warehouseKey; // CVE_ALM - Clave del almacén
```

**Antes:** Solo existía `warehouseName`
**Ahora:** Se almacenan tanto `warehouseKey` como `warehouseName` para consistencia

**Documentación de campos mejorada:**
- `productCode` → CVE_ART - Clave del producto
- `productName` → DESCR - Descripción del producto
- `warehouseKey` → CVE_ALM - Clave del almacén
- `warehouseName` → Nombre del almacén

---

### 2. Servicio de Importación (`MultiWarehouseServiceImpl.java`)

#### Método `createMissingWarehouses()`
**ANTES:** Buscaba almacenes por nombre (`warehouseName`)
**AHORA:** Busca almacenes por clave (`warehouseKey`) usando CVE_ALM del Excel

```java
// Buscar almacén existente por clave (CVE_ALM)
Optional<WarehouseEntity> existing =
    warehouseRepository.findByWarehouseKeyAndDeletedAtIsNull(warehouseKey);
```

**Regla de Negocio aplicada:**
- Si CVE_ALM del Excel no existe → Se crea el almacén automáticamente
- Se agrega observación: "Este almacén no existía y fue creado en la importación"

#### Método `createMissingProducts()`
**ANTES:** No actualizaba la descripción del producto desde el inventario
**AHORA:** Obtiene DESCR desde el catálogo de inventario (tabla products)

```java
if (existing.isPresent()) {
    productMap.put(productCode, existing.get().getIdProduct());
    // Actualizar la descripción del producto desde el inventario
    data.setProductName(existing.get().getDescr());
}
```

**Regla de Negocio aplicada:**
- La descripción del producto (DESCR) siempre viene del catálogo de inventario
- Si el producto no existe → Se crea con estado "A" (Alta)

#### Lógica de Importación
**ANTES:** Identificaba registros por `productCode + warehouseName`
**AHORA:** Identifica registros por `productCode + warehouseKey`

```java
// Clave de identificación corregida
String key = newData.getProductCode() + "|" + newData.getWarehouseKey();
```

**Reglas de Negocio aplicadas:**
1. ✅ Crear almacenes que no existen (usando CVE_ALM)
2. ✅ Crear productos que no existen con estado "A"
3. ✅ Importar productos nuevos al catálogo
4. ✅ Actualizar productos existentes
5. ✅ Marcar como "B" (Baja) productos no presentes en el Excel

#### Parsers (CSV y XLSX)
**ANTES:** Leían columna "almacen" o "almacén" como nombre
**AHORA:** Leen columna "CVE_ALM" como clave del almacén

```java
int iAlmacenKey = indexOf(headers,
    new String[]{"cve_alm","CVE_ALM","almacen_clave","warehouse_key"});
```

---

### 3. Repositorio (`MultiWarehouseRepository.java`)

#### Consulta de Búsqueda
**Agregado:** Búsqueda por `warehouseKey`

```java
"LOWER(e.warehouseKey) LIKE LOWER(CONCAT('%', :#{#search.search}, '%')) OR "
```

#### Método de Búsqueda Específica
**ANTES:** `findByProductCodeAndWarehouseNameAndPeriodId()`
**AHORA:** `findByProductCodeAndWarehouseKeyAndPeriodId()`

```java
@Query("SELECT e FROM MultiWarehouseExistence e WHERE " +
       "e.productCode = :productCode AND e.warehouseKey = :warehouseKey AND e.periodId = :periodId")
Optional<MultiWarehouseExistence> findByProductCodeAndWarehouseKeyAndPeriodId(
    @Param("productCode") String productCode,
    @Param("warehouseKey") String warehouseKey,
    @Param("periodId") Long periodId);
```

---

### 4. Exportación

**Encabezados del CSV exportado actualizados:**

**ANTES:**
```
Almacen,Producto,Descripcion,Existencias,Estado
```

**AHORA:**
```
Clave Producto,Producto,Clave Almacen,Almacen,Estado,Existencias
```

**Orden de columnas correcto según documentación:**
1. Clave Producto (CVE_ART)
2. Producto/Descripción (DESCR)
3. Clave Almacén (warehouse_key)
4. Almacén (name_warehouse)
5. Estado (STATUS)
6. Existencias (EXIST)

---

### 5. Base de Datos

#### Migración: `V1_1_0__Create_multiwarehouse_existences_table.sql`

**Tabla creada:** `multiwarehouse_existences`

**Columnas principales:**
- `id` → BIGINT PRIMARY KEY
- `period_id` → BIGINT NOT NULL (FK a periods)
- `warehouse_id` → BIGINT NOT NULL (FK a warehouse)
- `warehouse_key` → VARCHAR(50) **(NUEVA)**
- `warehouse_name` → VARCHAR(255)
- `product_code` → VARCHAR(50) NOT NULL
- `product_name` → VARCHAR(255)
- `stock` → DECIMAL(15,2)
- `status` → VARCHAR(1) (A=Alta, B=Baja)

**Índices creados:**
- `idx_multiwarehouse_period` → Búsqueda por periodo
- `idx_multiwarehouse_warehouse` → Búsqueda por almacén ID
- `idx_multiwarehouse_product` → Búsqueda por código de producto
- `idx_multiwarehouse_warehouse_key` → **Búsqueda por clave de almacén (NUEVO)**
- `idx_warehouse_product` → Búsqueda compuesta (warehouse_id, product_code)
- `idx_multiwarehouse_product_warehouse` → **Búsqueda compuesta (product_code, warehouse_key, period_id) (NUEVO)**
- `idx_multiwarehouse_period_status` → Búsqueda por periodo y estado

---

## Validación de Reglas de Negocio

### ✅ Regla 1: Crear almacenes que no existen
Si en el Excel aparece CVE_ALM que no existe → Se crea automáticamente con observación

### ✅ Regla 2: Crear productos que no existen
Si en el Excel aparece CVE_ART que no existe en inventario → Se crea con estado "A"

### ✅ Regla 3: Importar productos nuevos
Si producto existe en inventario pero no en multialmacén → Se importa

### ✅ Regla 4: Actualizar productos existentes
Si producto existe en inventario Y en multialmacén → Se actualizan sus valores

### ✅ Regla 5: Marcar productos como baja
Si producto existe en multialmacén pero NO en el Excel → Status cambia a "B"

---

## Impacto en el Frontend

**Consulta de MultiAlmacén mostrará:**
1. ✅ Clave producto (CVE_ART)
2. ✅ Producto (DESCR del inventario)
3. ✅ Clave Almacén (warehouse_key)
4. ✅ Almacén (name_warehouse)
5. ✅ Estado (A/B)
6. ✅ Existencias (stock)

**Búsqueda funcionará con:**
- Clave de producto
- Descripción de producto
- Clave de almacén **(NUEVO)**
- Nombre de almacén
- Existencias

---

## Formato del Excel de Importación

**Columnas requeridas en multialmacen.xlsx:**

| Columna | Nombre en Excel | Tipo | Obligatorio | Descripción |
|---------|----------------|------|-------------|-------------|
| CVE_ALM | CVE_ALM | String | ✅ Sí | Clave del almacén |
| CVE_ART | CVE_ART | String | ✅ Sí | Clave del producto |
| DESCR | DESCR | String | ⚠️ Opcional | Descripción (se sobrescribe con inventario) |
| STATUS | STATUS | String(1) | ✅ Sí | Estado: A=Alta, B=Baja |
| EXIST | EXIST | Decimal | ✅ Sí | Existencias del producto |

**Nota:** La columna DESCR es opcional en el Excel porque el sistema siempre obtiene la descripción desde el catálogo de inventario (tabla products).

---

## Testing Recomendado

### Caso 1: Importar con almacén nuevo
1. Excel con CVE_ALM = "ALM_NUEVO"
2. Verificar que se crea el almacén con observación
3. Verificar que warehouse_key = "ALM_NUEVO"

### Caso 2: Importar con producto nuevo
1. Excel con CVE_ART = "PROD_NUEVO"
2. Verificar que se crea en tabla products con status "A"
3. Verificar que product_name viene de DESCR del Excel o CVE_ART

### Caso 3: Actualizar producto existente
1. Excel con CVE_ART existente, nuevas existencias
2. Verificar que se actualiza stock
3. Verificar que product_name viene del inventario (no del Excel)

### Caso 4: Marcar productos como baja
1. Producto existe en BD pero NO en Excel
2. Verificar que status cambia a "B"

### Caso 5: Búsqueda por clave de almacén
1. Consultar con search = "ALM_01"
2. Verificar que encuentra productos de ese almacén

### Caso 6: Exportación
1. Exportar CSV
2. Verificar columnas: Clave Producto, Producto, Clave Almacen, Almacen, Estado, Existencias
3. Verificar que datos son correctos

---

## Próximos Pasos

1. ✅ Ejecutar migraciones de base de datos
2. ✅ Compilar y probar la aplicación
3. ⚠️ Actualizar el archivo multialmacen.xlsx de ejemplo con CVE_ALM
4. ⚠️ Actualizar el frontend si es necesario para mostrar warehouse_key
5. ⚠️ Realizar pruebas end-to-end de importación
6. ⚠️ Documentar el nuevo formato para usuarios finales

---

## Notas Técnicas

- Método `generateWarehouseKey()` eliminado (ya no se necesita)
- Todas las búsquedas usan `warehouseKey` como identificador primario
- La relación con `warehouse` table se mantiene por `warehouse_id` (FK)
- `warehouse_key` y `warehouse_name` se desnormalizan para performance
- Los índices compuestos mejoran significativamente las consultas

---

## Compatibilidad

**⚠️ IMPORTANTE:** Esta actualización requiere:

1. Migración de base de datos V1_1_0
2. Datos existentes en `multiwarehouse_existences` deben tener `warehouse_key` poblado
3. Archivos Excel de importación deben incluir columna CVE_ALM

**Retrocompatibilidad:**
- El parser sigue aceptando nombres alternativos de columnas
- Si warehouse_key es null, el sistema usa warehouse_id para recuperarlo

---

## Autores
- Corrección realizada por: GitHub Copilot
- Fecha: 2025-01-25
- Basado en: Documentación de reglas de negocio del catálogo de MultiAlmacén

