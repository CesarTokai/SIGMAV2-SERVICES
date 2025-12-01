# Actualización de inventory_stock - Integración con Módulos Labels y MultiWarehouse

## 📋 Resumen de Cambios

Se completó la implementación de la tabla `inventory_stock` para integrar correctamente los módulos de **Inventario**, **MultiWarehouse** y **Labels**, siguiendo las reglas de negocio documentadas.

---

## 🎯 Objetivo

Permitir que el módulo de **Labels** (Marbetes) consulte correctamente los productos del inventario filtrando por:
- **Almacén** (`id_warehouse`)
- **Periodo** (`id_period`)
- **Producto** (`id_product`)

---

## 🔧 Cambios Realizados

### 1. **Modelo de Dominio: `InventoryStock.java`**
**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/inventory/domain/model/InventoryStock.java`

**Cambios:**
- ✅ Agregado campo `periodId` (Long)
- ✅ Agregado campo `createdAt` (LocalDateTime)
- ✅ Constantes `STATUS_ACTIVE = "A"` y `STATUS_INACTIVE = "B"`
- ✅ Anotaciones Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`

**Campos finales:**
```java
- Long id
- Long productId
- Long warehouseId
- Long periodId          // NUEVO
- BigDecimal existQty
- String status
- LocalDateTime createdAt  // NUEVO
- LocalDateTime updatedAt
```

---

### 2. **Entidad JPA: `InventoryStockEntity.java`**
**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/inventory/infrastructure/persistence/entity/InventoryStockEntity.java`

**Cambios:**
- ✅ Agregado campo `periodId` con anotación `@Column(name = "id_period")`
- ✅ Agregado campo `createdAt` con `@Column(name = "created_at")`
- ✅ Cambiado tipo de `existQty` de `Integer` a `BigDecimal`
- ✅ Cambiado tipo de `status` de `String` a `Enum Status { A, B }`
- ✅ Agregada restricción única: `@UniqueConstraint(columnNames = {"id_product", "id_warehouse", "id_period"})`
- ✅ Métodos lifecycle `@PrePersist` y `@PreUpdate` para timestamps automáticos
- ✅ Valores por defecto: `existQty = 0.00`, `status = A`

**Mapeo completo:**
```java
@Entity
@Table(name = "inventory_stock",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"id_product", "id_warehouse", "id_period"}
       ))
public class InventoryStockEntity {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_product")
    private ProductEntity product;

    @ManyToOne
    @JoinColumn(name = "id_warehouse")
    private WarehouseEntity warehouse;

    @Column(name = "id_period")
    private Long periodId;  // NUEVO

    @Column(name = "exist_qty", precision = 10, scale = 2)
    private BigDecimal existQty;  // Cambió de Integer

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;  // Cambió de String a Enum

    @Column(name = "created_at")
    private LocalDateTime createdAt;  // NUEVO

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status { A, B }
}
```

---

### 3. **Mapper: `InventoryStockMapper.java`**
**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/inventory/infrastructure/mapper/InventoryStockMapper.java`

**Cambios:**
- ✅ Actualizado `toDomain()` para incluir `periodId` y `createdAt`
- ✅ Conversión de `Enum Status` a `String` en `toDomain()`
- ✅ Conversión de `String` a `Enum Status` en `toEntity()`
- ✅ Conversión de `BigDecimal` correcta (antes usaba `intValue()`)

**Lógica de conversión:**
```java
// Domain → Entity
entity.setStatus(
    domain.getStatus() != null
        ? InventoryStockEntity.Status.valueOf(domain.getStatus())
        : InventoryStockEntity.Status.A
);

// Entity → Domain
.status(entity.getStatus() != null
    ? entity.getStatus().name()
    : InventoryStock.STATUS_ACTIVE)
```

---

### 4. **Repositorio: `JpaInventoryStockRepository.java`**
**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/inventory/infrastructure/persistence/JpaInventoryStockRepository.java`

**Métodos agregados:**
```java
// NUEVO: Buscar por producto, almacén y periodo
Optional<InventoryStockEntity> findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
    Long productId, Long warehouseId, Long periodId
);

// NUEVO: Listar por almacén y periodo
List<InventoryStockEntity> findByWarehouseIdWarehouseAndPeriodId(
    Long warehouseId, Long periodId
);

