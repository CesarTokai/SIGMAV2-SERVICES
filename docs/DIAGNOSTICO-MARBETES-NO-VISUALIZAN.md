# Diagnóstico: Marbetes Generados No Se Visualizan

## Problema Reportado
Cuando se generan marbetes, no se visualizan en la interfaz de usuario.

## Cambios Implementados para Diagnóstico

### 1. Logs Detallados Agregados

Se agregaron logs extensivos en los siguientes métodos:

#### `LabelServiceImpl.generateBatch()`
- Log al inicio con todos los parámetros recibidos
- Log después de validar acceso al almacén
- Log al encontrar la solicitud existente
- Log del cálculo de marbetes a generar
- Log del rango de folios asignado
- Log antes y después de guardar marbetes
- Log al registrar el lote de generación
- Log al actualizar la solicitud
- Log al finalizar exitosamente

#### `LabelsPersistenceAdapter.saveLabelsBatch()`
- Log de todos los parámetros recibidos
- Log del número de objetos Label creados en memoria
- Log después de guardar en BD
- Log de verificación con conteo total en BD

### 2. Nuevo Endpoint de Diagnóstico

Se agregó el endpoint `GET /api/labels/debug/count` que permite:
- Verificar cuántos marbetes existen en la BD para un periodo y almacén
- Acceso restringido a roles: ADMINISTRADOR, AUXILIAR, ALMACENISTA
- Retorna información del usuario que hace la consulta

#### Uso:
```
GET /api/labels/debug/count?periodId=1&warehouseId=1
Authorization: Bearer <token>
```

#### Respuesta:
```json
{
  "periodId": 1,
  "warehouseId": 1,
  "totalLabels": 50,
  "userId": 1,
  "userRole": "ADMINISTRADOR"
}
```

### 3. Método de Servicio Agregado

Se agregó `countLabelsByPeriodAndWarehouse()` en `LabelService` y `LabelServiceImpl`.

## Herramientas de Diagnóstico Creadas

### 1. Script PowerShell: `test-debug-labels.ps1`
Script interactivo que:
- Consulta el endpoint de debug para contar marbetes
- Consulta el endpoint de summary para ver la lista
- Compara resultados
- Muestra advertencias si no hay marbetes

**Uso:**
```powershell
.\test-debug-labels.ps1
```

### 2. Script SQL: `diagnostico_marbetes.sql`
Consultas SQL para verificar directamente en la BD:
1. Solicitudes de folios
2. Marbetes generados
3. Lotes de generación
4. Secuencia de folios
5. Inconsistencias
6. Conteo por periodo y almacén
7. Últimos 20 marbetes
8. Resumen ejecutivo

## Posibles Causas del Problema

### 1. **Transacción No Comprometida**
- ❓ Los marbetes se crean pero la transacción no se compromete
- ✅ Verificación: Los logs mostrarán si `saveLabelsBatch` se completa
- 🔧 Solución: Ya tiene `@Transactional` pero podría necesitar flush

### 2. **Filtros de Consulta Incorrectos**
- ❓ El frontend está consultando con periodo/almacén incorrecto
- ✅ Verificación: Comparar logs del generateBatch vs los parámetros del summary
- 🔧 Solución: Verificar que el frontend use los mismos IDs

### 3. **Cache del Frontend**
- ❓ El frontend no está refrescando la lista después de generar
- ✅ Verificación: Hacer F5 manual o consultar directamente el endpoint
- 🔧 Solución: Agregar refresco automático después de generar

### 4. **Paginación**
- ❓ Los marbetes están en una página diferente
- ✅ Verificación: El endpoint debug mostrará el total sin paginación
- 🔧 Solución: Verificar que page=0 y size sea suficiente

### 5. **Permisos de Almacén**
- ❓ El usuario no tiene acceso al almacén donde generó
- ✅ Verificación: Los logs mostrarán si pasa la validación de acceso
- 🔧 Solución: Verificar asignaciones de almacén del usuario

### 6. **Estado de Marbetes**
- ❓ Los marbetes se guardan con un estado que el frontend filtra
- ✅ Verificación: SQL mostrará el estado real de los marbetes
- 🔧 Solución: Verificar que se guarden como 'GENERADO'

### 7. **Problema de Join en la Consulta**
- ❓ La consulta del summary tiene un problema con los joins
- ✅ Verificación: Logs muestran cuántos labels se recuperan
- 🔧 Solución: Revisar `findByPeriodIdAndWarehouseId`

## Pasos de Diagnóstico Recomendados

### Paso 1: Verificar Logs del Backend
1. Reiniciar la aplicación
2. Generar marbetes para un producto
3. Revisar los logs en la consola buscando:
   - `=== INICIO generateBatch ===`
   - `=== saveLabelsBatch INICIO ===`
   - El conteo de verificación al final

### Paso 2: Usar Endpoint de Debug
1. Ejecutar el script `test-debug-labels.ps1`
2. Ingresar el token JWT
3. Ingresar periodo y almacén
4. Ver si reporta marbetes

### Paso 3: Verificar en Base de Datos
1. Ejecutar el script `diagnostico_marbetes.sql`
2. Verificar el resumen ejecutivo
3. Revisar si hay inconsistencias

### Paso 4: Comparar Parámetros
1. Anotar periodId y warehouseId usados al generar
2. Verificar que el frontend consulte con los mismos IDs
3. Revisar las asignaciones de almacén del usuario

## Próximos Pasos Si No Se Resuelve

Si después del diagnóstico los marbetes existen en BD pero no se visualizan:

1. **Revisar el Frontend:**
   - Verificar la llamada al endpoint `/api/labels/summary`
   - Verificar el manejo de la respuesta
   - Verificar el renderizado de la tabla

2. **Agregar Flush Explícito:**
   ```java
   @PersistenceContext
   private EntityManager entityManager;

   // En saveLabelsBatch:
   jpaLabelRepository.saveAll(labels);
   entityManager.flush();
   ```

3. **Verificar Aislamiento de Transacción:**
   Cambiar a `@Transactional(isolation = Isolation.READ_COMMITTED)`

4. **Agregar Endpoint para Ver Marbetes de un Producto:**
   ```java
   GET /api/labels/by-product?productId=X&periodId=Y&warehouseId=Z
   ```

## Archivos Modificados

1. `LabelServiceImpl.java` - Logs detallados + método de conteo
2. `LabelsPersistenceAdapter.java` - Logs detallados + anotación @Slf4j
3. `LabelsController.java` - Endpoint de debug
4. `LabelService.java` - Interfaz del método de conteo

## Archivos Creados

1. `test-debug-labels.ps1` - Script de diagnóstico PowerShell
2. `diagnostico_marbetes.sql` - Queries de diagnóstico SQL
3. `DIAGNOSTICO-MARBETES-NO-VISUALIZAN.md` - Este documento

## Conclusión

Los cambios implementados proporcionan visibilidad completa del proceso de generación de marbetes. Los logs y herramientas de diagnóstico permitirán identificar exactamente dónde está el problema:
- Si los marbetes se están guardando en BD
- Si la consulta los está recuperando
- Si hay un problema de filtrado o presentación

**La aplicación sigue siendo completamente funcional** - solo se agregaron logs y herramientas de diagnóstico sin modificar la lógica de negocio existente.

