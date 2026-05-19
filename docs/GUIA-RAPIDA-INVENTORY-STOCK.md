# Guía Rápida - Uso de inventory_stock

## 🚀 Inicio Rápido

### 1. Ejecutar migraciones de base de datos

Las migraciones se ejecutarán automáticamente al iniciar la aplicación Spring Boot gracias a Flyway:

```bash
# Iniciar la aplicación
mvn spring-boot:run
```

O si prefieres compilar primero:

```bash
mvn clean package -DskipTests
java -jar target/SIGMAV2-0.0.1-SNAPSHOT.jar
```

### 2. Verificar que las tablas se crearon correctamente

Ejecuta en tu cliente MySQL:

```sql
-- Verificar estructura de inventory_stock
DESCRIBE inventory_stock;

-- Verificar que no hay datos aún
SELECT COUNT(*) FROM inventory_stock;
```

### 3. Importar datos de MultiAlmacén

**Opción A: A través de la API**

```bash
# Windows PowerShell
$token = "tu_token_jwt_aqui"
$headers = @{
    "Authorization" = "Bearer $token"
}

$formData = @{
    file = Get-Item "C:\ruta\a\multialmacen.xlsx"
}

Invoke-RestMethod -Uri "http://localhost:8080/api/multiwarehouse/import?period=11-2024" `
    -Method POST `
    -Headers $headers `
    -Form $formData
```

**Opción B: Migrar datos existentes**

Si ya tienes datos en `multiwarehouse_existences`, ejecuta el script SQL:

```sql
-- Este script ya se ejecutó automáticamente con Flyway
-- Si necesitas re-ejecutarlo manualmente:
SOURCE C:\Users\cesarg\Desktop\SIGMAV2\SIGMAV2\src\main\resources\db\migration\V1_1_2__Populate_inventory_stock_from_multiwarehouse.sql;
```

### 4. Verificar sincronización

Ejecuta el script de verificación:

```bash
mysql -u root -p tokai_db < verificar_sincronizacion_inventory_stock.sql
```

O ejecuta consultas individuales:

```sql
-- Ver total de registros
SELECT COUNT(*) FROM inventory_stock;

-- Ver registros por periodo
SELECT
    id_period,
    COUNT(*) as total_productos,
    SUM(exist_qty) as total_existencias
FROM inventory_stock
GROUP BY id_period;

-- Ver top 10 productos con más existencias
SELECT
    p.cve_art,
    p.descr,
    w.name_warehouse,
    ist.exist_qty,
    ist.status
FROM inventory_stock ist
JOIN products p ON p.id_product = ist.id_product
JOIN warehouse w ON w.id_warehouse = ist.id_warehouse
WHERE ist.id_period = 7
ORDER BY ist.exist_qty DESC
LIMIT 10;
```

### 5. Probar endpoint de Labels

**Opción A: Script PowerShell**

```bash
# Editar test-labels-summary.ps1 y agregar tu token
# Luego ejecutar:
.\test-labels-summary.ps1
```

**Opción B: Curl**

```bash
curl -X POST http://localhost:8080/api/sigmav2/labels/summary \
  -H "Authorization: Bearer TU_TOKEN_AQUI" \
  -H "Content-Type: application/json" \
  -d '{
    "periodId": 7,
    "warehouseId": 250,
    "page": 0,
    "size": 50,
    "sortBy": "nombreProducto",
    "sortDirection": "ASC"
  }'
```

**Opción C: Postman**

```
POST http://localhost:8080/api/sigmav2/labels/summary
Headers:
  Authorization: Bearer TU_TOKEN_AQUI
  Content-Type: application/json

Body (raw JSON):
{
  "periodId": 7,
  "warehouseId": 250,
  "page": 0,
  "size": 50,
  "searchText": null,
  "sortBy": "nombreProducto",
  "sortDirection": "ASC"
}
```

---

## 🔧 Solución de Problemas

### Problema: "No se encuentran productos"

**Causa:** No hay datos en `inventory_stock`

**Solución:**

1. Verificar que existen datos en `multiwarehouse_existences`:
```sql
SELECT COUNT(*) FROM multiwarehouse_existences;
```

2. Ejecutar migración manual:
```sql
SOURCE src/main/resources/db/migration/V1_1_2__Populate_inventory_stock_from_multiwarehouse.sql;
```

3. Importar archivo de MultiAlmacén de nuevo.

### Problema: "Cannot resolve table 'inventory_stock'"

**Causa:** El IDE no ha refrescado el esquema de la base de datos

**Solución:**
- En IntelliJ IDEA: Database → Refresh Schema
- Reiniciar el IDE
- Verificar en MySQL que la tabla existe:
```sql
SHOW TABLES LIKE 'inventory_stock';
```

### Problema: "Registros duplicados"

**Causa:** Se intentó insertar el mismo producto/almacén/periodo dos veces

**Solución:**
La tabla tiene constraint UNIQUE, por lo que automáticamente actualiza en lugar de duplicar:

```sql
-- Ver duplicados (no debería devolver nada)
SELECT
    id_product,
    id_warehouse,
    id_period,
    COUNT(*)
