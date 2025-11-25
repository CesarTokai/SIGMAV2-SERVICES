# Script de Testing - Importación de MultiAlmacén

## Prerequisitos
1. Asegurar que la base de datos esté actualizada con las migraciones
2. Tener el archivo multialmacen.xlsx en la ruta correcta
3. Tener un periodo activo en el sistema

## Formato del Archivo Excel (multialmacen.xlsx)

### Columnas Requeridas:
| CVE_ALM | CVE_ART | DESCR | STATUS | EXIST |
|---------|---------|-------|--------|-------|
| ALM_01 | PROD_001 | Producto de prueba 1 | A | 100.50 |
| ALM_01 | PROD_002 | Producto de prueba 2 | A | 250.00 |
| ALM_02 | PROD_001 | Producto de prueba 1 | A | 75.25 |
| ALM_02 | PROD_003 | Producto de prueba 3 | B | 0.00 |

### Notas:
- **CVE_ALM**: Clave del almacén (puede ser nueva o existente)
- **CVE_ART**: Clave del producto (puede ser nuevo o existente)
- **DESCR**: Descripción (opcional, se sobrescribe con datos del inventario)
- **STATUS**: A (Alta) o B (Baja)
- **EXIST**: Número decimal con existencias

## Testing con cURL

### 1. Listar Periodos Disponibles
```bash
curl -X GET "http://localhost:8080/api/periods" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### 2. Importar Archivo MultiAlmacén
```bash
curl -X POST "http://localhost:8080/api/multiwarehouse/import" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx" \
  -F "period=01-2025"
```

### 3. Consultar MultiAlmacén con Paginación
```bash
curl -X GET "http://localhost:8080/api/multiwarehouse/existences?page=0&size=50&periodId=1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### 4. Buscar por Texto
```bash
curl -X GET "http://localhost:8080/api/multiwarehouse/existences?search=ALM_01&periodId=1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### 5. Ordenar por Columna
```bash
curl -X GET "http://localhost:8080/api/multiwarehouse/existences?orderBy=clave_producto&ascending=true&periodId=1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### 6. Exportar a CSV
```bash
curl -X POST "http://localhost:8080/api/multiwarehouse/export" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"periodId": 1}' \
  -o multiwarehouse_export.csv
```

## Testing con Postman

### Colección de Postman

#### Request 1: Login
```
POST http://localhost:8080/api/auth/login
Body (JSON):
{
  "username": "admin",
  "password": "password"
}
```
Guardar el token en una variable de entorno.

#### Request 2: Importar MultiAlmacén
```
POST http://localhost:8080/api/multiwarehouse/import
Headers:
  Authorization: Bearer {{token}}
Body (form-data):
  file: [Seleccionar multialmacen.xlsx]
  period: "01-2025"
```

#### Request 3: Consultar con Filtros
```
GET http://localhost:8080/api/multiwarehouse/existences
Headers:
  Authorization: Bearer {{token}}
Query Params:
  page: 0
  size: 50
  periodId: 1
  search: "ALM_01"
  orderBy: "clave_producto"
  ascending: true
```

## Casos de Prueba

### Caso 1: Almacén Nuevo
**Archivo Excel:**
```
CVE_ALM: "ALM_NUEVO"
CVE_ART: "PROD_001"
STATUS: "A"
EXIST: 100
```

**Resultado Esperado:**
- ✅ Se crea almacén con warehouse_key = "ALM_NUEVO"
- ✅ Se agrega observación: "Este almacén no existía y fue creado en la importación"
- ✅ Se crea registro en multiwarehouse_existences

**Verificación SQL:**
```sql
SELECT * FROM warehouse WHERE warehouse_key = 'ALM_NUEVO';
SELECT * FROM multiwarehouse_existences WHERE warehouse_key = 'ALM_NUEVO';
```

### Caso 2: Producto Nuevo
**Archivo Excel:**
```
CVE_ALM: "ALM_01"
CVE_ART: "PROD_NUEVO"
DESCR: "Producto de Prueba Nuevo"
STATUS: "A"
EXIST: 50
```

**Resultado Esperado:**
- ✅ Se crea producto con cve_art = "PROD_NUEVO"
- ✅ Estado del producto = "A"
- ✅ Descripción = "Producto de Prueba Nuevo"
- ✅ Se crea registro en multiwarehouse_existences

**Verificación SQL:**
```sql
SELECT * FROM products WHERE cve_art = 'PROD_NUEVO';
SELECT * FROM multiwarehouse_existences WHERE product_code = 'PROD_NUEVO';
```

