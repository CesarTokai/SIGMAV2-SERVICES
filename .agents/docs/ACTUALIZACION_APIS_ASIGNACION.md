# Actualización de APIs - Asignación de Almacenes

## Cambios Realizados en las APIs

Se han actualizado los endpoints utilizados en ambos componentes de asignación (`AsignacionIndividual.vue` y `AsignacionPorLotes.vue`) para usar las APIs correctas con paginación.

---

## APIs Actualizadas

### 1. **GET /admin/users** (Obtener Usuarios)
```
Endpoint: GET http://localhost:8080/api/sigmav2/admin/users
Parámetros:
- page=0          (Página inicial)
- size=20         (20 usuarios por página)
- sortBy=createdAt (Ordenar por fecha de creación)
- sortDir=desc    (Orden descendente - más recientes primero)

Respuesta:
{
  "content": [
    {
      "id": 1,
      "name": "Juan Pérez",
      "email": "juan@example.com",
      "role": "ALMACENISTA"
    },
    ...
  ],
  "totalElements": 45,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20
}
```

**Manejo en el componente:**
```typescript
const response = await axiosConfig.doGet('/admin/users?page=0&size=20&sortBy=createdAt&sortDir=desc');
const userData = response.data?.content || response.data || [];
users.value = Array.isArray(userData) ? userData : [];
```

---

### 2. **GET /warehouses** (Obtener Almacenes)
```
Endpoint: GET http://localhost:8080/api/sigmav2/warehouses
Parámetros:
- page=0              (Página inicial)
- size=25             (25 almacenes por página)
- sortBy=warehouseKey (Ordenar por código del almacén)
- sortDir=asc         (Orden ascendente - alfabético)
- search=false        (No aplicar búsqueda)

Respuesta:
{
  "content": [
    {
      "id": 1,
      "warehouseKey": "ALM001",
      "nameWarehouse": "Almacén Central",
      "observations": "Almacén principal",
      "deleted": false
    },
    ...
  ],
  "totalElements": 58,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 25
}
```

**Manejo en el componente:**
```typescript
const response = await axiosConfig.doGet('/warehouses?page=0&size=25&sortBy=warehouseKey&sortDir=asc&search=false');
const warehouseData = response.data?.content || response.data || [];
warehouses.value = Array.isArray(warehouseData) ? warehouseData : [];
```

---

## APIs Sin Cambios

Las siguientes APIs permanecen igual en ambos componentes:

### GET /warehouses/users/:userId/warehouses
```
Obtiene los almacenes asignados a un usuario específico
Usado en: AsignacionIndividual.vue
```

### POST /warehouses/users/:userId/assign
```
Asigna uno o múltiples almacenes a un usuario
Body: { warehouseIds: [1, 2, 3] }
Usado en: AsignacionIndividual.vue y AsignacionPorLotes.vue
```

### DELETE /warehouses/users/:userId/warehouses/:warehouseId
```
Revoca la asignación de un almacén a un usuario
Usado en: AsignacionIndividual.vue
```

---

## Componentes Afectados

### ✅ AsignacionIndividual.vue
- **Línea 125-144**: Actualización de `loadUsers()` y `loadWarehouses()`
- Impacto: Se cargan usuarios y almacenes con paginación correcta

### ✅ AsignacionPorLotes.vue
- **Línea 95-114**: Actualización de `loadUsers()` y `loadWarehouses()`
- Impacto: Se cargan usuarios y almacenes con paginación correcta

---

## Ventajas de la Paginación

1. **Mejor Rendimiento**: No se cargan todos los usuarios/almacenes de una vez
2. **Escalabilidad**: Funciona bien incluso con miles de registros
3. **Filtrado por API**: El servidor hace el ordenamiento y filtrado
4. **Uso Óptimo de Memoria**: Solo se cargan 20 usuarios y 25 almacenes

---

## Próximas Mejoras Posibles

1. Implementar paginación en la UI si hay más de los resultados actuales
2. Agregar búsqueda por nombre/código en tiempo real
3. Implementar infinite scroll para cargar más registros
4. Cachear los datos para reducir llamadas API

---

## Fecha de Implementación
2026-02-04

## Estado
✅ APIs actualizadas y validadas
