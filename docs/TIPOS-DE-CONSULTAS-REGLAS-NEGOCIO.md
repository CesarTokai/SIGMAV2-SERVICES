# 📋 TIPOS DE CONSULTAS EN LAS REGLAS DE NEGOCIO - Módulo de Marbetes

## 📊 Resumen Ejecutivo

En el módulo de Marbetes se identifican **5 tipos principales de consultas/operaciones** según las reglas de negocio:

1. **Consultar el Inventario** ⭐ (Ya implementado)
2. **Solicitar Folios**
3. **Generar Folios (Marbetes)**
4. **Imprimir Marbetes**
5. **Registrar Conteos (C1 y C2)**

---

## 1. 📖 CONSULTAR EL INVENTARIO

### Descripción
Interfaz de usuario que permite localizar y obtener información de los productos que integran el inventario de la empresa, mostrando la cantidad de folios solicitados y existentes de cada producto.

### Funcionalidades
- ✅ Consultar productos del inventario por periodo y almacén
- ✅ Paginación personalizable (10, 25, 50, 100 registros)
- ✅ Búsqueda de productos mediante texto
- ✅ Ordenamiento personalizado por columnas
- ✅ Filtrado por periodo y almacén
- ✅ Visualización de folios solicitados y existentes
- ✅ Mostrar estado y existencias de cada producto

### Columnas Mostradas
| Columna | Descripción | Búsqueda | Ordenamiento |
|---------|-------------|----------|--------------|
| Folios solicitados | Cantidad de folios a generar | ❌ | ✅ |
| Folios existentes | Cantidad de folios ya generados | ❌ | ✅ |
| Clave de producto | Código del producto | ✅ | ✅ |
| Producto | Nombre del producto | ✅ | ✅ |
| Clave de almacén | Código del almacén | ✅ | ✅ |
| Almacén | Nombre del almacén | ✅ | ✅ |
| Estado | Estado del producto en inventario | ✅ | ✅ |
| Existencias | Cantidad disponible | ✅ | ✅ |

### Valores por Defecto
- **Periodo**: Último creado (ordenado por fecha DESC)
- **Almacén**: Primero (ordenado por ID ASC)
- **Paginación**: 10 registros por página
- **Ordenamiento**: Por "Clave de producto" ASC

### Estado Implementación
✅ **IMPLEMENTADO COMPLETAMENTE** (28/Nov/2025)

### Archivos Relacionados
- `LabelServiceImpl.getLabelSummary()`
- `LabelSummaryRequestDTO.java`
- `LabelSummaryResponseDTO.java`
- `docs/IMPLEMENTACION-CONSULTA-INVENTARIO-COMPLETA.md`

### Endpoint
```http
POST /api/sigmav2/labels/summary
```

### Ejemplo Request
```json
{
  "periodId": 7,
  "warehouseId": 250,
  "page": 0,
  "size": 10,
  "searchText": "tornillo",
  "sortBy": "claveProducto",
  "sortDirection": "ASC"
}
```

---

## 2. 📝 SOLICITAR FOLIOS

### Descripción
Operación que permite al usuario realizar una solicitud de folios nuevos (marbetes) para un determinado producto del inventario. La solicitud es individual para cada producto.

### Proceso
1. Usuario accede a "Consultar el inventario"
2. Localiza producto en el listado
3. Captura cantidad de folios en columna "Folios solicitados"
4. La cantidad permanece hasta que se ejecute "Generar marbetes"

### Restricciones y Validaciones
| # | Restricción | Descripción |
|---|-------------|-------------|
| 1 | Valor numérico | Solo se pueden ingresar cantidades enteras |
| 2 | Persistencia | Los datos permanecen aunque cambie de módulo o salga |
| 3 | Modificable | Puede cambiar la cantidad antes de generar |
| 4 | Bloqueo por impresión | No se pueden solicitar si existen marbetes GENERADOS sin imprimir |
| 5 | Uso de tabulador | Permite cambiar entre productos con TAB |

