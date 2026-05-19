# ✅ Guía de Validación y Testing - Solución 1 Implementada

## 🎯 Objetivo
Validar que la **Solución 1 (Reorden de Endpoints)** está funcionando correctamente en el backend y frontend.

---

## 🧪 Testing en Backend

### 1️⃣ Verificar Orden de Endpoints

**Archivo:** `MainWarehouseController.java`

```java
// ✅ DEBE VERSE ASÍ (Orden correcto)

// PRIMERO: Ruta específica (sin parámetros)
@GetMapping("/users-with-assignments")
public ResponseEntity<Map<String, Object>> getUsersWithAssignments() {
    // Devuelve todos los usuarios con almacenes asignados
}

// SEGUNDO: Ruta genérica (con parámetro)
@GetMapping("/users/{userId}")
public ResponseEntity<Map<String, Object>> getUserWarehouses(
    @PathVariable Long userId
) {
    // Devuelve almacenes de un usuario específico
}
```

### 2️⃣ Pruebas con cURL/Postman

#### ✅ Test 1: Obtener todos los usuarios con almacenes
```bash
curl -X GET "http://localhost:8080/api/sigmav2/warehouses/users-with-assignments" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "email": "user1@example.com",
      "name": "Juan Pérez",
      "role": "ALMACENISTA",
      "warehouseIds": [1, 2, 3]
    },
    {
      "userId": 2,
      "email": "user2@example.com",
      "name": "María López",
      "role": "AUXILIAR",
      "warehouseIds": [4, 5]
    }
  ],
  "total": 2,
  "message": "Usuarios con asignaciones cargados"
}
```

**❌ Respuesta de ERROR (antes del fix):**
```json
{
  "timestamp": "2026-02-06T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "NumberFormatException: For input string: \"users-with-assignments\"",
  "path": "/api/sigmav2/warehouses/users/users-with-assignments"
}
```

---

#### ✅ Test 2: Obtener almacenes de un usuario específico
```bash
curl -X GET "http://localhost:8080/api/sigmav2/warehouses/users/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Respuesta Esperada (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "warehouseId": 1,
      "warehouseKey": "ALM001",
      "warehouseName": "Almacén Central",
      "assignedAt": "2026-02-01T10:30:00Z"
    },
    {
      "userId": 1,
      "warehouseId": 2,
      "warehouseKey": "ALM002",
      "warehouseName": "Almacén Norte",
      "assignedAt": "2026-02-01T10:30:00Z"
    }
  ],
  "total": 2
}
```

---

#### ✅ Test 3: Crear asignación
```bash
curl -X POST "http://localhost:8080/api/sigmav2/warehouses/users/1/assign" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "warehouseIds": [1, 2, 3]
  }'
```

**Respuesta Esperada (200 OK):**
```json
{
  "success": true,
  "message": "Almacenes asignados correctamente",
  "data": {
    "userId": 1,
    "warehouseIds": [1, 2, 3],
    "assignedCount": 3
  }
}
```

---

## 🖥️ Testing en Frontend

### 1️⃣ Verificar Consola del Navegador

Abrir **DevTools** (F12) → **Console** en:
- `http://localhost:5173/admin/asignacion-lotes` (AsignacionPorLotes)

#### ✅ Logs Esperados (DESPUÉS del fix)

```javascript
// ✅ Sin error 400
[DEBUG] API Response /warehouses/users-with-assignments: {
  success: true,
  data: [
    {userId: 1, email: "user@example.com", warehouseIds: [1, 2, 3]},
    {userId: 2, email: "user2@example.com", warehouseIds: [4, 5]}
  ],
  total: 2
}

[DEBUG] Assignment data: [...]
[DEBUG] Warehouses disponibles: 58
[DEBUG] Buscando almacén 1: Almacén Central
[DEBUG] Buscando almacén 2: Almacén Norte
...

[DEBUG] Final User Assignments: [...]
```

#### ❌ Logs de ERROR (ANTES del fix)

```javascript
// ❌ Error 400 - NumberFormatException
Error: Request failed with status code 400

API Error Response: {
  timestamp: "2026-02-06T10:30:00Z",
  status: 400,
  error: "Bad Request",
  message: "NumberFormatException: For input string: \"users-with-assignments\""
}
```

---

### 2️⃣ Pruebas Funcionales en UI

#### ✅ Test 1: AsignacionPorLotes - Carga de tabla
```
1. Abrir: http://localhost:5173/admin/asignacion-lotes
2. Esperar a que cargue la tabla "Asignacion de Almacenes"
3. Debe mostrar:
   ✓ Lista de usuarios con avatares
   ✓ Rol de cada usuario
   ✓ Email de cada usuario
   ✓ Almacenes asignados a cada usuario
   ✓ Botones para eliminar asignaciones
```

**Resultado esperado:** ✅ Todo carga sin errores

---

#### ✅ Test 2: Seleccionar usuarios y almacenes
```
1. Ir a: http://localhost:5173/admin/asignacion-lotes
2. En panel izquierdo "SELECION DE VARIOS MULTIALMACENES":
   - Hacer click en 2-3 usuarios
   - Deben quedar marcados
3. En panel derecho "SELECCIONA VARIOS ALMACENES":
   - Hacer click en 2-3 almacenes
   - Deben quedar marcados
4. En sección "RESUMEN DE SELECCIONES":
   - Debe mostrar:
     * Usuarios seleccionados
     * Almacenes seleccionados
     * Total de asignaciones a crear
```

