# 📊 Tablas Consultadas por el Módulo de Labels

## 🗄️ Base de Datos

**Base de datos**: `SIGMAV2_2`
**Motor**: MySQL
**Host**: localhost:3306
**Usuario**: root

---

## 📋 Tablas Consultadas en `getLabelSummary()`

El método `getLabelSummary()` consulta las siguientes **6 tablas** de la base de datos MySQL:

### 1. 🏷️ **labels** (Tabla Principal de Marbetes)
```sql
@Entity
@Table(name = "labels")
```

**Descripción**: Almacena todos los marbetes (labels) generados en el sistema.

**Columnas principales**:
- `folio` (PK) - Número de folio único del marbete
- `id_label_request` - Referencia a la solicitud de marbete
- `id_period` - ID del periodo al que pertenece
- `id_warehouse` - ID del almacén
- `id_product` - ID del producto
- `estado` - Estado del marbete (GENERADO, IMPRESO, CANCELADO)
- `impreso_at` - Fecha de impresión
- `created_by` - Usuario que lo creó
- `created_at` - Fecha de creación

**Consulta**:
```java
persistence.findByPeriodIdAndWarehouseId(periodId, warehouseId, 0, 100000)
```

---

### 2. 📝 **label_requests** (Solicitudes de Marbetes)
```sql
@Entity
@Table(name = "label_requests")
```

**Descripción**: Almacena las solicitudes de marbetes por producto, almacén y periodo.

**Columnas principales**:
- `id_label_request` (PK) - ID de la solicitud
- `id_product` - ID del producto solicitado
- `id_warehouse` - ID del almacén
- `id_period` - ID del periodo
- `requested_labels` - Cantidad de marbetes solicitados
- `folios_generados` - Cantidad de folios generados
- `created_by` - Usuario que solicitó
- `created_at` - Fecha de solicitud

**Restricción única**: (id_product, id_warehouse, id_period)

**Consulta**:
```java
labelRequestRepository.findAll()
// Filtrado en memoria por periodId y warehouseId
```

---

### 3. 📦 **products** (Productos)
```sql
@Entity
@Table(name = "products")
```

**Descripción**: Catálogo de productos del inventario.

**Columnas principales**:
- `id_product` (PK) - ID del producto
- `cve_art` - **Clave del producto** (usado en búsqueda y ordenamiento)
- `descr` - **Descripción/Nombre del producto** (usado en búsqueda y ordenamiento)
- `uni_med` - Unidad de medida
- `status` - Estado del producto
- `lin_prod` - Línea de producto
- `created_at` - Fecha de creación

**Consulta**:
```java
productRepository.findById(productId)
```

---

### 4. 🏢 **warehouse** (Almacenes)
```sql
@Entity
@Table(name = "warehouse")
```

**Descripción**: Catálogo de almacenes de la empresa.

**Columnas principales**:
- `id_warehouse` (PK) - ID del almacén
- `warehouse_key` - **Clave del almacén** (usado en búsqueda y ordenamiento)
- `name_warehouse` - **Nombre del almacén** (usado en búsqueda y ordenamiento)
- `observations` - Observaciones

**Consultas**:
```java
// Para obtener información del almacén
warehouseRepository.findById(warehouseId)

// Para obtener almacén por defecto (primero)
warehouseRepository.findFirstByOrderByIdWarehouseAsc()
```

---

### 5. 📊 **inventory_stock** (Existencias de Inventario)
```sql
@Entity
@Table(name = "inventory_stock")
```

**Descripción**: Almacena las existencias actuales de productos por almacén.

**Columnas principales**:
- `id_stock` (PK) - ID del registro de stock
- `id_product` (FK) - ID del producto
- `id_warehouse` (FK) - ID del almacén
- `exist_qty` - **Cantidad de existencias** (usado en búsqueda y ordenamiento)
- `status` - **Estado del stock** (usado en búsqueda y ordenamiento)
- `updated_at` - Última actualización

**Consulta**:
```java
inventoryStockRepository.findByProductIdProductAndWarehouseIdWarehouse(productId, warehouseId)
```

---

### 6. 📅 **period** (Periodos de Inventario)
```sql
@Entity
@Table(name = "period")
```

**Descripción**: Periodos de inventario para organizar los marbetes.

**Columnas principales**:
- `id_period` (PK) - ID del periodo
- `period` - Fecha del periodo (UNIQUE)
- `comments` - Comentarios
- `state` - Estado del periodo

**Consulta**:
```java
// Para obtener el último periodo creado (por defecto)
jpaPeriodRepository.findLatestPeriod()
```

**Query utilizada**:
```sql
SELECT p FROM InventoryPeriodEntity p ORDER BY p.date DESC LIMIT 1
```

---

## 🔍 Flujo de Consultas en `getLabelSummary()`