### Regla de Negocio Crítica
```
❌ BLOQUEADO: No se podrán capturar folios nuevos (marbetes) si previamente
se generaron folios (marbetes) de ese almacén y no han sido impresos.
```

**Motivo**: Tener control adecuado en captura, generación e impresión de marbetes.

**Verificación**: Sub-módulo "Impresión de Marbetes"

### Estado Implementación
✅ **IMPLEMENTADO**

### Archivos Relacionados
- `LabelServiceImpl.requestLabels()`
- `LabelRequestDTO.java`
- Tabla: `label_requests`

### Endpoint
```http
POST /api/sigmav2/labels/request
```

### Ejemplo Request
```json
{
  "productId": 123,
  "warehouseId": 250,
  "periodId": 7,
  "requestedLabels": 100
}
```

### Roles Permitidos
- ✅ ADMINISTRADOR
- ✅ AUXILIAR
- ✅ ALMACENISTA
- ❌ AUXILIAR_DE_CONTEO

### Validaciones Implementadas
```java
// 1. Validar rol con @PreAuthorize
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")

// 2. Validar acceso al almacén
warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

// 3. Validar que no existan marbetes generados sin imprimir
if (persistence.existsGeneratedUnprintedForProductWarehousePeriod(...)) {
    throw new InvalidLabelStateException("Existen marbetes GENERADOS sin imprimir");
}
```

---

## 3. 🏭 GENERAR FOLIOS (MARBETES)

### Descripción
Operación que convierte las solicitudes de folios en marbetes físicos generados, asignando números de folio únicos consecutivos por periodo.

### Proceso
1. Sistema verifica solicitudes pendientes (folios solicitados > folios generados)
2. Usuario ejecuta "Generar marbetes"
3. Sistema asigna rango de folios consecutivos
4. Marbetes pasan a estado GENERADO
5. Contador "folios solicitados" se actualiza a 0 (o cantidad restante)
6. Contador "folios existentes" aumenta

### Asignación de Folios
- **Secuencia**: Consecutiva por periodo
- **Rango**: Se asigna primer y último folio
- **Bloqueo**: Transaccional con `PESSIMISTIC_WRITE`
- **Unicidad**: Garantizada por tabla `label_folio_sequence`

### Batch Generation
```
Solicitudes: 100 folios
Generación parcial: Permitida
Ejemplo: Generar 50 de 100 solicitados
Resultado: 50 quedan pendientes para siguiente generación
```

### Estado Implementación
✅ **IMPLEMENTADO**

### Archivos Relacionados
- `LabelServiceImpl.generateBatch()`
- `GenerateBatchDTO.java`
- `LabelsPersistenceAdapter.allocateFolioRange()`
- Tabla: `labels`
- Tabla: `label_folio_sequence`

### Endpoint
```http
POST /api/sigmav2/labels/generate
```

### Ejemplo Request
```json
{
  "productId": 123,
  "warehouseId": 250,
  "periodId": 7,
  "labelsToGenerate": 50
}
```

### Roles Permitidos
- ✅ ADMINISTRADOR
- ✅ AUXILIAR
- ✅ ALMACENISTA
- ❌ AUXILIAR_DE_CONTEO

### Validaciones Implementadas
```java
// 1. Validar rol
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")

// 2. Validar acceso al almacén
warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

// 3. Verificar solicitud existente
Optional<LabelRequest> req = persistence.findByProductWarehousePeriod(...);
if (req.isEmpty()) {
    throw new LabelNotFoundException("No existe solicitud");
}

// 4. Verificar folios pendientes
int remaining = req.getRequestedLabels() - req.getFoliosGenerados();
if (remaining <= 0) {
    throw new InvalidLabelStateException("No hay folios para generar");
}
```

---

## 4. 🖨️ IMPRIMIR MARBETES

### Descripción
Operación que registra la impresión de marbetes generados, cambiando su estado de GENERADO a IMPRESO.

