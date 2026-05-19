# Refactorización de Asignación de Almacenes - Pantallas Separadas

## Cambios Realizados

Se han separado las dos modalidades de asignación en pantallas independientes para mejorar la organización y evitar mezclar lógicas.

### Estructura Anterior
```
AsignacionAlmacenes.vue (Contenedor principal con ambas lógicas)
├── Asignación Individual (inline)
└── Asignación por Lotes (inline)
```

### Estructura Nueva
```
AsignacionAlmacenes.vue (Contenedor de navegación)
├── AsignacionIndividual.vue (Componente independiente)
└── AsignacionPorLotes.vue (Componente independiente)
```

---

## Archivos Creados/Modificados

### 1. **AsignacionIndividual.vue** (NUEVO)
- **Ubicación**: `src/modules/admin/views/almacenAdmin/AsignacionIndividual.vue`
- **Descripción**: Pantalla para asignar un usuario a múltiples almacenes individualmente
- **Características**:
  - Selección de un usuario
  - Selección de un almacén disponible
  - Tabla de asignaciones actuales del usuario
  - Paginación de asignaciones
  - Opción para revocar asignaciones
  - Breadcrumb específico con navegación

### 2. **AsignacionPorLotes.vue** (NUEVO)
- **Ubicación**: `src/modules/admin/views/almacenAdmin/AsignacionPorLotes.vue`
- **Descripción**: Pantalla para asignar múltiples usuarios a múltiples almacenes simultáneamente
- **Características**:
  - Selección múltiple de usuarios con búsqueda
  - Selección múltiple de almacenes con búsqueda
  - Resumen visual de asignaciones a crear
  - Confirmación antes de crear las asignaciones
  - Breadcrumb específico con navegación

### 3. **AsignacionAlmacenes.vue** (MODIFICADO)
- **Ubicación**: `src/modules/admin/views/almacenAdmin/AsignacionAlmacenes.vue`
- **Cambios**:
  - Eliminada toda la lógica individual (movida a AsignacionIndividual.vue)
  - Eliminada toda la lógica de lotes (movida a AsignacionPorLotes.vue)
  - Se convierte en un contenedor de navegación
  - Sistema de tabs para cambiar entre ambas vistas
  - Breadcrumb unificado en la pantalla principal

---

## Navegación y Breadcrumb

### Pantalla Principal (AsignacionAlmacenes.vue)
```
Gestión de Almacenes / Asignación de Almacenes
```

### Pantalla Individual (AsignacionIndividual.vue)
```
Gestión de Almacenes / Asignación / Individual
```

### Pantalla por Lotes (AsignacionPorLotes.vue)
```
Gestión de Almacenes / Asignación / Por Lotes
```

---

## Estructura de Navegación

El componente principal muestra dos botones con tabs:
- **👤 Asignación Individual**: Accede a `AsignacionIndividual.vue`
- **📦 Asignación por Lotes**: Accede a `AsignacionPorLotes.vue`

Cada componente tiene su propio breadcrumb que permite:
- Volver a "Gestión de Almacenes"
- Volver a "Asignación" (componente principal)
- Mostrar la opción actual ("Individual" o "Por Lotes")

---

## Lógica Separada

### AsignacionIndividual
```typescript
// Estados específicos
const selectedUser = ref<User | null>(null);
const selectedWarehouseId = ref<number | null>(null);
const assignedWarehouses = ref<AssignedWarehouse[]>([]);
const availableWarehouses = ref<Warehouse[]>([]);

// Funciones específicas
- onUserSelect()
- createAssignment()
- unassignWarehouse()
- loadUserWarehouses()
```

### AsignacionPorLotes
```typescript
// Estados específicos
const selectedUserIds = ref<number[]>([]);
const selectedWarehouseIds = ref<number[]>([]);
const searchUsersTerm = ref('');
const searchWarehousesTerm = ref('');

// Funciones específicas
- toggleUserSelection()
- toggleWarehouseSelection()
- confirmBatchAssignment()
```

---

## Beneficios de Esta Refactorización

1. **Separación de Responsabilidades**: Cada componente tiene una lógica clara y única
2. **Mantenimiento Más Fácil**: Cambios en un flujo no afectan al otro
3. **Mejor Escalabilidad**: Es más sencillo agregar nuevas características a cada opción
4. **Código Más Legible**: Menos código por archivo hace que sea más fácil de entender
5. **Mejor Testing**: Es más fácil escribir tests para componentes independientes

---

## APIs Utilizadas

Ambos componentes utilizan los mismos endpoints:

- `GET /users` - Obtener lista de usuarios
- `GET /warehouses` - Obtener lista de almacenes
- `GET /warehouses/users/:userId/warehouses` - Obtener almacenes asignados a un usuario
- `POST /warehouses/users/:userId/assign` - Asignar almacenes a un usuario
- `DELETE /warehouses/users/:userId/warehouses/:warehouseId` - Revocar asignación

---

## Estilos y Diseño

### Consistencia
- Ambos componentes siguen el mismo esquema de colores
- Tipografía uniforme
- Componentes reutilizables (badges, botones, etc.)

### Componentes Reutilizables
- `SweetAlert` para confirmaciones
- `ToastError` para notificaciones de error
- Sistema de paginación (solo en Individual)
- Avatares con colores dinámicos

---

## Próximas Mejoras Posibles

1. Agregar exportación a CSV en la pantalla individual
2. Agregar validación de permisos por rol
3. Historial de cambios en asignaciones
4. Notificaciones en tiempo real cuando se asignan almacenes
5. Búsqueda avanzada con filtros adicionales

---

## Cómo Usar

### Para los Usuarios
1. Acceder a "Gestión de Almacenes" > "Asignación de Almacenes"
2. Elegir entre:
   - **Individual**: Para asignar un usuario a varios almacenes
   - **Por Lotes**: Para asignar múltiples usuarios a múltiples almacenes
3. Completar la selección y confirmar

### Para los Desarrolladores
Los componentes están listos para ser modificados independientemente:
- Modificaciones en `AsignacionIndividual.vue` no afectan `AsignacionPorLotes.vue`
- Ambos componentes importan desde `AsignacionAlmacenes.vue` como padre
- El estado es local a cada componente (no hay estado compartido complejo)

---

## Fecha de Implementación
2026-02-04

## Estado
✅ Implementado y listo para pruebas
