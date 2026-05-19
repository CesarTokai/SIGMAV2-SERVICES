# 📡 API: GET /api/sigmav2/admin/users/with-warehouses

## 📋 Descripción General

Esta API lista todos los **usuarios que tienen al menos un almacén asignado**, con información paginada sobre:
- ID del usuario
- Email
- Rol
- Estado (activo/inactivo)
- Cantidad de almacenes asignados
- IDs de almacenes asignados

---

## 🔐 Seguridad

| Atributo | Valor |
|----------|-------|
| **Rol requerido** | `ADMINISTRADOR` |
| **Autenticación** | JWT Bearer Token |
| **HTTP Method** | `GET` |

```
Authorization: Bearer {tu_token_jwt}
```

---

## 📥 Parámetros de Entrada (Query Parameters)

Todos son **opcionales**:

| Parámetro | Tipo | Defecto | Descripción |
|-----------|------|---------|-------------|
| `page` | `int` | `0` | Número de página (0-indexed) |
| `size` | `int` | `20` | Cantidad de registros por página |
| `sortBy` | `string` | `email` | Campo para ordenar: `email` o `userId` o `warehousesCount` |
| `sortDir` | `string` | `asc` | Dirección: `asc` (ascendente) o `desc` (descendente) |

### Ejemplos de URLs:

```
# Página 0, 20 registros por página, ordenado por email ascendente (defecto)
GET /api/sigmav2/admin/users/with-warehouses

# Página 1, 10 registros por página
GET /api/sigmav2/admin/users/with-warehouses?page=1&size=10

# Ordenado por cantidad de almacenes, descendente
GET /api/sigmav2/admin/users/with-warehouses?sortBy=warehousesCount&sortDir=desc

# Ordenado por ID de usuario, ascendente
GET /api/sigmav2/admin/users/with-warehouses?sortBy=userId&sortDir=asc
```

---

## 📤 Respuesta

### ✅ Respuesta Exitosa (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "userId": 2,
      "email": "almacenista@example.com",
      "role": "ALMACENISTA",
      "status": true,
      "warehousesCount": 3,
      "warehouseIds": [420, 421, 422]
    },
    {
      "userId": 5,
      "email": "auxiliar@example.com",
      "role": "AUXILIAR_DE_CONTEO",
      "status": true,
      "warehousesCount": 1,
      "warehouseIds": [420]
    },
    {
      "userId": 7,
      "email": "auxiliar2@example.com",
      "role": "AUXILIAR",
      "status": false,
      "warehousesCount": 5,
      "warehouseIds": [420, 421, 422, 423, 424]
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 20,
  "hasNext": false,
  "hasPrevious": false
}
```

### 📊 Estructura de Respuesta

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `success` | `boolean` | Indica si la operación fue exitosa |
| `data` | `array` | Arreglo de usuarios con almacenes |
| `totalElements` | `long` | Total de usuarios con almacenes (sin paginar) |
| `totalPages` | `int` | Total de páginas |
| `currentPage` | `int` | Página actual (0-indexed) |
| `pageSize` | `int` | Registros por página |
| `hasNext` | `boolean` | ¿Hay siguiente página? |
| `hasPrevious` | `boolean` | ¿Hay página anterior? |

### 📋 Estructura de cada usuario en `data`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `userId` | `long` | ID único del usuario |
| `email` | `string` | Email del usuario |
| `role` | `string` | Rol del usuario (ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO) |
| `status` | `boolean` | `true` = activo, `false` = inactivo |
| `warehousesCount` | `long` | Cantidad de almacenes asignados |
| `warehouseIds` | `array[long]` | IDs de almacenes asignados al usuario |

---

## ❌ Respuestas de Error

### 401 Unauthorized - Token inválido o expirado

```json
{
  "timestamp": "2025-03-11T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token JWT inválido o expirado"
}
```

### 403 Forbidden - Sin permiso

```json
{
  "timestamp": "2025-03-11T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Solo administradores pueden acceder a este recurso"
}
```

---

## 🔄 Flujo de Procesamiento

```
┌─────────────────────────────────────────────┐
│ GET /api/sigmav2/admin/users/with-warehouses│
│ ?page=0&size=20&sortBy=email&sortDir=asc    │
└────────────┬────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│ AdminUserController              │
│ .getUsersWithWarehouses()        │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────────────┐
│ 1. Validar parámetros de ordenamiento                    │
│    - sortBy solo permite: userId, warehousesCount, email │
│    - sortDir solo permite: asc, desc                     │
└────────────┬─────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────┐
│ 2. UserWarehouseAssignmentRepository        │
│    .findUsersWithActiveWarehouses(pageable) │
│                                              │
│ SQL QUERY:                                  │
│ SELECT u.userId AS userId,                 │
│        COUNT(u.warehouseId) AS warehousesCount
│ FROM user_warehouse_assignments u          │
│ WHERE u.warehouse.deleted_at IS NULL       │
│ GROUP BY u.userId                          │
│ OFFSET {page * size} LIMIT {size}          │
└────────────┬────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────────────┐
│ 3. Para cada usuario encontrado:                │
│    a) Buscar datos del usuario en la BD          │
│    b) Obtener lista de IDs de almacenes asignados
│    c) Construir UserWarehouseSummaryResponse    │
└────────────┬───────────────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────┐
│ 4. Construir respuesta paginada          │
│    - success: true                       │
│    - data: lista de usuarios             │
│    - totalElements: total sin paginar    │
│    - totalPages, currentPage, hasNext... │
└────────────┬───────────────────────────┘
             │
             ▼