### Proceso
1. Usuario consulta marbetes en estado GENERADO
2. Selecciona rango de folios a imprimir
3. Sistema marca marbetes como IMPRESOS
4. Registra timestamp de impresión
5. Genera archivo para impresora física

### Estados de Marbetes
| Estado | Descripción | Puede Imprimir |
|--------|-------------|----------------|
| GENERADO | Recién creado, sin imprimir | ✅ Sí |
| IMPRESO | Ya fue impreso | ❌ No |
| CANCELADO | Anulado/Eliminado | ❌ No |

### Formato de Impresión
```
FOLIO: 000001
PERIODO: 2025-11
ALMACÉN: CEDIS TOKAI
PRODUCTO: TORNILLO 1/4 x 2"
EXISTENCIAS: 456
```

### Estado Implementación
✅ **IMPLEMENTADO**

### Archivos Relacionados
- `LabelServiceImpl.printLabels()`
- `PrintRequestDTO.java`
- Tabla: `label_prints`

### Endpoint
```http
POST /api/sigmav2/labels/print
```

### Ejemplo Request
```json
{
  "warehouseId": 250,
  "periodId": 7,
  "startFolio": 1,
  "endFolio": 50
}
```

### Roles Permitidos
- ✅ ADMINISTRADOR
- ✅ AUXILIAR
- ✅ ALMACENISTA
- ❌ AUXILIAR_DE_CONTEO

### Validaciones Implementadas
```java
// 1. Validar rol
@PreAuthorize("hasAnyRole('ADMINISTRADOR','AUXILIAR','ALMACENISTA')")

// 2. Validar acceso al almacén
warehouseAccessService.validateWarehouseAccess(userId, warehouseId, userRole);

// 3. Verificar estado de marbetes
List<Label> labels = persistence.findGeneratedByRequestIdRange(...);
for (Label lbl : labels) {
    if (lbl.getEstado() != Label.State.GENERADO) {
        throw new InvalidLabelStateException("Marbete ya impreso o cancelado");
    }
}

// 4. TODO: Validar catálogos cargados
// if (!inventoryService.existsForPeriodWarehouse(...)) { throw ... }
```

---

## 5. 🔢 REGISTRAR CONTEOS (C1 y C2)

### Descripción
Sistema de registro de conteos físicos de inventario en dos fases (Conteo 1 y Conteo 2) para verificación cruzada.

### 5.1 Conteo C1 (Primer Conteo)

#### Descripción
Primer conteo físico del inventario, puede ser realizado por personal operativo.

#### Características
- **Secuencia**: Primer conteo, número 1
- **Múltiples conteos**: Solo se puede registrar 1 vez por marbete
- **Roles**: Amplio acceso

#### Roles Permitidos C1
- ✅ ADMINISTRADOR
- ✅ AUXILIAR
- ✅ ALMACENISTA
- ✅ AUXILIAR_DE_CONTEO

#### Endpoint
```http
POST /api/sigmav2/labels/counts/c1
```

#### Ejemplo Request
```json
{
  "folio": 1,
  "countNumber": 1,
  "countedQuantity": 456
}
```

#### Validaciones
```java
@PreAuthorize("hasAnyRole('ADMINISTRADOR','ALMACENISTA','AUXILIAR','AUXILIAR_DE_CONTEO')")

// Validar acceso al almacén del marbete
Label label = persistence.findByFolio(dto.getFolio()).orElseThrow(...);
warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

// Verificar que no se haya registrado antes
if (persistence.hasCountNumber(folio, 1)) {
    throw new DuplicateCountException("C1 ya registrado");
}
```

### 5.2 Conteo C2 (Segundo Conteo)

#### Descripción
Segundo conteo físico del inventario para verificación cruzada. **EXCLUSIVO** de personal especializado.

#### Características
- **Secuencia**: Segundo conteo, número 2
- **Requisito**: Debe existir C1 previo
- **Roles**: Acceso restringido
- **Propósito**: Validación cruzada

