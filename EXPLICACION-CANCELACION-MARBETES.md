# 📋 Explicación Completa: ¿Qué Pasa Cuando Cancelas un Marbete?

## 🎯 Respuesta Rápida

Cuando cancelas un marbete, **NO se elimina**, sino que:
1. ✅ Se marca como `CANCELADO` en la tabla `labels`
2. ✅ Se crea un registro completo en la tabla `labels_cancelled` (historial)
3. ✅ Se guarda información de auditoría (quién, cuándo, por qué)
4. ✅ El folio queda reservado pero inutilizable

---

## 🔍 Proceso Detallado de Cancelación

### **Paso 1: Se Recibe la Solicitud**

```bash
POST /api/sigmav2/labels/cancel
Body: {
  "folio": 10001,
  "periodId": 1,
  "warehouseId": 1,
  "motivoCancelacion": "Error en impresión del código de barras"
}
```

### **Paso 2: Validaciones**

El sistema verifica:
1. ✅ El marbete existe
2. ✅ Pertenece al periodo y almacén especificados
3. ✅ NO está ya cancelado (evita doble cancelación)
4. ✅ El usuario tiene acceso al almacén

### **Paso 3: Actualización en Tabla `labels`**

```sql
-- El registro NO se elimina, solo cambia su estado
UPDATE labels
SET estado = 'CANCELADO'
WHERE folio = 10001;
```

**Antes de cancelar**:
```
folio | estado   | producto_id | almacen_id | periodo_id
------|----------|-------------|------------|------------
10001 | IMPRESO  | 123         | 1          | 1
```

**Después de cancelar**:
```
folio | estado     | producto_id | almacen_id | periodo_id
------|------------|-------------|------------|------------
10001 | CANCELADO  | 123         | 1          | 1
```

### **Paso 4: Creación de Registro en `labels_cancelled`**

Se crea un **nuevo registro de auditoría** con toda la información:

```sql
INSERT INTO labels_cancelled (
    folio,
    id_label_request,
    id_period,
    id_warehouse,
    id_product,
    existencias_al_cancelar,
    existencias_actuales,
    motivo_cancelacion,
    cancelado_at,
    cancelado_by,
    reactivado,
    reactivado_at,
    reactivado_by,
    notas
) VALUES (
    10001,                                        -- folio cancelado
    5,                                            -- ID de la solicitud original
    1,                                            -- periodo
    1,                                            -- almacén
    123,                                          -- producto
    0,                                            -- existencias al momento de cancelar
    0,                                            -- existencias actuales
    'Error en impresión del código de barras',   -- motivo
    '2025-12-09 11:45:00',                       -- fecha/hora de cancelación
    7,                                            -- usuario que canceló
    false,                                        -- no reactivado
    NULL,                                         -- fecha de reactivación (null)
    NULL,                                         -- usuario que reactivó (null)
    NULL                                          -- notas adicionales (null)
);
```

---

## 📊 Estructura de las Tablas

### **Tabla 1: `labels` (Marbetes Activos)**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| folio | BIGINT (PK) | Número único del marbete |
| id_label_request | BIGINT | Referencia a la solicitud |
| id_period | BIGINT | Periodo del inventario |
| id_warehouse | BIGINT | Almacén |
| id_product | BIGINT | Producto |
| **estado** | VARCHAR | **GENERADO, IMPRESO, CANCELADO** |
| impreso_at | TIMESTAMP | Cuándo se imprimió |
| created_by | BIGINT | Quién lo creó |
| created_at | TIMESTAMP | Cuándo se creó |

### **Tabla 2: `labels_cancelled` (Historial de Cancelaciones)**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id_label_cancelled | BIGINT (PK) | ID autoincremental |
| folio | BIGINT (UNIQUE) | Folio del marbete cancelado |
| id_label_request | BIGINT | Solicitud original |
| id_period | BIGINT | Periodo |
| id_warehouse | BIGINT | Almacén |
| id_product | BIGINT | Producto |
| existencias_al_cancelar | INT | Existencias al momento de cancelar |
| existencias_actuales | INT | Existencias actuales (puede cambiar) |
| motivo_cancelacion | TEXT | Razón de la cancelación |
| **cancelado_at** | TIMESTAMP | **Fecha/hora de cancelación** |
| **cancelado_by** | BIGINT | **Usuario que canceló** |
| reactivado | BOOLEAN | ¿Fue reactivado? |
| reactivado_at | TIMESTAMP | Cuándo se reactivó (si aplica) |
| reactivado_by | BIGINT | Quién lo reactivó (si aplica) |
| notas | TEXT | Notas adicionales |

---

## 🔄 Estados del Marbete

```
┌─────────────┐
│  GENERADO   │ ← Estado inicial al crear el marbete
└──────┬──────┘
       │
       │ (Imprimir)
       ↓
┌─────────────┐
│   IMPRESO   │ ← Marbete listo para conteo
└──────┬──────┘
       │
       │ (Cancelar)
       ↓
┌─────────────┐
│  CANCELADO  │ ← NO se puede usar para conteo
└─────────────┘   (Queda registrado en ambas tablas)
```