┌──────────────────────────────────────────┐
│ 200 OK - Respuesta JSON                  │
└──────────────────────────────────────────┘
```

---

## 💾 Base de Datos - Tablas Involucradas

### `user_warehouse_assignments` (alias: `user_warehouses`)

Tabla de asignaciones de usuarios a almacenes:

```sql
CREATE TABLE user_warehouse_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id),
    UNIQUE KEY uk_user_warehouse (user_id, warehouse_id)
);
```

### `users`

Tabla de usuarios:

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,  -- ADMINISTRADOR, ALMACENISTA, AUXILIAR, AUXILIAR_DE_CONTEO
    status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
);
```

### Query SQL que ejecuta internamente

```sql
SELECT 
    u.userId AS userId,
    COUNT(u.warehouseId) AS warehousesCount
FROM user_warehouse_assignments u
WHERE u.warehouse_id NOT IN (
    SELECT w.id FROM warehouses w WHERE w.deleted_at IS NOT NULL
)
GROUP BY u.userId
ORDER BY {sortBy} {sortDir}
LIMIT {size} OFFSET {page * size};
```

---

## 📝 Uso en Frontend

### Ejemplo con Axios

```typescript
// Obtener usuarios con almacenes asignados
const obtenerUsuariosConAlmacenes = async () => {
  try {
    const response = await axios.get(
      '/api/sigmav2/admin/users/with-warehouses',
      {
        params: {
          page: 0,
          size: 20,
          sortBy: 'email',
          sortDir: 'asc'
        }
      }
    );
    
    console.log('Usuarios:', response.data.data);
    console.log('Total:', response.data.totalElements);
    console.log('Páginas:', response.data.totalPages);
    
    return response.data;
  } catch (error) {
    console.error('Error al obtener usuarios:', error);
  }
};

// Casos de uso comunes

// 1. Usuarios con más almacenes asignados
const usuariosMasCargados = await axios.get(
  '/api/sigmav2/admin/users/with-warehouses?sortBy=warehousesCount&sortDir=desc'
);

// 2. Usuarios paginados de a 10 registros
const pagina2 = await axios.get(
  '/api/sigmav2/admin/users/with-warehouses?page=1&size=10'
);

// 3. Usuarios ordenados por ID
const usuariosOrdenados = await axios.get(
  '/api/sigmav2/admin/users/with-warehouses?sortBy=userId&sortDir=asc'
);
```

---

## 🎯 Casos de Uso

### 1. Listar todos los usuarios con almacenes para asignación de permisos

```bash
GET /api/sigmav2/admin/users/with-warehouses
```

### 2. Encontrar usuarios con pocos almacenes asignados

```bash
GET /api/sigmav2/admin/users/with-warehouses?sortBy=warehousesCount&sortDir=asc&size=50
```

### 3. Buscar usuario específico e imponer almacén (requiere búsqueda adicional)

```bash
# Esta API no filtra por usuario, pero devuelve todos
# Para buscar un usuario específico, usar:
GET /api/sigmav2/admin/users/{userId}
```

### 4. Auditoría - ver qué usuarios tienen acceso a qué almacenes

```bash
GET /api/sigmav2/admin/users/with-warehouses?sortBy=email&sortDir=asc&size=100
# Revisar warehouseIds de cada usuario
```

---

## ⚙️ Implementación en Código

### Ubicación del Controlador

**Archivo:** `AdminUserController.java`

**Ubicación:** `modules/users/adapter/web/`

**Método:** `getUsersWithWarehouses(page, size, sortBy, sortDir)`

**Líneas:** 435-486

```java
@GetMapping("/with-warehouses")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public ResponseEntity<Map<String, Object>> getUsersWithWarehouses(
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "sortBy", defaultValue = "email") String sortBy,
        @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir)
```

### DTO de Respuesta

**Clase:** `UserWarehouseSummaryResponse`

**Ubicación:** `modules/users/adapter/web/dto/`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWarehouseSummaryResponse {
    private Long userId;
    private String email;
    private String role;
    private boolean status;
    private long warehousesCount;
    private List<Long> warehouseIds;
}
```

### Repositorio

**Interfaz:** `UserWarehouseAssignmentRepository` o `UserWarehouseRepository`

**Método:** `findUsersWithActiveWarehouses(Pageable)`

---

## 🚀 Mejoras Sugeridas

1. **Filtrado por rol** - Agregar parámetro `role` para filtrar por ALMACENISTA, AUXILIAR, etc.
2. **Búsqueda por email** - Agregar parámetro `search` para buscar usuarios por email
3. **Filtrado por estado** - Agregar parámetro `status` para filtrar activos/inactivos
4. **Exportación** - Agregar endpoint para exportar a CSV/Excel

---

## 📚 Relacionados

- `GET /api/sigmav2/warehouses/users/{userId}` - Obtener almacenes de un usuario
- `POST /api/sigmav2/warehouses/users/{userId}/assign` - Asignar almacenes a usuario
- `DELETE /api/sigmav2/warehouses/users/{userId}/warehouses/{warehouseId}` - Revocar almacén
- `GET /api/sigmav2/admin/users` - Listar todos los usuarios