### Paso 1: Obtener Periodo y Almacén (si no se especifican)
```sql
-- Si periodId es null
SELECT * FROM period ORDER BY period DESC LIMIT 1;

-- Si warehouseId es null
SELECT * FROM warehouse ORDER BY id_warehouse ASC LIMIT 1;
```

### Paso 2: Obtener Información del Almacén
```sql
SELECT * FROM warehouse WHERE id_warehouse = ?;
```

### Paso 3: Obtener Solicitudes de Marbetes
```sql
SELECT * FROM label_requests;
-- Filtrado en memoria:
-- WHERE id_period = ? AND id_warehouse = ?
```

### Paso 4: Obtener Marbetes Generados
```sql
SELECT * FROM labels
WHERE id_period = ? AND id_warehouse = ?
LIMIT 100000;
```

### Paso 5: Para cada Producto Único
```sql
-- Información del producto
SELECT * FROM products WHERE id_product = ?;

-- Existencias del producto en el almacén
SELECT * FROM inventory_stock
WHERE id_product = ? AND id_warehouse = ?;
```

---

## 📈 Resumen de Operaciones por Tabla

| Tabla | Operación | Frecuencia | Propósito |
|-------|-----------|------------|-----------|
| **period** | SELECT (1) | 1 vez (si periodId null) | Obtener periodo por defecto |
| **warehouse** | SELECT (2) | 1-2 veces | Almacén por defecto + info |
| **label_requests** | SELECT ALL | 1 vez | Solicitudes de marbetes |
| **labels** | SELECT | 1 vez | Marbetes generados |
| **products** | SELECT | N veces | Info por cada producto (N = productos únicos) |
| **inventory_stock** | SELECT | N veces | Existencias por producto (N = productos únicos) |

**N** = Número de productos únicos con solicitudes o marbetes

---

## 🎯 Columnas Usadas en Búsqueda

El filtro de búsqueda (`searchText`) busca en las siguientes columnas:

1. **products.cve_art** - Clave de producto
2. **products.descr** - Nombre del producto
3. **warehouse.warehouse_key** - Clave de almacén
4. **warehouse.name_warehouse** - Nombre del almacén
5. **inventory_stock.status** - Estado del producto
6. **inventory_stock.exist_qty** - Existencias

---

## 🔢 Columnas Usadas en Ordenamiento

El ordenamiento (`sortBy`) puede usar:

1. **labels.folio** (COUNT) - `foliosExistentes`
2. **products.cve_art** - `claveProducto` ⭐ (default)
3. **products.descr** - `producto` / `nombreProducto`
4. **warehouse.warehouse_key** - `claveAlmacen`
5. **warehouse.name_warehouse** - `almacen` / `nombreAlmacen`
6. **inventory_stock.status** - `estado`
7. **inventory_stock.exist_qty** - `existencias`

---

## ⚡ Optimizaciones Recomendadas

### Índices Sugeridos

```sql
-- Para búsquedas frecuentes por periodo y almacén
CREATE INDEX idx_labels_period_warehouse
ON labels(id_period, id_warehouse);

CREATE INDEX idx_label_requests_period_warehouse
ON label_requests(id_period, id_warehouse);

-- Para búsquedas de existencias
CREATE INDEX idx_inventory_stock_product_warehouse
ON inventory_stock(id_product, id_warehouse);

-- Para búsquedas de texto en productos
CREATE INDEX idx_products_cve_art ON products(cve_art);
CREATE INDEX idx_products_descr ON products(descr);

-- Para almacenes
CREATE INDEX idx_warehouse_key ON warehouse(warehouse_key);
```

### Consideraciones de Rendimiento

- ✅ **Paginación**: Se aplica en memoria después del filtrado
- ⚠️ **Búsqueda**: Se realiza en memoria (case-insensitive)
- ⚠️ **Ordenamiento**: Se realiza en memoria
- 💡 **Mejora futura**: Implementar búsqueda y ordenamiento en SQL para grandes volúmenes

---

## 📌 Notas Importantes

1. **Búsqueda case-insensitive**: Se realiza en Java convirtiendo a lowercase
2. **Filtrado en memoria**: `label_requests` se filtra en memoria después de `findAll()`
3. **N+1 queries**: Se hace una query por cada producto para obtener sus existencias
4. **Límite de marbetes**: Se cargan máximo 100,000 marbetes por consulta
5. **Paginación**: Se aplica después de cargar, filtrar y ordenar todos los datos

---

## 🔗 Relaciones entre Tablas

```
period (1) ─────┐
                ├─→ labels (N)
warehouse (1) ──┤
                └─→ label_requests (N)
                    │
                    └─→ products (1)

products (1) ────┐
                 ├─→ inventory_stock (N)
warehouse (1) ───┘
```

---

**Fecha**: 2025-11-28
**Base de datos**: SIGMAV2_2 (MySQL)
**Método**: `LabelServiceImpl.getLabelSummary()`

