# 📊 Flujo de Validación Mejorado - getLabelForCount

## 🔄 Diagrama de Flujo

```
┌─────────────────────────────────────────────────────────────────┐
│  Solicitud: GET /api/sigmav2/labels/for-count (POST)            │
│  Datos: { folio, periodId, warehouseId }                        │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
         ┌───────────────────┐
         │ Validar Acceso    │
         │ al Almacén        │
         └────────┬──────────┘
                  │
                  ▼
      ┌──────────────────────────┐
      │ Buscar folio en BD       │
      │ jpaLabelRepository.      │
      │ findById(folio)          │
      └──────┬───────────┬───────┘
             │           │
        EXISTE         NO EXISTE
             │           │
             ▼           ▼
        ┌────────┐   ┌──────────────────────────┐
        │ label  │   │ Buscar marbetes en       │
        │ != null│   │ periodo/almacen solicitado
        └────┬───┘   │ (findByPeriodIdAndWarehouId)
             │       └──────┬──────────────────┘
             │              │
             │              ▼
             │          ¿Existen?
             │         /         \
             │      SÍ             NO
             │     /               \
             │    │                 ▼
             │    │        ┌──────────────────────┐
             │    │        │ THROW LabelNotFound  │
             │    │        │ "Folio no existe.    │
             │    │        │  No hay marbetes     │
             │    │        │  en período/almacén" │
             │    │        └──────────────────────┘
             │    │
             │    ▼
             │  ┌──────────────────────┐
             │  │ Extraer folios       │
             │  │ disponibles (limit 10)
             │  └────────┬─────────────┘
             │           │
             │           ▼
             │   ┌──────────────────────┐
             │   │ THROW LabelNotFound  │
             │   │ "Folio no encontrado │
             │   │  Disponibles: X,Y,Z" │
             │   └──────────────────────┘
             │
             ▼
    ┌────────────────────────────┐
    │ ¿Pertenece a periodo y     │
    │ almacén especificados?     │
    │ label.getPeriodId() ==     │
    │ periodId && label.          │
    │ getWarehouseId() ==        │
    │ warehouseId                │
    └──┬─────────────────────┬───┘
       │                     │
      SÍ                     NO
       │                     │
       │                     ▼
       │            ┌──────────────────────────┐
       │            │ Buscar marbetes en       │
       │            │ periodo/almacen          │
       │            │ solicitado               │
       │            └──────┬───────────────────┘
       │                   │
       │                   ▼
       │        ┌────────────────────┐
       │        │ Obtener lista de   │
       │        │ folios disponibles │
       │        │ (limit 10)         │
       │        └──────┬─────────────┘
       │               │
       │               ▼
       │        ┌────────────────────────────┐
       │        │ THROW InvalidLabelState    │
       │        │ "Folio X pertenece a      │
       │        │  período A, almacén B.    │
       │        │  Consultó período C,      │
       │        │  almacén D.               │
       │        │  Disponibles: ..."        │
       │        └────────────────────────────┘
       │
       ▼
    ┌──────────────────────────────────┐
    │ Obtener datos del producto       │
    │ productRepository.findById()     │
    └────────┬─────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────┐
    │ Obtener datos del almacén        │
    │ warehouseRepository.findById()   │
    └────────┬─────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────┐
    │ Buscar eventos de conteo         │
    │ jpaLabelCountEventRepository.    │
    │ findByFolioOrderByCreatedAtAsc() │
    └────────┬─────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────┐
    │ Construir DTO                    │
    │ LabelForCountDTO                 │
    └────────┬─────────────────────────┘
             │
             ▼
    ┌──────────────────────────────────┐
    │ ✅ RETURN LabelForCountDTO       │
    │    Con información completa:     │
    │    - Folio                       │
    │    - Producto                    │
    │    - Conteos C1, C2              │
    │    - Mensaje informativo         │
    └──────────────────────────────────┘
```

---

## 📋 Casos de Uso

### Caso 1: Folio Válido en Contexto Correcto ✅
```
Input:  { folio: 246, periodId: 20, warehouseId: 420 }
Output: ✅ LabelForCountDTO completo
```

### Caso 2: Folio No Existe en BD ❌
```
Input:  { folio: 999, periodId: 20, warehouseId: 420 }
Error:  "Folio 999 no encontrado. Folios disponibles: 246, 247, 248, 249, 250, 251"
```

### Caso 3: Folio Existe pero Pertenece a Otro Período/Almacén ❌
```
Input:  { folio: 246, periodId: 21, warehouseId: 368 }
Error:  "Folio 246 pertenece a período 20 y almacén 420, pero consultó período 21 
         y almacén 368. Folios disponibles: 123, 124, 125, 126"
```

### Caso 4: Período/Almacén Sin Marbetes ❌
```
Input:  { folio: 100, periodId: 99, warehouseId: 999 }
Error:  "Folio 100 no encontrado. No hay marbetes en el período 99 y almacén 999"
```

---

## 🔍 Métodos Utilizados

| Método | Clase | Propósito |
|--------|-------|----------|
| `findById(folio)` | JpaLabelRepository | Buscar marbete por PK |
| `findByPeriodIdAndWarehouseId()` | LabelPersistence | Buscar marbetes en contexto |
| `findByFolioOrderByCreatedAtAsc()` | JpaLabelCountEventRepository | Obtener eventos de conteo |
| `joining()` | Collectors | Formatear lista de folios |

---

## 🎯 Validaciones de Seguridad

1. ✅ **Validar acceso al almacén** (warehouseAccessService)
2. ✅ **Validar que el folio existe**
3. ✅ **Validar que pertenece al contexto solicitado**
4. ✅ **Proporcionar contexto útil en errores**

---

## 📊 Comparativa de Rendimiento

| Aspecto | Antes | Después |
|---------|-------|---------|
| Queries en caso exitoso | 1 | 1 |
| Queries en error (folio no existe) | 0 | 1 (contexto) |
| Mensajes de error | 1 genérico | N específicos |
| Información útil al usuario | ❌ | ✅ |

---

## 🚀 Mejoras Futuras

1. Cachear folios disponibles por período/almacén
2. Crear endpoint separado para obtener folios disponibles
3. Implementar búsqueda fuzzy para folios similares
4. Guardar historial de búsquedas fallidas para analytics


