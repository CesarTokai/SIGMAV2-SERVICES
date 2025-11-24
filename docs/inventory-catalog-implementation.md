# Catálogo de Inventario - Documentación de Implementación

## Descripción General

El **Catálogo de Inventario** es una funcionalidad del SIGMA que permite a los administradores consultar el inventario de la organización por periodo. Esta implementación incluye consultas paginadas, búsqueda en tiempo real y ordenación personalizable.

## Características Implementadas

### Backend (Spring Boot)

#### 1. Endpoint de Consulta de Inventario con Paginación
**URL:** `GET /api/sigmav2/inventory/period-report`

**Parámetros:**
- `periodId` (Long, requerido): ID del periodo a consultar
- `warehouseId` (Long, opcional): ID del almacén (para filtrar por almacén específico)
- `search` (String, opcional): Texto de búsqueda (busca en clave de producto, producto y unidad)
- `page` (int, default: 0): Número de página (base 0)
- `size` (int, default: 10): Cantidad de registros por página
- `sort` (String[], default: "cveArt,asc"): Campo y dirección de ordenación

**Respuesta:** Objeto `Page<InventoryPeriodReportDTO>` con:
```json
{
  "content": [
    {
      "cveArt": "PROD001",
      "descr": "Producto de ejemplo",
      "uniMed": "PZA",
      "existQty": 100.00,
      "status": "A"
    }
  ],
  "totalElements": 150,
  "totalPages": 15,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false
}
```

#### 2. Endpoint para Obtener el Último Periodo
**URL:** `GET /api/sigmav2/inventory/latest-period`

**Respuesta:** Objeto `Period` con el último periodo registrado
```json
{
  "id": 12,
  "date": "2024-01-01",
  "comments": "Periodo enero 2024",
  "state": "OPEN"
}
```

#### 3. Endpoint para Listar Todos los Periodos
**URL:** `GET /api/sigmav2/inventory/all-periods`

**Respuesta:** Lista de objetos `Period`

### Frontend (HTML + JavaScript)

#### Archivo: `inventory-catalog.html`

**Características:**
- Selección de periodo mediante dropdown (carga automáticamente el último periodo)
- Búsqueda en tiempo real con debounce (500ms)
- Tabla con las siguientes columnas:
  - Clave de Producto
  - Producto
  - Unidad
  - Existencias
  - Estado (A - Alta, B - Baja)
- Ordenación por cualquier columna (clic en encabezado)
- Paginación con opciones de 10, 25, 50 o 100 registros por página
- Navegación: Primera, Anterior, Siguiente, Última página
- Diseño responsive y moderno
- Manejo de errores y estados de carga

## Estructura de Base de Datos

### Tabla: `inventory_snapshot`
```sql
CREATE TABLE inventory_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    period_id BIGINT NOT NULL,
    exist_qty DECIMAL(10,2),
    status VARCHAR(1), -- 'A' = Alta, 'B' = Baja
    created_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id_product),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id_warehouse),
    FOREIGN KEY (period_id) REFERENCES period(id_period)
);
```

### Tabla: `products`
```sql
CREATE TABLE products (
    id_product BIGINT PRIMARY KEY AUTO_INCREMENT,
    cve_art VARCHAR(50),
    descr VARCHAR(255),
    uni_med VARCHAR(20),
    status VARCHAR(1),
    lin_prod VARCHAR(50),
    created_at TIMESTAMP
);
```

### Tabla: `period`
```sql
CREATE TABLE period (
    id_period BIGINT PRIMARY KEY AUTO_INCREMENT,
    period DATE NOT NULL UNIQUE,
    comments VARCHAR(500),
    state VARCHAR(20) NOT NULL
);
```

## Archivos Modificados/Creados

### Backend
1. **JpaInventorySnapshotRepository.java** (Modificado)
   - Agregado método `findByPeriodWithSearch()` con soporte para paginación y búsqueda

2. **InventoryController.java** (Modificado)
   - Agregado endpoint `/period-report` mejorado con paginación
   - Agregado endpoint `/latest-period`
   - Agregado método auxiliar `mapSortField()` para mapeo de campos

3. **PeriodRepository.java** (Modificado)
   - Agregado método `findLatest()`

4. **JpaPeriodRepository.java** (Modificado)
   - Agregado método `findLatestPeriod()`

5. **PeriodRepositoryAdapter.java** (Modificado)
   - Implementado método `findLatest()`

### Frontend
1. **inventory-catalog.html** (Creado)
   - Interfaz completa de usuario para consulta de inventario

## Uso de la API

### Ejemplo 1: Consultar inventario del periodo 12 (primera página, 10 registros)
```bash
GET /api/sigmav2/inventory/period-report?periodId=12&page=0&size=10&sort=cveArt,asc
```

### Ejemplo 2: Buscar productos que contengan "BOLT" en el periodo 12
```bash
GET /api/sigmav2/inventory/period-report?periodId=12&search=BOLT&page=0&size=25
```

### Ejemplo 3: Ordenar por existencias descendente
```bash
GET /api/sigmav2/inventory/period-report?periodId=12&page=0&size=50&sort=existQty,desc
```

### Ejemplo 4: Filtrar por almacén específico
```bash
GET /api/sigmav2/inventory/period-report?periodId=12&warehouseId=5&page=0&size=10
```

## Consideraciones de Seguridad

- Solo usuarios con rol **Administrador** pueden acceder a estos endpoints
- La autenticación se realiza mediante JWT token
- El token debe enviarse en el header: `Authorization: Bearer {token}`

## Instrucciones de Despliegue

1. **Compilar el proyecto:**
   ```bash
   mvn clean install
   ```

2. **Ejecutar el proyecto:**
   ```bash
   mvn spring-boot:run
   ```

3. **Acceder a la interfaz:**
   ```
   http://localhost:8080/inventory-catalog.html
   ```

## Configuración de Seguridad

Asegúrese de que el archivo de configuración de seguridad (`SecurityConfig.java`) permita el acceso a los endpoints de inventario solo para usuarios autenticados con rol ADMIN:

```java
.requestMatchers("/api/sigmav2/inventory/**").hasRole("ADMIN")
```

## Testing

### Probar con cURL

1. **Obtener token de autenticación:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'
```

2. **Consultar inventario:**
```bash
curl -X GET "http://localhost:8080/api/sigmav2/inventory/period-report?periodId=12&page=0&size=10" \
  -H "Authorization: Bearer {your-token}"
```

## Mejoras Futuras

1. **Exportación de datos** (Excel, PDF)
2. **Filtros avanzados** (por línea de producto, rango de existencias)
3. **Gráficos y estadísticas** (productos con más/menos existencias)
4. **Historial de cambios** (auditoría de modificaciones)
5. **Alertas de stock bajo** (notificaciones automáticas)
6. **Importación desde múltiples formatos** (CSV, Excel, JSON)

## Soporte

Para preguntas o problemas, contactar al equipo de desarrollo de SIGMA.