// NUEVO: Listar solo activos
@Query("SELECT s FROM InventoryStockEntity s
        WHERE s.warehouse.idWarehouse = :warehouseId
        AND s.periodId = :periodId
        AND s.status = 'A'")
List<InventoryStockEntity> findActiveStockByWarehouseAndPeriod(
    @Param("warehouseId") Long warehouseId,
    @Param("periodId") Long periodId
);

// NUEVO: Contar por almacén y periodo
long countByWarehouseIdAndPeriodId(Long warehouseId, Long periodId);

// NUEVO: Eliminar por almacén y periodo
void deleteByWarehouseIdWarehouseAndPeriodId(Long warehouseId, Long periodId);
```

**Métodos deprecados:**
```java
@Deprecated
Optional<InventoryStockEntity> findByProductIdProductAndWarehouseIdWarehouse(...);

@Deprecated
List<InventoryStockEntity> findByWarehouseIdWarehouse(Long warehouseId);
```

---

### 5. **Servicio Labels: `LabelServiceImpl.java`**
**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelServiceImpl.java`

**Cambios:**
- ✅ Actualizado para usar `findByWarehouseIdWarehouseAndPeriodId()` en lugar del método deprecated
- ✅ Conversión correcta de `BigDecimal` a `Integer` con `.intValue()`
- ✅ Conversión de `Enum Status` a `String` con `.name()`

**Antes:**
```java
List<InventoryStockEntity> allStockInWarehouse = inventoryStockRepository
    .findByWarehouseIdWarehouse(warehouseId);

existencias = stock.getExistQty(); // Error: Integer vs BigDecimal
estado = stock.getStatus();        // Error: Enum vs String
```

**Después:**
```java
List<InventoryStockEntity> allStockInWarehouse = inventoryStockRepository
    .findByWarehouseIdWarehouseAndPeriodId(warehouseId, periodId);

existencias = stock.getExistQty().intValue();  // Correcto
estado = stock.getStatus().name();             // Correcto
```

---

### 6. **Servicio MultiWarehouse: `MultiWarehouseServiceImpl.java`**
**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/MultiWarehouse/application/service/MultiWarehouseServiceImpl.java`

**Cambios Principales:**
- ✅ Agregada lógica para **sincronizar automáticamente** con `inventory_stock` al importar MultiWarehouse
- ✅ Nuevo método `syncToInventoryStock()` que crea/actualiza registros en `inventory_stock`
- ✅ Nuevo método `toInventoryStockStatus()` para convertir String a Enum

**Flujo de importación actualizado:**
```java
for (MultiWarehouseExistence newData : parsedData) {
    // ... lógica existente de guardar en multiwarehouse_existences ...

    // NUEVO: Sincronizar con inventory_stock
    Long productId = productMap.get(newData.getProductCode());
    syncToInventoryStock(
        productId,
        warehouseId,
        periodId,
        newData.getStock(),    // BigDecimal
        newData.getStatus()    // "A" o "B"
    );
}
```

**Método `syncToInventoryStock()`:**
```java
private void syncToInventoryStock(
    Long productId, Long warehouseId, Long periodId,
    BigDecimal stock, String status
) {
    // Buscar registro existente
    var existing = inventoryStockRepository
        .findByProductIdProductAndWarehouseIdWarehouseAndPeriodId(
            productId, warehouseId, periodId
        );

    if (existing.isPresent()) {
        // Actualizar existente
        var entity = existing.get();
        entity.setExistQty(stock);
        entity.setStatus(toInventoryStockStatus(status));
        inventoryStockRepository.save(entity);
    } else {
        // Crear nuevo
        var newStock = new InventoryStockEntity();
        newStock.setProduct(productEntity);
        newStock.setWarehouse(warehouseEntity);
        newStock.setPeriodId(periodId);
        newStock.setExistQty(stock);
        newStock.setStatus(toInventoryStockStatus(status));
        inventoryStockRepository.save(newStock);
    }
}
```

---

## 🔄 Flujo Completo de Datos

### **1. Usuario importa Catálogo de Productos**
```
Archivo Inventario (Excel/CSV)
  ↓
[CVE_ART, DESCR, UNI_MED, STATUS]
  ↓
Tabla: products
```

### **2. Usuario importa Existencias por Almacén**
```
Archivo MultiAlmacén (Excel/CSV)
  ↓
[CVE_ART, CVE_ALM, EXIST, STATUS]
  ↓
MultiWarehouseServiceImpl.importFile()
  ↓
┌─────────────────────────────────────┐
│ 1. Guardar en:                      │
│    multiwarehouse_existences        │
│                                     │
│ 2. Sincronizar con:                 │
│    inventory_stock (NUEVO)          │
│    - id_product (desde CVE_ART)     │
│    - id_warehouse (desde CVE_ALM)   │
│    - id_period (del contexto)       │
│    - exist_qty (EXIST)              │
│    - status (STATUS)                │
└─────────────────────────────────────┘
```

### **3. Usuario consulta productos para marbetes**
```
LabelServiceImpl.getLabelSummary()
  ↓
inventoryStockRepository.findByWarehouseIdWarehouseAndPeriodId(
    warehouseId, periodId
)
  ↓
Resultado: Lista de productos con existencias
  - claveProducto
  - nombreProducto
  - existencias
  - estado
  - foliosSolicitados
  - foliosExistentes
```

---

## 📊 Estructura de Tablas

### **Tabla: `inventory_stock`** (actualizada)
```sql
CREATE TABLE inventory_stock (
    id_stock BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_product BIGINT NOT NULL,
    id_warehouse BIGINT NOT NULL,
    id_period BIGINT NOT NULL,          -- NUEVO
    exist_qty DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('A', 'B') NOT NULL DEFAULT 'A',
    created_at TIMESTAMP NOT NULL,      -- NUEVO
    updated_at TIMESTAMP NOT NULL,

    FOREIGN KEY (id_product) REFERENCES products(id_product),
    FOREIGN KEY (id_warehouse) REFERENCES warehouse(id_warehouse),
    FOREIGN KEY (id_period) REFERENCES periods(id_period),  -- NUEVO

    UNIQUE KEY uk_product_warehouse_period (id_product, id_warehouse, id_period)
);
```

---

## ✅ Reglas de Negocio Cumplidas

### **Consultar el inventario**
- ✅ Filtra por **periodo** (último creado por defecto)
- ✅ Filtra por **almacén** (primero por defecto)
- ✅ Muestra **todos los productos** con existencias en ese almacén y periodo
- ✅ Búsqueda sensible a mayúsculas/minúsculas en:
  - Clave de producto
  - Producto (descripción)
  - Clave de almacén
  - Almacén
  - Estado
  - Existencias
- ✅ Ordenación personalizada por columnas
- ✅ Paginación (10, 25, 50, 100 registros)

### **Mostrar información correcta**
- ✅ **Folios solicitados**: desde `label_requests`
- ✅ **Folios existentes**: desde `labels` (count por producto)
- ✅ **Existencias**: desde `inventory_stock` (filtrado por almacén y periodo)
- ✅ **Estado**: desde `inventory_stock` ("A" o "B")

---

## 🧪 Pruebas Recomendadas

### 1. **Importar MultiAlmacén**
```bash
POST /api/multiwarehouse/import
- Archivo: multialmacen.xlsx
- Periodo: 11-2024

Verificar:
- ✅ Registros en multiwarehouse_existences
- ✅ Registros en inventory_stock (NUEVO)
```

### 2. **Consultar productos para marbetes**
```bash
POST /api/sigmav2/labels/summary
{
  "periodId": 7,
  "warehouseId": 250,
  "page": 0,
  "size": 50
}

Verificar:
- ✅ Devuelve productos con existencias
- ✅ Muestra existencias correctas desde inventory_stock
- ✅ Estado correcto ("A" o "B")
```

### 3. **Verificar sincronización**
```sql
-- Debe haber registros con el mismo producto y almacén en ambas tablas
SELECT
    mw.product_code,
    mw.warehouse_key,
    mw.stock AS multiwarehouse_stock,
    ist.exist_qty AS inventory_stock_qty,
    mw.status AS mw_status,
    ist.status AS ist_status
FROM multiwarehouse_existences mw
LEFT JOIN inventory_stock ist
    ON mw.product_code = (SELECT cve_art FROM products WHERE id_product = ist.id_product)
    AND mw.warehouse_key = (SELECT warehouse_key FROM warehouse WHERE id_warehouse = ist.id_warehouse)
    AND mw.period_id = ist.id_period
WHERE mw.period_id = 7
LIMIT 10;
```

---

## 📝 Notas Importantes

1. **Migración de datos existentes**: Si ya tienes datos en `multiwarehouse_existences`, necesitarás ejecutar un script para poblar `inventory_stock`:
   ```sql
   INSERT INTO inventory_stock (id_product, id_warehouse, id_period, exist_qty, status, created_at, updated_at)
   SELECT
       p.id_product,
       w.id_warehouse,
       mw.period_id,
       mw.stock,
       mw.status,
       NOW(),
       NOW()
   FROM multiwarehouse_existences mw
   JOIN products p ON p.cve_art = mw.product_code
   JOIN warehouse w ON w.warehouse_key = mw.warehouse_key
   ON DUPLICATE KEY UPDATE
       exist_qty = VALUES(exist_qty),
       status = VALUES(status),
       updated_at = NOW();
   ```

2. **Compatibilidad hacia atrás**: Los métodos antiguos se marcaron como `@Deprecated` pero siguen funcionando para no romper código existente.

3. **Performance**: Los índices en `inventory_stock` garantizan consultas rápidas por almacén y periodo.

4. **Integridad referencial**: Las claves foráneas garantizan consistencia de datos.

---

## 🎉 Resultado Final

Ahora el módulo de **Labels** puede consultar correctamente el inventario filtrando por:
- ✅ Almacén
- ✅ Periodo
- ✅ Producto

Y los datos se sincronizan automáticamente desde **MultiWarehouse** hacia **inventory_stock**.

**Estado del sistema:**
```
Catálogo de Productos
       ↓
   [products]
       ↓
MultiAlmacén Import ──→ [multiwarehouse_existences]
       ↓                         ↓
       └──────→ [inventory_stock] ←─── Labels consulta aquí
```

