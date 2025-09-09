# Módulo de Inventarios - SIGMAV2

## Descripción

El módulo de inventarios permite la **carga masiva de archivos Excel** con validación completa de duplicados, productos inexistentes y generación de logs descargables en formato CSV.

## ✅ Reglas de Negocio Implementadas

### **Carga Masiva de Archivo INVENTARIO.XLSX**

**Datos de entrada:**
- ✅ **Archivo Excel*** – Formato XLSX/XLS válido con columnas: CVE_ART, DESCR, UNI_MED, EXIST, STATUS
- ✅ **Periodo*** – Selección obligatoria de periodo existente

**Validaciones implementadas:**
- ✅ **Valida duplicados** en el archivo (mismo CVE_ART + almacén)
- ✅ **Valida productos inexistentes** (CVE_ART debe existir en catálogo)
- ✅ **Transacción envolvente** - rollback completo en caso de error
- ✅ **Generación de log descargable** (.CSV) con altas, bajas y actualizaciones

**Criterios de aceptación cumplidos:**
- ✅ Se genera log descargable (.CSV) con todas las operaciones
- ✅ Transacción todo-o-nada implementada
- ✅ Validación completa de integridad de datos

## Características Implementadas

### ✅ Entidades
- **Product**: Catálogo de productos con clave única
- **Warehouse**: Catálogo de almacenes
- **Period**: Periodos de inventario (formato YYYY-MM-01)
- **InventoryStock**: Existencias por producto y almacén

### ✅ Funcionalidades
1. **Importación de inventarios**
   - ✅ Formato XLSX/XLS/CSV soportado completamente
   - ✅ Validaciones de integridad de datos
   - ✅ Modo MERGE y REPLACE
   - ✅ Transacciones atómicas (todo o nada)
   - ✅ **Detección de duplicados en archivo**
   - ✅ **Validación de productos inexistentes**
   - ✅ **Log detallado CSV descargable**

2. **Seguimiento de importaciones**
   - ✅ Tabla `inventory_import_jobs` para auditoría
   - ✅ Estados: PENDING, RUNNING, DONE, ERROR
   - ✅ Checksums para prevenir duplicados
   - ✅ Logs descargables por job

3. **Consulta de inventarios**
   - ✅ Filtros por periodo, almacén y texto libre
   - ✅ Paginación configurable
   - ✅ Ordenamiento por múltiples campos

4. **Validaciones de negocio**
   - ✅ Periodo obligatorio
   - ✅ CVE_ART debe existir en productos
   - ✅ EXIST >= 0
   - ✅ STATUS ∈ {'A','B'}
   - ✅ **Sin duplicados en archivo**

## API Endpoints

### Importar inventario
```http
POST /api/v1/inventory/import
Content-Type: multipart/form-data

Parámetros:
- file: archivo XLSX/XLS/CSV
- idPeriod: ID del periodo (obligatorio)
- idWarehouse: ID del almacén (opcional)
- mode: MERGE|REPLACE (default: MERGE)
- idempotencyKey: UUID opcional

Respuesta exitosa incluye:
- jobId: ID único del job
- jobIdLong: ID numérico para descargar log
- logDownloadUrl: URL para descargar el log CSV
```

### Descargar log de importación
```http
GET /api/v1/inventory/import/log/{jobId}

Descarga el archivo CSV con el log detallado de la importación.
Incluye: ROW_NUMBER, CVE_ART, OPERATION, DESCRIPTION, PREVIOUS_VALUE, NEW_VALUE, ERROR_MESSAGE
```

### Consultar inventarios
```http
GET /api/v1/inventory?idPeriod={id}&idWarehouse={id}&q={texto}&page={0}&size={25}&sort={campo}

Parámetros:
- idPeriod: ID del periodo (obligatorio)
- idWarehouse: ID del almacén (opcional)
- q: búsqueda por texto en clave/descripción (opcional)
- page: número de página (default: 0)
- size: tamaño de página (default: 25, max: 100)
- sort: campo de ordenamiento (default: cveArt)
```

### Exportar inventarios (Pendiente)
```http
GET /api/v1/inventory/export?idPeriod={id}&format={CSV|XLSX|PDF|TXT}
```

