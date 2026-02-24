# 🔧 SOLUCIÓN: Error "The given id must not be null" en Cancelación de Marbetes

## ❌ Problema Identificado

**Error reportado:**
```
org.springframework.dao.InvalidDataAccessApiUsageException: The given id must not be null
```

**En el log:**
```
2026-02-24T10:01:40.848-06:00  INFO ... Cancelando marbete folio 151 por usuario 7 con rol ADMINISTRADOR
2026-02-24T10:01:40.849-06:00 ERROR ... The given id must not be null
```

## 🔍 Causa Raíz

El marbete **folio 151** tiene `id_label_request = NULL` en la base de datos.

Cuando el código intenta hacer:
```java
LabelRequest labelRequest = labelRequestRepository.findById(label.getLabelRequestId())
```

Pasa `null` a `findById(null)`, lo cual causa que Hibernate lance la excepción.

### ¿Por qué ocurre esto?

Hay marbetes que fueron creados sin pasar por el flujo correcto de solicitud de folios, lo que resulta en registros sin una referencia válida a `label_requests`.

---

## ✅ Solución Implementada

Se agregó una **validación preventiva** en el método `cancelLabel()` de `LabelServiceImpl.java` (líneas 1304-1310):

```java
// Validar primero que labelRequestId no sea nulo
if (label.getLabelRequestId() == null) {
    log.warn("⚠️ El marbete {} tiene id_label_request = NULL. Esto indica un problema de integridad de datos.", 
        dto.getFolio());
    throw new InvalidLabelStateException(
        "El marbete no tiene una solicitud de folios válida asociada. " +
        "Este es un problema de integridad de datos que debe ser revisado por administración."
    );
}
```

### Beneficios

✅ **Evita el error de Hibernate** → Se valida ANTES de pasar null a findById()
✅ **Mensaje claro** → Le indica al usuario que hay un problema de integridad de datos
✅ **Logging** → Registra el evento para auditoría y debugging
✅ **Mantenibilidad** → El código es más robusto ante datos corruptos

---

## 📋 Flujo de Cancelación (Controlador)

```
POST /api/sigmav2/labels/cancel
├─ LabelsController.cancelLabel()
│  ├─ Extrae userId y userRole del token JWT
│  ├─ Log: "Cancelando marbete folio X por usuario Y con rol Z"
│  └─ Llama a: labelService.cancelLabel(dto, userId, userRole)
│
└─ LabelServiceImpl.cancelLabel()
   ├─ ✓ Valida acceso al almacén
   ├─ ✓ Busca el marbete por folio
   ├─ ✓ Valida que pertenece al periodo/almacén
   ├─ ✓ Valida que NO esté ya cancelado
   ├─ ✓ [NUEVA] Valida que labelRequestId NO sea NULL ⭐
   ├─ ✓ Obtiene LabelRequest y valida requestedLabels > 0
   ├─ ✓ Cambia estado a CANCELADO
   ├─ ✓ Registra en tabla labels_cancelled
   └─ ✓ Obtiene y guarda existencias
```

---

## 🔧 Cambios Realizados

**Archivo:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelServiceImpl.java`

**Líneas:** 1304-1310

**Tipo de cambio:** Agregación de validación preventiva

---

## 📊 Tabla Afectada

```sql
-- Tabla de marbetes
CREATE TABLE labels (
    folio BIGINT PRIMARY KEY,
    id_label_request BIGINT,  -- ⚠️ PROBLEMA: Puede ser NULL
    id_period BIGINT NOT NULL,
    id_warehouse BIGINT NOT NULL,
    id_product BIGINT NOT NULL,
    estado VARCHAR(50),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
```

---

## 🚀 Próximos Pasos Recomendados

### 1. **Identificar Marbetes Corruptos**
```sql
-- Buscar marbetes con id_label_request NULL
SELECT folio, id_period, id_warehouse, id_product, estado, created_at
FROM labels
WHERE id_label_request IS NULL;
```

### 2. **Limpiar Datos** (Si es necesario)
```sql
-- Opción A: Eliminar marbetes huérfanos (si no hay conteos)
DELETE FROM labels
WHERE id_label_request IS NULL
  AND estado = 'GENERADO'
  AND NOT EXISTS (SELECT 1 FROM label_count_events WHERE folio = labels.folio);

-- Opción B: Crear LabelRequest huérfanos (más complejo, requiere auditoría)
-- Consultar con el equipo antes de ejecutar
```

### 3. **Validación en Creación** (Futuro)
Agregar constraint en la BD o en la lógica para asegurar que `id_label_request` nunca sea NULL:

```sql
ALTER TABLE labels
ADD CONSTRAINT fk_labels_label_request 
FOREIGN KEY (id_label_request) REFERENCES label_requests(id_label_request);
```

---

## 📝 Casos de Prueba

| Caso | Entrada | Comportamiento Esperado |
|------|---------|------------------------|
| Marbete válido | folio=151, id_label_request=5 | Cancela exitosamente |
| Marbete corrupido | folio=151, id_label_request=NULL | ❌ InvalidLabelStateException con mensaje claro |
| Marbete no existe | folio=999, id_label_request=5 | ❌ LabelNotFoundException |
| Ya cancelado | folio=151, estado=CANCELADO | ❌ LabelAlreadyCancelledException |

---

## ✨ Estado

✅ **SOLUCIONADO** - Error manejado gracefully
⚠️ **PENDIENTE** - Limpiar datos corruptos en la BD
📋 **RECOMENDADO** - Agregar constraint en tabla labels