#### Roles Permitidos C2
- ❌ ADMINISTRADOR
- ❌ AUXILIAR
- ❌ ALMACENISTA
- ✅ **AUXILIAR_DE_CONTEO** (Exclusivo)

#### Endpoint
```http
POST /api/sigmav2/labels/counts/c2
```

#### Ejemplo Request
```json
{
  "folio": 1,
  "countNumber": 2,
  "countedQuantity": 456
}
```

#### Validaciones
```java
@PreAuthorize("hasRole('AUXILIAR_DE_CONTEO')")  // ← Solo este rol

// Validar acceso al almacén
Label label = persistence.findByFolio(dto.getFolio()).orElseThrow(...);
warehouseAccessService.validateWarehouseAccess(userId, label.getWarehouseId(), userRole);

// Verificar que existe C1 previo
if (!persistence.hasCountNumber(folio, 1)) {
    throw new CountSequenceException("Debe existir C1 antes de C2");
}

// Verificar que C2 no esté duplicado
if (persistence.hasCountNumber(folio, 2)) {
    throw new DuplicateCountException("C2 ya registrado");
}
```

### Estado Implementación Conteos
✅ **IMPLEMENTADO** con restricción AUXILIAR_DE_CONTEO en C2

⚠️ **PENDIENTE ACLARACIÓN**: Documentación ambigua sobre si otros roles pueden hacer C2

### Archivos Relacionados
- `LabelServiceImpl.registerCountC1()`
- `LabelServiceImpl.registerCountC2()`
- `CountEventDTO.java`
- Tabla: `label_count_events`

---

## 📊 MATRIZ DE PERMISOS COMPLETA

| Operación | Endpoint | ADMIN | AUXILIAR | ALMACENISTA | AUX_CONTEO |
|-----------|----------|-------|----------|-------------|------------|
| **1. Consultar Inventario** | GET /summary | ✅ | ✅ | ✅ | ✅ |
| **2. Solicitar Folios** | POST /request | ✅ | ✅ | ✅ | ❌ |
| **3. Generar Marbetes** | POST /generate | ✅ | ✅ | ✅ | ❌ |
| **4. Imprimir Marbetes** | POST /print | ✅ | ✅ | ✅ | ❌ |
| **5. Conteo C1** | POST /counts/c1 | ✅ | ✅ | ✅ | ✅ |
| **6. Conteo C2** | POST /counts/c2 | ❌ | ❌ | ❌ | ✅ |

---

## 🔐 VALIDACIÓN DE ALMACENES POR ROL

| Rol | Acceso a Almacenes | Validación |
|-----|-------------------|------------|
| **ADMINISTRADOR** | Todos | ❌ No valida (acceso total) |
| **AUXILIAR** | Todos | ❌ No valida (acceso total) |
| **ALMACENISTA** | Solo asignados | ✅ Valida tabla `user_warehouse_assignments` |
| **AUXILIAR_DE_CONTEO** | Solo asignados | ✅ Valida tabla `user_warehouse_assignments` |

---

## 📋 TABLAS DE BASE DE DATOS INVOLUCRADAS

| # | Tabla | Propósito | Operación(es) |
|---|-------|-----------|---------------|
| 1 | `products` | Catálogo de productos | Todas |
| 2 | `warehouse` | Catálogo de almacenes | Todas |
| 3 | `period` | Periodos de inventario | Todas |
| 4 | `inventory_stock` | Existencias actuales | Consultar inventario |
| 5 | `label_requests` | Solicitudes de folios | Solicitar, Generar |
| 6 | `labels` | Marbetes generados | Generar, Imprimir, Conteos |
| 7 | `label_folio_sequence` | Secuencia de folios | Generar |
| 8 | `label_prints` | Registro de impresiones | Imprimir |
| 9 | `label_count_events` | Registro de conteos | Conteos C1/C2 |
| 10 | `user_warehouse_assignments` | Asignaciones usuario-almacén | Todas (validación) |

---

## 🎯 FLUJO COMPLETO DE OPERACIONES