---

## ❓ Preguntas Frecuentes

### **1. ¿Se elimina el registro de la tabla `labels`?**
❌ **NO**. El registro permanece en la tabla `labels`, solo cambia su `estado` a `CANCELADO`.

### **2. ¿Puedo volver a usar el mismo folio?**
❌ **NO**. Los folios son únicos y no se reutilizan. Una vez cancelado, ese folio queda reservado pero inutilizable.

### **3. ¿Dónde puedo ver los marbetes cancelados?**
✅ En **2 lugares**:
- Tabla `labels` con `estado = 'CANCELADO'`
- Tabla `labels_cancelled` (historial completo con auditoría)

### **4. ¿Se pierden los conteos si cancelo un marbete?**
✅ **NO**. Los conteos permanecen en la tabla `label_count_events`. Sin embargo, el marbete cancelado NO se usa en reportes de inventario físico.

### **5. ¿Puedo reactivar un marbete cancelado?**
✅ **SÍ**, existe funcionalidad para reactivar marbetes cancelados si se actualizan las existencias:

```
PUT /api/sigmav2/labels/cancelled/update-stock
Body: {
  "folio": 10001,
  "existenciasActuales": 50,
  "notas": "Se recibieron existencias del proveedor"
}
```

Esto marca `reactivado = true` y crea un nuevo registro en `labels` con estado `GENERADO`.

### **6. ¿Por qué se guardan en dos tablas?**
Por **auditoría y trazabilidad**:
- `labels`: Estado actual del marbete (para operaciones)
- `labels_cancelled`: Historial completo (para reportes y auditorías)

---

## 📈 Reportes que Incluyen Marbetes Cancelados

### **Reporte de Marbetes Cancelados**
```
POST /api/sigmav2/labels/reports/cancelled
Body: { "periodId": 1, "warehouseId": 1 }
```

Muestra:
- Folio cancelado
- Producto
- Motivo de cancelación
- Fecha y hora
- Usuario que canceló
- Conteos registrados (si los había)

### **Consultar Marbetes Cancelados**
```
GET /api/sigmav2/labels/cancelled?periodId=1&warehouseId=1
```

Devuelve lista completa con:
- Información del marbete
- Existencias al momento de cancelar
- Existencias actuales
- Estado de reactivación

---

## 🎯 Ejemplo Práctico

### **Escenario**: Cancelas el marbete folio 10050

**1. Antes de cancelar**:

```sql
-- En tabla labels
SELECT * FROM labels WHERE folio = 10050;
```
```
folio | estado  | producto_id | almacen_id
------|---------|-------------|------------
10050 | IMPRESO | 456         | 1
```

```sql
-- En tabla labels_cancelled
SELECT * FROM labels_cancelled WHERE folio = 10050;
```
```
(Sin registros)
```

**2. Ejecutas la cancelación**:

```bash
POST /api/sigmav2/labels/cancel
Body: {
  "folio": 10050,
  "periodId": 1,
  "warehouseId": 1,
  "motivoCancelacion": "Producto descontinuado"
}
```

**3. Después de cancelar**:

```sql
-- En tabla labels (ACTUALIZADO, NO ELIMINADO)
SELECT * FROM labels WHERE folio = 10050;
```
```
folio | estado     | producto_id | almacen_id
------|------------|-------------|------------
10050 | CANCELADO  | 456         | 1
```

```sql
-- En tabla labels_cancelled (NUEVO REGISTRO CREADO)
SELECT * FROM labels_cancelled WHERE folio = 10050;
```
```
id  | folio | producto_id | motivo                    | cancelado_at        | cancelado_by
----|-------|-------------|---------------------------|---------------------|-------------
123 | 10050 | 456         | Producto descontinuado    | 2025-12-09 11:45:00 | 7
```

---

## ✅ Resumen

| Aspecto | Comportamiento |
|---------|----------------|
| **¿Se elimina el registro?** | ❌ NO, se marca como CANCELADO |
| **¿Dónde queda guardado?** | ✅ En `labels` (cancelado) y `labels_cancelled` (auditoría) |
| **¿Se puede reutilizar el folio?** | ❌ NO, los folios son únicos |
| **¿Se pierden los conteos?** | ❌ NO, quedan en `label_count_events` |
| **¿Se puede reactivar?** | ✅ SÍ, con API de actualización de existencias |
| **¿Aparece en reportes?** | ✅ SÍ, en el reporte específico de cancelados |
| **¿Afecta el inventario físico?** | ✅ NO se cuenta en inventario físico final |

---

## 🔐 Auditoría Completa

El sistema guarda para cada cancelación:
- ✅ **Quién** canceló (usuario)
- ✅ **Cuándo** canceló (fecha/hora exacta)
- ✅ **Por qué** canceló (motivo)
- ✅ **Qué** canceló (folio, producto, almacén)
- ✅ **Cuánto** había (existencias al momento)
- ✅ **Estado actual** (reactivado o no)

**Esto permite trazabilidad completa y cumplimiento de auditorías** ✅

