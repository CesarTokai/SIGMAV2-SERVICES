# Guía Rápida: Diagnóstico de Marbetes No Visualizados

## 🚀 Inicio Rápido

### Opción 1: Usar PowerShell (Recomendado)

```powershell
# 1. Navegar al directorio del proyecto
cd C:\Users\cesarg\Desktop\SIGMAV2\SIGMAV2

# 2. Ejecutar script de diagnóstico
.\test-debug-labels.ps1

# 3. Seguir las instrucciones en pantalla
#    - Ingresar token JWT
#    - Ingresar ID de periodo
#    - Ingresar ID de almacén
```

### Opción 2: Consulta Directa con curl

```bash
# Reemplaza TOKEN, PERIOD_ID y WAREHOUSE_ID con tus valores
curl -X GET "http://localhost:8080/api/labels/debug/count?periodId=PERIOD_ID&warehouseId=WAREHOUSE_ID" \
  -H "Authorization: Bearer TOKEN"
```

### Opción 3: Verificación en Base de Datos

```sql
-- Ejecutar en MySQL/MariaDB
-- Ver archivo: diagnostico_marbetes.sql

-- Consulta rápida:
SELECT
    l.id_period,
    l.id_warehouse,
    COUNT(*) as total_marbetes
FROM labels l
GROUP BY l.id_period, l.id_warehouse;
```

## 📊 Interpretación de Resultados

### ✅ Caso 1: Marbetes Existen (totalLabels > 0)
**Problema:** Frontend no actualiza o consulta incorrectamente
**Solución:**
1. Verificar que el frontend use los mismos periodId y warehouseId
2. Hacer refresh (F5) en el navegador
3. Verificar consola del navegador por errores JavaScript
4. Revisar que el endpoint `/api/labels/summary` funcione correctamente

### ❌ Caso 2: No Hay Marbetes (totalLabels = 0)
**Problema:** Los marbetes no se están generando
**Solución:**
1. Verificar los logs del backend al momento de generar
2. Buscar en logs: `=== INICIO generateBatch ===`
3. Verificar que no haya excepciones
4. Revisar la tabla `label_request` en BD

### ⚠️ Caso 3: Error 403 Forbidden
**Problema:** Usuario sin permisos
**Solución:**
1. Verificar que el usuario tenga rol: ADMINISTRADOR, AUXILIAR o ALMACENISTA
2. Verificar que el token sea válido
3. Verificar asignación de almacén

## 🔍 Checklist de Verificación

- [ ] ¿El backend está corriendo? (puerto 8080)
- [ ] ¿Has solicitado folios para el producto?
- [ ] ¿Has ejecutado "Generar marbetes"?
- [ ] ¿El token JWT es válido?
- [ ] ¿El usuario tiene acceso al almacén?
- [ ] ¿Los IDs de periodo y almacén son correctos?
- [ ] ¿Hay errores en los logs del backend?
- [ ] ¿El frontend está consultando el endpoint correcto?

## 📝 Logs a Revisar

Cuando generas marbetes, deberías ver en los logs:

```
INFO  --- generateBatch INICIO ===
INFO  --- DTO recibido: productId=X, warehouseId=Y, periodId=Z, labelsToGenerate=N
INFO  --- Acceso al almacén validado correctamente
INFO  --- Solicitud encontrada: id=..., requestedLabels=..., foliosGenerados=...
INFO  --- Se generarán N marbetes
INFO  --- Rango de folios asignado: X a Y
INFO  --- Guardando N marbetes en la base de datos...
INFO  --- saveLabelsBatch INICIO ===
INFO  --- Creados N objetos Label en memoria
INFO  --- Guardados N marbetes en la base de datos exitosamente
INFO  --- Verificación: Total de marbetes en BD para periodId=..., warehouseId=...: N
INFO  --- Marbetes guardados exitosamente
INFO  --- FIN generateBatch EXITOSO ===
```

Si NO ves estos logs, el método `generateBatch` no se está ejecutando.

## 🛠️ Soluciones Comunes

### Problema: Frontend no refresca automáticamente
```javascript
// Agregar en el frontend después de generar marbetes:
await generarMarbetes(...);
await obtenerResumen(); // Refrescar la lista
```

### Problema: Usuario en almacén incorrecto
```sql
-- Verificar asignación del usuario
SELECT * FROM user_warehouse_assignment WHERE user_id = YOUR_USER_ID;
```

### Problema: Periodo o almacén incorrecto
```sql
-- Ver todos los periodos
SELECT * FROM periods ORDER BY id DESC;

-- Ver todos los almacenes
SELECT * FROM warehouse;
```

## 📞 Si Nada Funciona

1. Detén el backend
2. Reinicia el backend y observa los logs desde el inicio
3. Ejecuta el script de diagnóstico INMEDIATAMENTE después de generar
4. Captura los logs completos y la respuesta del script
5. Ejecuta las queries SQL de diagnóstico
6. Comparte los resultados para análisis detallado

## 🎯 Comandos de Emergencia

```sql
-- Ver TODOS los marbetes
SELECT * FROM labels ORDER BY created_at DESC LIMIT 50;

-- Ver TODAS las solicitudes
SELECT * FROM label_request ORDER BY created_at DESC;

-- Ver TODOS los lotes de generación
SELECT * FROM label_generation_batch ORDER BY generado_at DESC;

-- Contar por periodo
SELECT id_period, id_warehouse, COUNT(*)
FROM labels
GROUP BY id_period, id_warehouse;
```

## ✨ Tip Final

Si después de todo el diagnóstico confirmas que:
- ✅ Los marbetes existen en BD
- ✅ El endpoint de debug los reporta
- ❌ Pero el frontend no los muestra

Entonces el problema está 100% en el frontend, no en el backend.
Revisa:
- La llamada AJAX/fetch al endpoint
- El manejo de la respuesta
- El renderizado de la tabla
- Filtros aplicados en el frontend

