# Análisis de Inconsistencia en APIs de Asignación de Almacenes

## 📋 Problema Identificado

Existen **dos formas diferentes** de obtener la información de usuarios con almacenes asignados:

### 1. **AsignacionIndividual.vue**
```typescript
// Carga un usuario individual y luego carga sus almacenes
const selectedUser = ref<User>(); // Usuario seleccionado

await loadUserWarehouses(selectedUser.value.id);
// API: GET /warehouses/users/{userId}
// Retorna: Array de almacenes asignados a ese usuario
```

**Flujo:**
1. Seleccionar un usuario
2. Hacer llamada a `/warehouses/users/{userId}` 
3. Mostrar sus almacenes asignados en tabla

---

### 2. **AsignacionPorLotes.vue**
```typescript
// Carga todos los usuarios con sus almacenes en una sola llamada
const userAssignments = ref<UserAssignment[]>([]);

await loadUserAssignments();
// API: GET /warehouses/users-with-assignments
// Retorna: Array de todos los usuarios con sus almacenes asignados
```

**Flujo:**
1. Una sola llamada a `/warehouses/users-with-assignments`
2. Obtiene todos los usuarios con sus almacenes
3. Mostrar tabla unificada de asignaciones

---

## ⚠️ Problemas Generados

| Aspecto | AsignacionIndividual | AsignacionPorLotes |
|--------|---------------------|-------------------|
| **API usada** | `/warehouses/users/{userId}` | `/warehouses/users-with-assignments` |
| **Flujo** | Usuario → Almacenes | Todo en una llamada |
| **Consistencia** | Reactiva (seleccionar usuario) | Automática (carga todo) |
| **Performance** | Múltiples llamadas | Una sola llamada |
| **Mantenimiento** | 2 APIs diferentes | Confusión en la codebase |

---

## 🎯 Soluciones Posibles

### **Opción 1: Unificar a una sola API (Recomendado)**

Crear/usar una sola API que retorne **todos los usuarios con sus almacenes asignados**:

```typescript
// En AMBOS componentes
const loadUserAssignments = async () => {
  const response = await axiosConfig.doGet('/warehouses/users/assignments');
  // Retorna:
  // {
  //   data: [
  //     {
  //       userId: 1,
  //       email: "user@example.com",
  //       name: "Juan Pérez",
  //       role: "ALMACENISTA",
  //       warehouseIds: [1, 2, 3]
  //     },
  //     ...
  //   ]
  // }
};
```

**Ventajas:**
✅ Una sola API para ambos componentes
✅ Consistencia en toda la aplicación
✅ Más fácil de mantener
✅ Mejor performance (una llamada)

**Desventajas:**
❌ Si hay muchos usuarios, puede ser lenta
❌ Requiere cambios en el backend

---

### **Opción 2: Mantener APIs actuales pero mejor documentadas**

Si el backend tiene limitaciones, documentar claramente:

```typescript
// AsignacionIndividual.vue
// Obtiene almacenes de UN usuario específico
const loadUserWarehouses = async (userId: number) => {
  const response = await axiosConfig.doGet(`/warehouses/users/${userId}`);
  // Retorna: Array de almacenes del usuario
};

// AsignacionPorLotes.vue  
// Obtiene TODOS los usuarios con sus almacenes
const loadUserAssignments = async () => {
  const response = await axiosConfig.doGet('/warehouses/users-with-assignments');
  // Retorna: Array de UserAssignment con todos los datos
};
```

**Ventajas:**
✅ No requiere cambios en el backend
✅ Ambos métodos son válidos según el caso de uso

**Desventajas:**
❌ Dos APIs diferentes
❌ Riesgo de inconsistencia
❌ Más código para mantener

---

### **Opción 3: Unificar con parámetro opcional**

Una sola API que sea flexible:

```typescript
// Obtener UN usuario específico
const response1 = await axiosConfig.doGet('/warehouses/users/assignments?userId=1');

// Obtener TODOS los usuarios
const response2 = await axiosConfig.doGet('/warehouses/users/assignments');

// Respuesta en ambos casos:
{
  data: [
    {
      userId: 1,
      email: "user@example.com",
      name: "Juan Pérez",
      role: "ALMACENISTA",
      warehouseIds: [1, 2, 3],
      assignedWarehouses: [
        { warehouseId: 1, warehouseName: "Almacén Central" },
        { warehouseId: 2, warehouseName: "Almacén Norte" }
      ]
    }
  ]
}
```

**Ventajas:**
✅ Una sola API flexible
✅ Funciona para ambos casos de uso
✅ Mejor performance (obtener solo lo necesario)

**Desventajas:**
❌ Requiere cambios en el backend
❌ Más complejidad en la API

---

## 📊 Comparativa de Respuestas

