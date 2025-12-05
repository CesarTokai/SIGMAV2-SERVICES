# Implementación de Validación de Existencias y Marbetes Cancelados

## 🎯 Resumen de Cambios

Se ha implementado un sistema completo para gestionar marbetes con y sin existencias, cumpliendo con las nuevas reglas de negocio:

1. **Validación de existencias al generar** marbetes
2. **Tabla separada** para marbetes cancelados por falta de existencias
3. **Modal con resumen** de generación (total, con existencias, sin existencias)
4. **Actualización de existencias** sin modificar folios
5. **Reactivación automática** de marbetes cuando se registran existencias

---

## 📋 Nueva Regla de Negocio

### Al Generar Marbetes

**ANTES:**
- Todos los marbetes se generaban con estado `GENERADO`
- No se validaban existencias

**AHORA:**
- ✅ Si **existencias > 0**: Marbete → tabla `labels` con estado `GENERADO`
- ❌ Si **existencias = 0**: Marbete → tabla `labels_cancelled` con estado `CANCELADO`
- 📊 Se retorna información detallada en modal

---

## 🗄️ Nueva Tabla: `labels_cancelled`

```sql
CREATE TABLE labels_cancelled (
    id_label_cancelled BIGINT AUTO_INCREMENT PRIMARY KEY,
    folio BIGINT NOT NULL UNIQUE,
    id_label_request BIGINT NOT NULL,
    id_period BIGINT NOT NULL,
    id_warehouse BIGINT NOT NULL,
    id_product BIGINT NOT NULL,
    existencias_al_cancelar INT DEFAULT 0,
    existencias_actuales INT DEFAULT 0,
    motivo_cancelacion VARCHAR(500),
    cancelado_at DATETIME NOT NULL,
    cancelado_by BIGINT NOT NULL,
    reactivado BOOLEAN DEFAULT FALSE,
    reactivado_at DATETIME,
    reactivado_by BIGINT,
    notas TEXT
);
```

### Ejecutar Script
```bash
mysql -u root -p sigma_db < crear_tabla_labels_cancelled.sql
```

---

## 🆕 Nuevos DTOs

### 1. `GenerateBatchResponseDTO`
Respuesta al generar marbetes con información detallada:

```json
{
  "totalGenerados": 10,
  "generadosConExistencias": 7,
  "generadosSinExistencias": 3,
  "primerFolio": 1001,
  "ultimoFolio": 1010,
  "mensaje": "Generación completada: 10 marbete(s) total. 7 con existencias (GENERADOS), 3 sin existencias (CANCELADOS)"
}
```

### 2. `LabelCancelledDTO`
Información de marbetes cancelados:

```json
{
  "idLabelCancelled": 1,
  "folio": 1005,
  "productId": 123,
  "claveProducto": "COM-5CLNQ",
  "nombreProducto": "CUBRE FLAMA M4L NIQUELADO",
  "warehouseId": 15,
  "claveAlmacen": "15",
  "nombreAlmacen": "Almacén 15",
  "periodId": 1,
  "existenciasAlCancelar": 0,
  "existenciasActuales": 158532,
  "motivoCancelacion": "Sin existencias al momento de generación",
  "canceladoAt": "2025-12-05T12:00:00",
  "reactivado": true,
  "reactivadoAt": "2025-12-05T14:00:00",
  "notas": "Existencias actualizadas manualmente"
}
```

### 3. `UpdateCancelledStockDTO`
Para actualizar existencias:

```json
{
  "folio": 1005,
  "existenciasActuales": 158532,
  "notas": "Existencias confirmadas por almacenista"
}
```

---

## 🔌 Nuevos Endpoints

### 1. Generar Marbetes (Modificado)
**POST** `/api/sigmav2/labels/generate`

**Request:**
```json
{
  "productId": 123,
  "warehouseId": 15,
  "periodId": 1,
  "labelsToGenerate": 10
}
```