### Caso 3: Actualización de Existencias
**Datos Previos:**
```
product_code: "PROD_001"
warehouse_key: "ALM_01"
stock: 100
```

**Archivo Excel:**
```
CVE_ALM: "ALM_01"
CVE_ART: "PROD_001"
STATUS: "A"
EXIST: 150
```

**Resultado Esperado:**
- ✅ stock actualizado a 150
- ✅ status permanece "A"
- ✅ product_name se actualiza desde inventario

**Verificación SQL:**
```sql
SELECT * FROM multiwarehouse_existences
WHERE product_code = 'PROD_001' AND warehouse_key = 'ALM_01';
```

### Caso 4: Marcar como Baja
**Datos Previos:**
```
product_code: "PROD_002"
warehouse_key: "ALM_01"
status: "A"
```

**Archivo Excel:** (NO incluye PROD_002)
```
CVE_ALM: "ALM_01"
CVE_ART: "PROD_001"
STATUS: "A"
EXIST: 100
```

**Resultado Esperado:**
- ✅ PROD_002 status cambia a "B"
- ✅ stock no cambia

**Verificación SQL:**
```sql
SELECT * FROM multiwarehouse_existences
WHERE product_code = 'PROD_002' AND warehouse_key = 'ALM_01';
-- Debe mostrar status = 'B'
```

### Caso 5: Búsqueda por Clave de Almacén
**Request:**
```
GET /api/multiwarehouse/existences?search=ALM_01&periodId=1
```

**Resultado Esperado:**
- ✅ Retorna todos los productos con warehouse_key = "ALM_01"
- ✅ Incluye warehouse_name en la respuesta
- ✅ Paginación funciona correctamente

### Caso 6: Ordenación Personalizada
**Request:**
```
GET /api/multiwarehouse/existences?orderBy=clave_almacen&ascending=false&periodId=1
```

**Resultado Esperado:**
- ✅ Resultados ordenados por warehouse_key descendente
- ✅ Respeta paginación

## Verificación de Base de Datos

### Verificar estructura de tabla
```sql
DESCRIBE multiwarehouse_existences;
-- Debe mostrar columna warehouse_key VARCHAR(50)
```

### Verificar índices
```sql
SHOW INDEX FROM multiwarehouse_existences;
-- Debe incluir idx_multiwarehouse_warehouse_key
-- Debe incluir idx_multiwarehouse_product_warehouse
```

### Verificar datos después de importación
```sql
-- Contar registros por almacén
SELECT warehouse_key, warehouse_name, COUNT(*) as total
FROM multiwarehouse_existences
WHERE period_id = 1
GROUP BY warehouse_key, warehouse_name;

-- Verificar productos con estado "B"
SELECT product_code, product_name, warehouse_key, status, stock
FROM multiwarehouse_existences
WHERE period_id = 1 AND status = 'B';

-- Verificar almacenes creados automáticamente
SELECT warehouse_key, name_warehouse, observations
FROM warehouse
WHERE observations LIKE '%creado en la importación%';
```

## Errores Comunes

### Error: "Periodo* es obligatorio"
**Causa:** No se proporcionó el parámetro "period"
**Solución:** Agregar `period=MM-yyyy` en la request

### Error: "El archivo CSV no contiene todas las columnas requeridas"
**Causa:** Faltan columnas CVE_ALM, CVE_ART, STATUS o EXIST
**Solución:** Verificar que el archivo tenga todas las columnas obligatorias

### Error: "El periodo está CLOSED, no se permite importar"
**Causa:** El periodo seleccionado está cerrado
**Solución:** Seleccionar un periodo con estado OPEN o ACTIVE

### Error: "El archivo ya fue importado previamente"
**Causa:** El hash del archivo ya existe en import_log
**Solución:** Modificar el archivo o usar un periodo diferente

## Logs de Importación

### Consultar logs
```sql
SELECT * FROM multiwarehouse_import_logs
ORDER BY import_date DESC
LIMIT 10;
```

### Verificar hash de archivo
```sql
SELECT file_name, period, file_hash, status, message
FROM multiwarehouse_import_logs
WHERE period = '01-2025';
```

## Conclusión

Este script de testing cubre todos los casos de uso principales del sistema de MultiAlmacén. Asegúrese de ejecutar cada caso de prueba y verificar los resultados tanto en la API como en la base de datos.

Para soporte adicional, consulte:
- `/docs/CORRECCION-MULTIALMACEN-REGLAS-NEGOCIO.md`
- `/docs/GUIA-USO-CATALOGO-INVENTARIO.md`
- `/docs/inventory-api-usage.md`