### Vista multi-almacén (Pendiente)
```http
GET /api/v1/inventory/multi-warehouse?q={texto}&page={0}&size={25}
```

## Formato de archivo Excel/CSV

### Encabezados obligatorios:
- `CVE_ART`: Clave del artículo
- `DESCR`: Descripción del producto  
- `UNI_MED`: Unidad de medida
- `EXIST`: Existencia (decimal, >= 0)
- `STATUS`: Estado (A=Activo, B=Baja, default: A)

### Encabezados opcionales:
- `WAREHOUSE_KEY`: Clave del almacén (si no se especifica idWarehouse)

### Ejemplo CSV:
```csv
CVE_ART,DESCR,UNI_MED,EXIST,STATUS,WAREHOUSE_KEY
PROD001,Producto de prueba,PCS,100.50,A,WH001
PROD002,Otro producto,KG,25.75,A,WH001
```

### Ejemplo Excel:
Mismas columnas en archivo XLSX/XLS con encabezados en la primera fila.

## Estructura de archivos

```
src/main/java/tokai/com/mx/SIGMAV2/modules/inventory/
├── controllers/
│   └── InventoryController.java
├── dto/
│   ├── ImportResultDto.java
│   ├── InventoryDto.java
│   └── InventoryImportRowDto.java
├── entities/
│   ├── InventoryStock.java
│   ├── Period.java
│   ├── Product.java
│   └── Warehouse.java
├── exceptions/
│   └── InventoryException.java
├── repositories/
│   ├── InventoryStockRepository.java
│   ├── PeriodRepository.java
│   ├── ProductRepository.java
│   └── WarehouseRepository.java
└── services/
    ├── FileParserService.java
    ├── InventoryImportService.java
    └── InventoryQueryService.java
```

## Próximas implementaciones

### 📋 Pendientes por completar:
1. ✅ ~~**Dependencias de Apache POI** para soporte XLSX~~ **COMPLETADO**
2. **Servicios de exportación** (XLSX/CSV/PDF/TXT)
3. **Vista multi-almacén** comparativa
4. ✅ ~~**Tabla de import jobs** para seguimiento~~ **COMPLETADO**
5. ✅ ~~**Validación de idempotencia** avanzada~~ **COMPLETADO**
6. **Más pruebas unitarias e integración**

### ✅ Dependencias agregadas al pom.xml:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi</artifactId>
    <version>5.2.3</version>
</dependency>
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

### 📋 Scripts SQL requeridos:
Ejecutar el script: `docs/sql/add_inventory_import_jobs.sql` para crear la tabla de jobs.

## Ejemplos de uso

### 1. Importar inventario
```bash
curl -X POST "http://localhost:8080/api/v1/inventory/import" \
  -F "file=@inventario.csv" \
  -F "idPeriod=1" \
  -F "idWarehouse=1" \
  -F "mode=MERGE"
```

### 2. Consultar inventario
```bash
curl "http://localhost:8080/api/v1/inventory?idPeriod=1&page=0&size=25&sort=cveArt"
```

### 3. Buscar productos
```bash
curl "http://localhost:8080/api/v1/inventory?idPeriod=1&q=producto&idWarehouse=1"
```

## Notas de implementación

- **Transacciones**: Todas las importaciones son atómicas (todo o nada)
- **Validaciones**: Se validan todas las filas antes de procesar
- **Performance**: Uso de JPA batch para operaciones masivas
- **Seguridad**: Filtros por almacén preparados para RBAC futuro
- **Logging**: Errores registrados para debugging

## ✅ Estado de Implementación

**Este módulo está COMPLETAMENTE FUNCIONAL** con todas las reglas de negocio implementadas:

✅ Carga masiva de archivos Excel (XLSX/XLS) y CSV  
✅ Validación de duplicados en archivo  
✅ Validación de productos inexistentes  
✅ Transacción envolvente con rollback completo  
✅ Log descargable CSV con todas las operaciones  
✅ Seguimiento completo de jobs de importación  

**Listo para producción** - Solo faltan exportaciones y vista multi-almacén (funcionalidades adicionales).