**Response:** (Ahora retorna información detallada)
```json
{
  "totalGenerados": 10,
  "generadosConExistencias": 7,
  "generadosSinExistencias": 3,
  "primerFolio": 1001,
  "ultimoFolio": 1010,
  "mensaje": "Generación completada: 10 marbete(s) total. 7 con existencias (GENERADOS), 3 sin existencias (CANCELADOS)"
}
```

### 2. Consultar Marbetes Cancelados
**GET** `/api/sigmav2/labels/cancelled?periodId={id}&warehouseId={id}`

**Response:**
```json
[
  {
    "idLabelCancelled": 1,
    "folio": 1005,
    "productId": 123,
    "claveProducto": "COM-5CLNQ",
    "nombreProducto": "CUBRE FLAMA M4L NIQUELADO",
    "existenciasAlCancelar": 0,
    "existenciasActuales": 0,
    "reactivado": false,
    ...
  }
]
```

### 3. Actualizar Existencias de Marbete Cancelado
**PUT** `/api/sigmav2/labels/cancelled/update-stock`

**Request:**
```json
{
  "folio": 1005,
  "existenciasActuales": 158532,
  "notas": "Existencias confirmadas"
}
```

**Response:**
```json
{
  "idLabelCancelled": 1,
  "folio": 1005,
  "existenciasActuales": 158532,
  "reactivado": true,
  "reactivadoAt": "2025-12-05T14:00:00",
  ...
}
```

**Nota:** Si `existenciasActuales > 0`, el marbete se reactivará automáticamente y se creará en la tabla `labels` con estado `GENERADO`.

---

## 🔄 Flujo de Negocio Completo

### Escenario 1: Producto CON Existencias

```
1. Usuario solicita 10 folios → tabla label_request
2. Usuario genera marbetes
3. Sistema verifica existencias → 158,532 unidades ✅
4. Sistema crea 10 marbetes en tabla labels con estado GENERADO
5. Sistema retorna: {totalGenerados: 10, generadosConExistencias: 10, generadosSinExistencias: 0}
6. Frontend muestra modal: "✅ 10 marbetes generados exitosamente"
```

### Escenario 2: Producto SIN Existencias

```
1. Usuario solicita 10 folios → tabla label_request
2. Usuario genera marbetes
3. Sistema verifica existencias → 0 unidades ❌
4. Sistema crea 10 marbetes en tabla labels_cancelled
5. Sistema retorna: {totalGenerados: 10, generadosConExistencias: 0, generadosSinExistencias: 10}
6. Frontend muestra modal: "⚠️ 10 marbetes sin existencias (CANCELADOS)"
```

### Escenario 3: Actualización Posterior de Existencias

```
1. Marbetes en labels_cancelled (reactivado=false)
2. Usuario consulta marbetes cancelados → GET /api/sigmav2/labels/cancelled
3. Usuario actualiza existencias del folio 1005 → PUT /api/sigmav2/labels/cancelled/update-stock
4. Sistema actualiza existencias_actuales = 158532
5. Como existencias > 0, sistema:
   a. Marca reactivado=true en labels_cancelled
   b. Crea marbete en labels con estado GENERADO
6. Marbete ahora disponible para impresión
```

---

## 📱 Integración con Frontend

### Modal de Generación

```javascript
// Llamada al endpoint
const response = await fetch('/api/sigmav2/labels/generate', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    productId: 123,
    warehouseId: 15,
    periodId: 1,
    labelsToGenerate: 10
  })
});

const result = await response.json();

// Mostrar modal con resultados
if (result.generadosSinExistencias > 0) {
  showModal({
    title: '⚠️ Generación con Advertencias',
    message: `
      Total generados: ${result.totalGenerados}
      ✅ Con existencias: ${result.generadosConExistencias}
      ❌ Sin existencias (cancelados): ${result.generadosSinExistencias}

      Los marbetes cancelados pueden consultarse en la sección "Marbetes Cancelados".
    `,
    type: 'warning'
  });
} else {
  showModal({
    title: '✅ Generación Exitosa',
    message: `Se generaron ${result.totalGenerados} marbetes correctamente.`,
    type: 'success'
  });
}
```