### **API Actual: `/warehouses/users/{userId}`**
```json
// Retorna SOLO almacenes de un usuario
[
  {
    "userId": 1,
    "warehouseId": 1,
    "assignedAt": "2026-02-01T10:30:00",
    "assignedBy": 5,
    "isActive": true
  },
  {
    "userId": 1,
    "warehouseId": 2,
    "assignedAt": "2026-02-01T10:30:00",
    "assignedBy": 5,
    "isActive": true
  }
]
```

### **API Actual: `/warehouses/users-with-assignments`**
```json
// Retorna TODOS los usuarios con sus almacenes
{
  "data": [
    {
      "userId": 1,
      "email": "user1@example.com",
      "role": "ALMACENISTA",
      "warehouseIds": [1, 2, 3]
    },
    {
      "userId": 2,
      "email": "user2@example.com",
      "role": "AUXILIAR",
      "warehouseIds": [4, 5]
    }
  ],
  "total": 2,
  "success": true
}
```

### **API Ideal: `/warehouses/users/assignments`**
```json
// Retorna usuarios con detalles completos de almacenes
{
  "data": [
    {
      "userId": 1,
      "email": "user1@example.com",
      "name": "Juan Pérez",
      "role": "ALMACENISTA",
      "warehouseIds": [1, 2, 3],
      "assignedWarehouses": [
        {
          "warehouseId": 1,
          "warehouseKey": "ALM001",
          "warehouseName": "Almacén Central",
          "assignedAt": "2026-02-01T10:30:00"
        },
        {
          "warehouseId": 2,
          "warehouseKey": "ALM002",
          "warehouseName": "Almacén Norte",
          "assignedAt": "2026-02-01T10:30:00"
        },
        {
          "warehouseId": 3,
          "warehouseKey": "ALM003",
          "warehouseName": "Almacén Sur",
          "assignedAt": "2026-02-01T10:30:00"
        }
      ]
    },
    {
      "userId": 2,
      "email": "user2@example.com",
      "name": "María López",
      "role": "AUXILIAR",
      "warehouseIds": [4, 5],
      "assignedWarehouses": [
        {
          "warehouseId": 4,
          "warehouseKey": "ALM004",
          "warehouseName": "Almacén Este",
          "assignedAt": "2026-02-05T14:20:00"
        },
        {
          "warehouseId": 5,
          "warehouseKey": "ALM005",
          "warehouseName": "Almacén Oeste",
          "assignedAt": "2026-02-05T14:20:00"
        }
      ]
    }
  ],
  "total": 2
}
```

---

## ✅ Recomendación Final

**Implementar la Opción 3 (API Flexible):**

### Backend
```java
// Controller
@GetMapping("/warehouses/users/assignments")
public ResponseEntity<?> getUserAssignments(
    @RequestParam(required = false) Integer userId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
) {
    if (userId != null) {
        // Obtener asignaciones de UN usuario específico
        return getUserAssignmentsByUserId(userId);
    } else {
        // Obtener asignaciones de TODOS los usuarios
        return getAllUsersWithAssignments(page, size);
    }
}
```

### Frontend - AsignacionIndividual.vue
```typescript
const loadUserWarehouses = async (userId: number) => {
  // Usa el parámetro userId
  const response = await axiosConfig.doGet(
    `/warehouses/users/assignments?userId=${userId}`
  );
  // El backend retorna un array con los almacenes del usuario
  assignedWarehouses.value = response.data[0]?.assignedWarehouses || [];
};
```

### Frontend - AsignacionPorLotes.vue
```typescript
const loadUserAssignments = async () => {
  // Sin parámetro userId, obtiene todos
  const response = await axiosConfig.doGet('/warehouses/users/assignments');
  // El backend retorna un array de todos los usuarios con almacenes
  userAssignments.value = response.data.data || [];
};
```

---

## 📅 Plan de Implementación

1. **Fase 1 (Corto Plazo):** Documentar bien las dos APIs actuales
2. **Fase 2 (Mediano Plazo):** Crear nueva API flexible en el backend
3. **Fase 3 (Largo Plazo):** Actualizar componentes para usar la nueva API
4. **Fase 4 (Mantenimiento):** Deprecar APIs antiguas

---

## 🔍 Estado Actual de los Componentes

| Componente | API Usada | Necesita Cambio |
|-----------|-----------|-----------------|
| AsignacionIndividual.vue | `/warehouses/users/{userId}` | ✅ Sí (usar nueva API con parámetro) |
| AsignacionPorLotes.vue | `/warehouses/users-with-assignments` | ✅ Sí (renombrar a `/warehouses/users/assignments`) |

---

## 📞 Preguntas para el Backend

1. ¿Existe ya la API `/warehouses/users-with-assignments`?
2. ¿Cuál es su estructura de respuesta exacta?
3. ¿Puedes crear una API flexible que acepte `?userId={id}` como parámetro opcional?
4. ¿Cuál es el rendimiento si obtienen todos los usuarios con sus almacenes?

---

## 📝 Fecha de Creación
2026-02-06

## ✨ Autor
Análisis Técnico de Inconsistencia de APIs