**Resultado esperado:** ✅ Todo funciona sin errores

---

#### ✅ Test 3: Confirmar asignación por lotes
```
1. Ir a: http://localhost:5173/admin/asignacion-lotes
2. Seleccionar 2 usuarios y 2 almacenes
3. Click en "Confirmar asignación de Almacenes"
4. Confirmar en modal
5. Esperar a que se complete
```

**Resultado esperado:** 
- ✅ Modal de éxito
- ✅ Tabla se actualiza con nuevas asignaciones

---

#### ✅ Test 4: AsignacionIndividual - Seleccionar usuario
```
1. Ir a: http://localhost:5173/admin/asignacion-individual
2. En "SELECCION DE USUARIO" dropdown:
   - Seleccionar un usuario
   - Debe cargar sus almacenes en la tabla
   - Mostrar almacenes ya asignados
```

**Resultado esperado:** ✅ Se cargan almacenes sin errores

---

## 📊 Checklist de Validación

### ✅ Backend
- [ ] Endpoints están en orden correcto
- [ ] GET `/warehouses/users-with-assignments` retorna 200 OK
- [ ] GET `/warehouses/users/{userId}` retorna 200 OK
- [ ] No hay errores 400 en logs
- [ ] Respuestas tienen estructura correcta

### ✅ Frontend
- [ ] Console no muestra errores 400
- [ ] AsignacionPorLotes carga tabla sin errores
- [ ] AsignacionIndividual carga almacenes de usuario
- [ ] Ambos componentes funcionan correctamente
- [ ] No hay error "NumberFormatException" en logs

### ✅ Funcionalidad
- [ ] Crear asignación individual funciona
- [ ] Crear asignación por lotes funciona
- [ ] Eliminar asignación funciona
- [ ] Seleccionar usuario actualiza tabla
- [ ] Filtros de búsqueda funcionan

---

## 🔍 Troubleshooting

### ❌ Problema: Error 400 persiste

**Causa:** Los endpoints no están en el orden correcto

**Solución:**
1. Verifica que `/users-with-assignments` esté ANTES de `/users/{userId}`
2. Revisa la clase `MainWarehouseController.java`
3. Asegúrate de que Spring se haya recargado
4. Reinicia la aplicación backend

```bash
# Reiniciar backend
mvn clean spring-boot:run
```

---

### ❌ Problema: Tabla en AsignacionPorLotes vacía

**Causa:** La API devuelve datos pero el componente no los mapea correctamente

**Solución:**
1. Abre DevTools (F12) → Console
2. Verifica el log: `[DEBUG] API Response /warehouses/users-with-assignments:`
3. Verifica que tenga estructura:
   ```json
   {
     "data": [ {...} ],
     "total": number,
     "success": true
   }
   ```
4. Si la estructura es diferente, actualiza el mapeo en el componente

---

### ❌ Problema: Almacenes no se cargan en tabla

**Causa:** Posiblemente `warehouses` no está cargado antes de mapear asignaciones

**Solución:**
Verifica en AsignacionPorLotes.vue que en `onMounted()`:
```typescript
onMounted(async () => {
  await loadUsers();
  await loadWarehouses();  // ← Debe ser ANTES de loadUserAssignments
  await loadUserAssignments();  // ← Después
});
```

---

## 📈 Métricas de Éxito

| Métrica | Antes | Después | Status |
|---------|-------|---------|--------|
| Error 400 en `/users-with-assignments` | ❌ Sí | ✅ No | ✅ |
| AsignacionPorLotes carga tabla | ❌ No | ✅ Sí | ✅ |
| AsignacionIndividual funciona | ✅ Sí | ✅ Sí | ✅ |
| Tests sin errores en console | ❌ No | ✅ Sí | ⏳ |

---

## 📝 Reporte de Validación

**Después de completar este testing, genera un reporte:**

```markdown
# Reporte de Validación - Solución 1

**Fecha:** 2026-02-06  
**Responsable:** [Tu nombre]  
**Estado:** [COMPLETADO/EN PROCESO/FALLIDO]

## Pruebas Backend
- [ ] GET /warehouses/users-with-assignments → 200 OK
- [ ] GET /warehouses/users/1 → 200 OK
- [ ] POST /warehouses/users/1/assign → 200 OK

## Pruebas Frontend
- [ ] AsignacionPorLotes: Tabla carga sin errores
- [ ] AsignacionIndividual: Almacenes cargan sin errores
- [ ] Console: Sin errores 400

## Estado General
✅ SOLUCIÓN 1 VALIDADA - LISTA PARA PRODUCCIÓN
```

---

## 📞 Soporte

Si encuentras problemas:

1. **Revisa los logs del backend:**
   ```bash
   tail -f logs/application.log
   ```

2. **Verifica la consola del navegador:** F12 → Console

3. **Comprueba el orden de endpoints** en el código fuente

4. **Reinicia ambos servicios** (backend + frontend)

---

## 📅 Fecha de Creación
**2026-02-06**

## ✨ Status
**DOCUMENTO DE VALIDACIÓN LISTO**