### Tabla de Marbetes Cancelados

```javascript
// Consultar marbetes cancelados
const response = await fetch(
  `/api/sigmav2/labels/cancelled?periodId=${periodId}&warehouseId=${warehouseId}`,
  {
    headers: { 'Authorization': `Bearer ${token}` }
  }
);

const cancelledLabels = await response.json();

// Renderizar tabla
<table>
  <thead>
    <tr>
      <th>Folio</th>
      <th>Producto</th>
      <th>Existencias Actuales</th>
      <th>Acciones</th>
    </tr>
  </thead>
  <tbody>
    {cancelledLabels.map(label => (
      <tr>
        <td>{label.folio}</td>
        <td>{label.claveProducto} - {label.nombreProducto}</td>
        <td>
          <input
            type="number"
            value={label.existenciasActuales}
            onChange={(e) => handleUpdateStock(label.folio, e.target.value)}
          />
        </td>
        <td>
          <button onClick={() => updateStock(label.folio)}>
            Actualizar
          </button>
        </td>
      </tr>
    ))}
  </tbody>
</table>
```

### Actualizar Existencias

```javascript
async function updateStock(folio, existencias, notas) {
  const response = await fetch('/api/sigmav2/labels/cancelled/update-stock', {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      folio: folio,
      existenciasActuales: existencias,
      notas: notas
    })
  });

  const updated = await response.json();

  if (updated.reactivado) {
    showNotification('✅ Marbete reactivado y disponible para impresión');
    refreshList();
  } else {
    showNotification('✅ Existencias actualizadas');
  }
}
```

---

## 🔐 Permisos de Endpoints

| Endpoint | Roles Permitidos |
|----------|------------------|
| POST `/generate` | ADMINISTRADOR, AUXILIAR, ALMACENISTA |
| GET `/cancelled` | ADMINISTRADOR, AUXILIAR, ALMACENISTA |
| PUT `/cancelled/update-stock` | ADMINISTRADOR, AUXILIAR, ALMACENISTA |

---

## 📊 Consultas SQL Útiles

### Ver marbetes cancelados
```sql
SELECT
    lc.folio,
    p.cve_art,
    p.descr,
    lc.existencias_al_cancelar,
    lc.existencias_actuales,
    lc.reactivado,
    lc.cancelado_at
FROM labels_cancelled lc
JOIN products p ON lc.id_product = p.id_product
WHERE lc.id_period = 1
  AND lc.id_warehouse = 15
  AND lc.reactivado = FALSE
ORDER BY lc.folio;
```

### Ver marbetes reactivados
```sql
SELECT
    lc.folio,
    p.cve_art,
    lc.existencias_al_cancelar,
    lc.existencias_actuales,
    lc.reactivado_at
FROM labels_cancelled lc
JOIN products p ON lc.id_product = p.id_product
WHERE lc.reactivado = TRUE
ORDER BY lc.reactivado_at DESC;
```

### Estadísticas de generación
```sql
SELECT
    COUNT(*) as total_folios_solicitados,
    SUM(CASE WHEN l.id_label IS NOT NULL THEN 1 ELSE 0 END) as con_existencias,
    SUM(CASE WHEN lc.id_label_cancelled IS NOT NULL THEN 1 ELSE 0 END) as sin_existencias,
    SUM(CASE WHEN lc.reactivado = TRUE THEN 1 ELSE 0 END) as reactivados
FROM label_request lr
LEFT JOIN labels l ON l.id_label_request = lr.id_label_request
LEFT JOIN labels_cancelled lc ON lc.id_label_request = lr.id_label_request
WHERE lr.id_period = 1 AND lr.id_warehouse = 15;
```

---

## ✅ Testing

### Prueba 1: Generar con Existencias
```bash
curl -X POST "http://localhost:8080/api/sigmav2/labels/generate" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 123,
    "warehouseId": 15,
    "periodId": 1,
    "labelsToGenerate": 10
  }'
```