```
1. CONSULTAR INVENTARIO
   ↓ Usuario ve productos y decide cantidades

2. SOLICITAR FOLIOS
   ↓ Usuario captura cantidad de marbetes necesarios
   ↓ Se registra en: label_requests

3. GENERAR MARBETES
   ↓ Sistema asigna folios consecutivos
   ↓ Se crean registros en: labels (estado: GENERADO)
   ↓ Se actualiza: label_folio_sequence

4. IMPRIMIR MARBETES
   ↓ Sistema cambia estado a IMPRESO
   ↓ Se registra en: label_prints
   ↓ Se genera archivo de impresión

5. CONTEO C1
   ↓ Personal cuenta físicamente
   ↓ Se registra en: label_count_events (count_number=1)

6. CONTEO C2
   ↓ AUXILIAR_DE_CONTEO verifica
   ↓ Se registra en: label_count_events (count_number=2)
   ↓ Se comparan C1 vs C2 para validar
```

---

## 📚 DOCUMENTOS DE REFERENCIA

1. **Implementación Completa de Consulta**
   - `docs/IMPLEMENTACION-CONSULTA-INVENTARIO-COMPLETA.md`

2. **Reglas de Negocio y Validaciones**
   - `docs/README-MARBETES-REGLAS-NEGOCIO.md`
   - `docs/IMPLEMENTACION-REGLAS-NEGOCIO-MARBETES.md`

3. **Correcciones Multi-almacén**
   - `docs/CORRECCION-MULTIALMACEN-REGLAS-NEGOCIO.md`

4. **Tablas Consultadas**
   - `docs/TABLAS-CONSULTADAS-MODULO-LABELS.md`

5. **Análisis de Cumplimiento**
   - `docs/ANALISIS-CUMPLIMIENTO-MARBETES.md`

6. **Ejemplos de Asignación**
   - `docs/EJEMPLOS-ASIGNACION-ALMACENES.sql`

---

## ✅ ESTADO GENERAL DE IMPLEMENTACIÓN

| Operación | Estado | Fecha Implementación |
|-----------|--------|---------------------|
| Consultar Inventario | ✅ **COMPLETO** | 28/Nov/2025 |
| Solicitar Folios | ✅ **COMPLETO** | 27/Nov/2025 |
| Generar Marbetes | ✅ **COMPLETO** | 27/Nov/2025 |
| Imprimir Marbetes | ⚠️ **PENDIENTE**: Validación catálogos | 27/Nov/2025 |
| Conteo C1 | ✅ **COMPLETO** | 27/Nov/2025 |
| Conteo C2 | ⚠️ **PENDIENTE ACLARACIÓN**: Roles | 27/Nov/2025 |
| Sistema de Almacenes | ✅ **COMPLETO** | 27/Nov/2025 |

---

## 🔍 PENDIENTES IDENTIFICADOS

### 1. ⚠️ Validación de Catálogos en Impresión
**Ubicación**: `LabelServiceImpl.printLabels()`
```java
// TODO: Agregar validación de catálogos cargados
```

### 2. ⚠️ Aclaración de Roles en C2
**Documentación ambigua**: "Todos pueden operar conteo" vs "C2 solo AUXILIAR_DE_CONTEO"
**Recomendación**: Clarificar con stakeholder

### 3. 📝 Poblar Tabla de Asignaciones
**Acción requerida**: Insertar datos en `user_warehouse_assignments`
**Script disponible**: `docs/EJEMPLOS-ASIGNACION-ALMACENES.sql`

### 4. 🔧 Frontend - Agregar Header X-User-Role
**Cambio requerido**: Todas las peticiones al módulo de marbetes
```javascript
headers: {
    'X-User-Id': userId,
    'X-User-Role': userRole  // ← Nuevo
}
```

---

**Fecha de Generación**: 28/Noviembre/2025
**Versión del Documento**: 1.0
**Autor**: Sistema SIGMAV2 - Análisis de Reglas de Negocio