FROM inventory_stock
GROUP BY id_product, id_warehouse, id_period
HAVING COUNT(*) > 1;
```

### Problema: "Status = null o exist_qty = null"

**Causa:** Datos mal sincronizados

**Solución:**
```sql
-- Actualizar valores nulos
UPDATE inventory_stock
SET status = 'A'
WHERE status IS NULL;

UPDATE inventory_stock
SET exist_qty = 0.00
WHERE exist_qty IS NULL;
```

---

## 📊 Consultas Útiles

### Ver productos disponibles para marbetes (almacén 250, periodo 7)

```sql
SELECT
    p.cve_art AS clave_producto,
    p.descr AS producto,
    w.warehouse_key AS clave_almacen,
    w.name_warehouse AS almacen,
    ist.exist_qty AS existencias,
    ist.status AS estado,
    COALESCE(lr.requested_labels, 0) AS folios_solicitados,
    COALESCE(l.folios_generados, 0) AS folios_existentes
FROM inventory_stock ist
JOIN products p ON p.id_product = ist.id_product
JOIN warehouse w ON w.id_warehouse = ist.id_warehouse
LEFT JOIN label_requests lr
    ON lr.id_product = ist.id_product
    AND lr.id_warehouse = ist.id_warehouse
    AND lr.id_period = ist.id_period
LEFT JOIN (
    SELECT id_product, id_warehouse, id_period, COUNT(*) as folios_generados
    FROM labels
    GROUP BY id_product, id_warehouse, id_period
) l ON l.id_product = ist.id_product
    AND l.id_warehouse = ist.id_warehouse
    AND l.id_period = ist.id_period
WHERE ist.id_warehouse = 250
  AND ist.id_period = 7
  AND ist.status = 'A'
ORDER BY p.cve_art
LIMIT 50;
```

### Comparar datos MultiWarehouse vs Inventory Stock

```sql
SELECT
    'MultiWarehouse' AS origen,
    COUNT(DISTINCT product_code) AS productos,
    COUNT(DISTINCT warehouse_key) AS almacenes,
    SUM(stock) AS total_existencias
FROM multiwarehouse_existences
WHERE period_id = 7

UNION ALL

SELECT
    'Inventory Stock' AS origen,
    COUNT(DISTINCT id_product) AS productos,
    COUNT(DISTINCT id_warehouse) AS almacenes,
    SUM(exist_qty) AS total_existencias
FROM inventory_stock
WHERE id_period = 7;
```

### Ver historial de cambios en inventory_stock

```sql
SELECT
    p.cve_art,
    w.warehouse_key,
    ist.id_period,
    ist.exist_qty,
    ist.status,
    ist.created_at,
    ist.updated_at,
    TIMESTAMPDIFF(MINUTE, ist.created_at, ist.updated_at) AS minutos_hasta_actualizacion
FROM inventory_stock ist
JOIN products p ON p.id_product = ist.id_product
JOIN warehouse w ON w.id_warehouse = ist.id_warehouse
WHERE ist.updated_at > ist.created_at
ORDER BY ist.updated_at DESC
LIMIT 20;
```

---

## 🎯 Checklist de Validación

Usa este checklist para verificar que todo funciona correctamente:

- [ ] Tabla `inventory_stock` existe en la base de datos
- [ ] Tabla tiene constraint UNIQUE en (id_product, id_warehouse, id_period)
- [ ] Datos migrados desde `multiwarehouse_existences`
- [ ] Total de registros coincide entre ambas tablas
- [ ] Endpoint `/api/sigmav2/labels/summary` devuelve productos
- [ ] Búsqueda por texto funciona correctamente
- [ ] Ordenación funciona en todas las columnas
- [ ] Paginación funciona correctamente
- [ ] Filtros por periodo y almacén funcionan
- [ ] Al importar MultiAlmacén, se actualiza `inventory_stock`
- [ ] Estados A y B se muestran correctamente
- [ ] Existencias se muestran con 2 decimales

---

## 📞 Soporte

Si encuentras algún problema:

1. Revisa los logs de la aplicación:
```bash
tail -f logs/spring-boot-application.log
```

2. Verifica errores de Hibernate/JPA:
```sql
SHOW ENGINE INNODB STATUS;
```

3. Consulta la documentación completa:
- `docs/ACTUALIZACION-INVENTORY-STOCK.md`
- `docs/IMPLEMENTACION-REGLAS-NEGOCIO-MARBETES.md`

4. Ejecuta los scripts de verificación:
- `verificar_sincronizacion_inventory_stock.sql`
- `test-labels-summary.ps1`