**Resultado Esperado:**
- Status: 200 OK
- JSON con `generadosConExistencias: 10`

### Prueba 2: Generar sin Existencias
```bash
curl -X POST "http://localhost:8080/api/sigmav2/labels/generate" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 999,
    "warehouseId": 15,
    "periodId": 1,
    "labelsToGenerate": 5
  }'
```

**Resultado Esperado:**
- Status: 200 OK
- JSON con `generadosSinExistencias: 5`
- Registros en `labels_cancelled`

### Prueba 3: Consultar Cancelados
```bash
curl -X GET "http://localhost:8080/api/sigmav2/labels/cancelled?periodId=1&warehouseId=15" \
  -H "Authorization: Bearer ${TOKEN}"
```

**Resultado Esperado:**
- Status: 200 OK
- Array con marbetes cancelados

### Prueba 4: Actualizar y Reactivar
```bash
curl -X PUT "http://localhost:8080/api/sigmav2/labels/cancelled/update-stock" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "folio": 1005,
    "existenciasActuales": 100,
    "notas": "Existencias confirmadas"
  }'
```

**Resultado Esperado:**
- Status: 200 OK
- JSON con `reactivado: true`
- Registro en `labels` con estado `GENERADO`

---

## 📝 Archivos Creados/Modificados

### Nuevos Archivos
1. `GenerateBatchResponseDTO.java` - DTO de respuesta de generación
2. `LabelCancelled.java` - Entidad para marbetes cancelados
3. `LabelCancelledDTO.java` - DTO para consultas de cancelados
4. `UpdateCancelledStockDTO.java` - DTO para actualizar existencias
5. `JpaLabelCancelledRepository.java` - Repositorio JPA
6. `crear_tabla_labels_cancelled.sql` - Script SQL de tabla
7. `IMPLEMENTACION-MARBETES-SIN-EXISTENCIAS.md` - Este documento

### Archivos Modificados
1. `LabelService.java` - Interfaz con nuevos métodos
2. `LabelServiceImpl.java` - Implementación con validación de existencias
3. `LabelsPersistenceAdapter.java` - Métodos para marbetes cancelados
4. `LabelsController.java` - Nuevos endpoints

---

## 🚀 Despliegue

### Paso 1: Crear la Tabla
```bash
mysql -u root -p sigma_db < crear_tabla_labels_cancelled.sql
```

### Paso 2: Compilar y Empaquetar
```bash
./mvnw.cmd clean package -DskipTests
```

### Paso 3: Reiniciar Aplicación
```bash
java -jar target/SIGMAV2-0.0.1-SNAPSHOT.jar
```

### Paso 4: Verificar
```bash
# Verificar que la tabla existe
mysql -u root -p sigma_db -e "SHOW TABLES LIKE 'labels_cancelled';"

# Probar endpoint de generación
curl -X POST http://localhost:8080/api/sigmav2/labels/generate \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"productId":123,"warehouseId":15,"periodId":1,"labelsToGenerate":1}'
```

---

## 🎓 Beneficios de esta Implementación

1. ✅ **Separación clara** entre marbetes válidos y cancelados
2. ✅ **Trazabilidad completa** de existencias y reactivaciones
3. ✅ **No se modifican folios** al actualizar existencias
4. ✅ **Reactivación automática** cuando hay existencias
5. ✅ **Modal informativo** con detalles de generación
6. ✅ **Auditoría completa** (quién, cuándo, por qué)
7. ✅ **Consultas optimizadas** con índices apropiados
8. ✅ **Escalable** para futuras mejoras

---

## 🔮 Futuras Mejoras Sugeridas

1. **Dashboard** de marbetes cancelados vs activos
2. **Notificaciones** cuando marbetes son reactivados
3. **Carga masiva** de existencias desde Excel
4. **Historial** de cambios de existencias
5. **Reportes** de eficiencia de generación

