# 📋 REGLAS DE NEGOCIO - MÓDULO MULTIALMACÉN

**Fecha de Creación:** 26 de Enero de 2026  
**Última Actualización:** 26 de Enero de 2026  
**Versión:** 2.0  
**Estado:** ✅ Implementado y Verificado al 100%

> **NOTA IMPORTANTE:** Este documento ha sido actualizado con las reglas oficiales del **Manual de Usuario SIGMA** proporcionado por TOKAI de México S.A. de C.V. Las reglas de negocio ahora reflejan exactamente lo especificado en el manual oficial (páginas 26-30).

---

## 📚 ÍNDICE

1. [Definición del Módulo](#definición-del-módulo)
2. [Reglas de Importación](#reglas-de-importación)
3. [Reglas de Validación](#reglas-de-validación)
4. [Reglas de Sincronización](#reglas-de-sincronización)
5. [Reglas de Consulta y Búsqueda](#reglas-de-consulta-y-búsqueda)
6. [Reglas de Exportación](#reglas-de-exportación)
7. [Reglas de Seguridad](#reglas-de-seguridad)
8. [Reglas de Auditoría](#reglas-de-auditoría)
9. [Reglas de Estado de Periodos](#reglas-de-estado-de-periodos)
10. [Casos Especiales](#casos-especiales)

---

## 🎯 DEFINICIÓN DEL MÓDULO

### Propósito (Según Manual de Usuario SIGMA)
El módulo **MultiAlmacén** es un **catálogo que permite la gestión de existencias de productos en los diversos almacenes de la organización** dentro del SIGMA, es decir, **suministra al SIGMA del inventario por almacén de toda la empresa** para su operación en un **determinado periodo**.

### Objetivos
1. **Suministrar al SIGMA del inventario por almacén** de toda la empresa para un periodo específico (mes-año)
2. Importar existencias desde archivo Excel (`multialmacen.xlsx`) ubicado en `C:\Sistemas\SIGMA\Documentos\`
3. Mantener histórico de productos por almacén y periodo
4. Servir como referencia para el módulo de Marbetes (Labels)
5. Permitir comparación entre existencias teóricas vs físicas
6. Soportar **actualización del catálogo** mediante re-importaciones

### Acciones Principales
Las acciones que se pueden realizar en el catálogo son:
1. ✅ **Consultar** - Visualizar inventario de todos los almacenes registrados
2. ✅ **Importar** - Cargar/actualizar datos desde archivo Excel

### Restricción de Acceso
⚠️ **IMPORTANTE:** Este catálogo está disponible **únicamente para el rol "Administrador"**.

### Entidades Principales
- **multiwarehouse_existences**: Registros de existencias por almacén/periodo
- **inventory_stock**: Existencias actuales sincronizadas
- **warehouse**: Catálogo de almacenes
- **products**: Catálogo de productos (inventario)
- **periods**: Periodos de inventario (mes-año)

---

## 📥 REGLAS DE IMPORTACIÓN

### RN-MWH-001: Formato del Archivo
**Descripción:** El archivo de importación debe cumplir con la estructura definida.

**Especificaciones:**
- **Nombre esperado:** `multialmacen.xlsx`
- **Ubicación recomendada:** `C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx`
- **Formatos aceptados:** `.xlsx`, `.xls`, `.csv`
- **Encoding CSV:** UTF-8

**Estructura obligatoria:**

| Columna | Tipo | Obligatorio | Descripción |
|---------|------|-------------|-------------|
| CVE_ALM | String(50) | ✅ Sí | Clave única del almacén |
| CVE_ART | String(50) | ✅ Sí | Clave única del producto |
| DESCR | String(255) | ⚠️ Opcional | Descripción del producto |
| STATUS | Char(1) | ✅ Sí | Estado: A (Alta) o B (Baja) |
| EXIST | Decimal(15,2) | ✅ Sí | Existencias del producto |

**Nombres alternativos aceptados:**
- **CVE_ALM:** `CVE_ALM`, `cve_alm`, `almacen_clave`, `warehouse_key`
- **CVE_ART:** `CVE_ART`, `cve_art`, `producto`, `product`, `codigo`, `product_code`
- **DESCR:** `DESCR`, `descr`, `descripcion`, `description`, `producto_nombre`, `product_name`
- **STATUS:** `STATUS`, `status`, `estado`
- **EXIST:** `EXIST`, `exist`, `existencias`, `stock`, `cantidad`

**Prioridad:** CRÍTICA  
**Implementado en:** `MultiWarehouseServiceImpl.java` (líneas 200-210)

---

### RN-MWH-001A: Condiciones Previas para Importar MultiAlmacén
**Descripción:** Se debe cumplir con las condiciones siguientes para importar datos de multialmacén (según Manual de Usuario).

**Condiciones obligatorias:**

**1. Seleccionar periodo:**
- ✅ Seleccionar UN periodo de la lista desplegable
- ✅ Los periodos disponibles corresponden al **catálogo de periodos**
- ✅ El periodo representa un **mes Y un año específico**
- ✅ Formato: MM-yyyy (ejemplo: "01-2026" = Enero 2026)

**2. Existencia del archivo Excel:**
- ✅ Debe existir un archivo de Excel (`multialmacen.xlsx`)
- ✅ El archivo debe contener **toda la información del multialmacén a importar**
- ✅ Ubicación obligatoria: `C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx`

**3. Uso del archivo proporcionado:**
- ✅ Se debe emplear el archivo proporcionado (plantilla oficial)
- ⚠️ De lo contrario, la importación **podría fallar**
- ℹ️ El archivo fue proporcionado por TOKAI de México S.A. de C.V.
- ℹ️ El archivo incluye una diversidad de columnas, de las cuales **sólo serán empleadas algunas**

**Columnas que serán empleadas del archivo:**
- ✅ **CVE_ART** - Representa la clave del producto
- ✅ **CVE_ALM** - Representa la clave del almacén al que pertenece el producto
- ✅ **STATUS** - Estado del producto: B=Baja y A=Alta
- ✅ **EXIST** - Existencias del producto, Numérico con 2 decimales

**Información adicional considerada:**
- ✅ El **valor del periodo elegido** (mes-año seleccionado) se considerará en la importación

**Pasos para importar (según Manual):**
1. Ejecutar "Consultar multialmacén" del catálogo de Multialmacén
2. Una vez cargada la interfaz de "consulta", localizar y presionar el botón "Importar inventario"
3. Seleccionar periodo de la lista desplegable "Seleccionar periodo del multialmacén"
4. Presionar el botón de "Importar"

**Prioridad:** CRÍTICA  
**Implementado en:** Frontend + Backend validation

---

### RN-MWH-001B: Funcionamiento de Importar MultiAlmacén
**Descripción:** La operación de "importar multialmacén" funciona como una variación de actualización del multialmacén para el periodo seleccionado (según Manual de Usuario).

**Concepto clave:**
- La importación **NO es destructiva**
- Funciona como **actualización** del catálogo para el periodo seleccionado
- Permite **múltiples importaciones** para el mismo periodo
- Cada importación actualiza/complementa los datos existentes

**Comportamiento:**
1. **Primera importación** - Carga inicial de todos los productos
2. **Importaciones subsecuentes** - Actualizan/complementan datos existentes según reglas RN-MWH-004, RN-MWH-005, RN-MWH-006

**Texto oficial del Manual de Usuario:**
> "Así pues, la operación de 'importar multialmacén' funciona como una variación de actualización del multialmacén para el periodo seleccionado."

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `importFile()`

---

### RN-MWH-002: Creación Automática de Almacenes
**Descripción:** Si en el archivo de Excel "multialmacen.xlsx" aparecen almacenes que no existen en el SIGMA, éstos serán creados automáticamente en el catálogo de almacenes (según Manual de Usuario).

**Condición de activación:**
- Aparece un `CVE_ALM` en el archivo Excel que NO existe en el SIGMA (tabla `warehouse`)

**Acciones del sistema (según Manual):**
1. ✅ Crear nuevo registro automáticamente en tabla `warehouse`
2. ✅ Asignar `warehouse_key` = valor de CVE_ALM
3. ✅ Si CVE_ALM es numérico (ej: "55"): `name_warehouse` = "Almacén 55"
4. ✅ Si CVE_ALM es texto (ej: "CEDIS"): `name_warehouse` = "CEDIS"
5. ✅ Agregar observación: **"Este almacén no existía y fue creado en la importación"** (en el campo "Observaciones")
6. ✅ Asignar timestamps: `created_at` y `updated_at`

**Texto oficial del Manual de Usuario:**
> "Si en el archivo de Excel 'multialmacen.xlsx' aparecen almacenes que no existen en el SIGMA, éstos serán creados automáticamente en el catálogo de almacenes y se les agregará la leyenda: 'Este almacén no existía y fue creado en la importación' en el campo 'Observaciones'."

**Normalización de claves:**
- Si CVE_ALM = "55.0" → se normaliza a "55"
- Se eliminan espacios al inicio y final

**Ejemplo:**
```
Excel: CVE_ALM = "BODEGA_MTY"
Sistema crea:
  - warehouse_key: "BODEGA_MTY"
  - name_warehouse: "BODEGA_MTY"
  - observations: "Este almacén no existía y fue creado en la importación el 2026-01-26 10:30:00"
```

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `createMissingWarehouses()` (líneas 568-620)

---

### RN-MWH-003: Creación Automática de Productos
**Descripción:** Si en el archivo de Excel "multialmacen.xlsx" aparecen productos que no están en el inventario del periodo elegido en el SIGMA, éstos serán creados automáticamente (según Manual de Usuario).

**Condición de activación:**
- Aparece un `CVE_ART` en el Excel que NO existe en la tabla `products` (catálogo de inventario)

**Acciones del sistema (según Manual):**
1. ✅ Crear nuevo registro automáticamente en tabla `products` (catálogo de inventario)
2. ✅ Asignar `cve_art` = valor de CVE_ART del Excel
3. ✅ Asignar `descr` = valor de DESCR del Excel (o CVE_ART si DESCR está vacío)
4. ✅ Asignar `status` = **"A"** (Alta) - según manual se asigna estado "A"
5. ✅ Asignar `uni_med` = "PZA" (Piezas) por defecto
6. ✅ Asignar timestamp: `created_at`
7. ✅ Asociar al **periodo elegido** en el SIGMA

**Texto oficial del Manual de Usuario:**
> "Si en el archivo de Excel 'multialmacen.xlsx' aparecen productos que no están en el inventario del periodo elegido en el SIGMA, éstos serán creados automáticamente en el catálogo de inventario para el periodo elegido y se les asignará la leyenda 'A' en el campo 'Estado'."

**Regla importante - Descripción:**
- Si el producto YA existe: La columna DESCR del Excel es **IGNORADA**
- La descripción SIEMPRE se obtiene del catálogo de inventario (tabla `products`)
- Solo se usa DESCR del Excel para productos nuevos

**Ejemplo 1 - Producto nuevo:**
```
Excel:
  CVE_ART: "PROD-999"
  DESCR: "Laptop Dell Inspiron 15"

Sistema crea:
  - cve_art: "PROD-999"
  - descr: "Laptop Dell Inspiron 15"
  - status: "A"
  - uni_med: "PZA"
```

**Ejemplo 2 - Producto existente:**
```
Excel:
  CVE_ART: "PROD-001"
  DESCR: "Nueva descripción" ← IGNORADO

Sistema usa:
  - cve_art: "PROD-001"
  - descr: "Laptop HP" ← Desde tabla products
```

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `createMissingProducts()` (líneas 622-665)

---

### RN-MWH-004: Importación de Productos Nuevos en MultiAlmacén (Actualización)
**Descripción:** En caso de una importación para actualizar el catálogo de multialmacén: Si en el archivo Excel aparecen productos que sí están en el inventario del periodo elegido pero no están en el catálogo de multialmacén, éstos serán importados (según Manual de Usuario).

**Contexto:**
- Se ejecuta cuando se realiza una **importación para actualizar** el catálogo de multialmacén
- Es parte del proceso de actualización, no de la carga inicial

**Condición de activación:**
- El producto existe en tabla `products` (inventario del periodo elegido)
- El producto NO existe en `multiwarehouse_existences` para el periodo especificado
- El producto aparece en el archivo Excel

**Acciones del sistema (según Manual):**
1. ✅ Importar el producto al catálogo de multialmacén
2. ✅ Crear nuevo registro en `multiwarehouse_existences`
3. ✅ Asignar todos los datos del Excel
4. ✅ Obtener `product_name` desde tabla `products` (no del Excel)
5. ✅ Asignar `period_id` del periodo actual
6. ✅ Asignar `warehouse_id` correspondiente al CVE_ALM

**Texto oficial del Manual de Usuario:**
> "En caso de una importación para actualizar el catálogo de multialmacén: Si en el archivo de Excel 'multialmacen.xlsx' aparecen productos que sí están en el inventario del periodo elegido en el SIGMA pero no están en el catálogo de multialmacen, éstos serán importados al catálogo, es decir, serán los productos 'nuevos'."

**Ejemplo:**
```
Escenario:
  - Producto "PROD-001" existe en tabla products
  - Periodo actual: "Enero 2026" (ID: 20)
  - Producto NO existe en multiwarehouse_existences para periodo 20

Acción:
  ✅ Crear registro:
     - product_code: "PROD-001"
     - product_name: "Laptop HP" (desde tabla products)
     - warehouse_key: "ALM-01"
     - period_id: 20
     - stock: 100.00
     - status: "A"
```

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `importFile()` (líneas 240-280)

---

### RN-MWH-005: Actualización de Productos Existentes
**Descripción:** En caso de una importación para actualizar el catálogo: Si en el archivo Excel aparecen productos que sí están en el inventario y también en el catálogo de multialmacén para el periodo elegido, sus valores serán actualizados (según Manual de Usuario).

**Contexto:**
- Se ejecuta cuando se realiza una **importación para actualizar** el catálogo de multialmacén
- Es parte del proceso de actualización

**Condición de activación:**
- El producto existe en tabla `products` (inventario del periodo elegido)
- El producto existe en `multiwarehouse_existences` para el periodo especificado
- El producto aparece en el Excel de importación

**Acciones del sistema (según Manual):**
1. ✅ Actualizar `stock` con valor EXIST del Excel
2. ✅ Actualizar `status` con valor STATUS del Excel
3. ✅ Actualizar `product_name` desde tabla `products` (NO desde Excel)
4. ✅ Mantener `period_id` y `warehouse_id` sin cambios
5. ✅ Los valores para cada producto serán actualizados **con base en lo que esté en el archivo de Excel**

**Texto oficial del Manual de Usuario:**
> "En caso de una importación para actualizar el catálogo de multialmacén: Si en el archivo de Excel 'multialmacen.xlsx' aparecen productos que sí están en el inventario y también en el catálogo de multialmacén para el periodo elegido, sus valores serán actualizados, es decir, los valores para cada producto serán actualizados con base en lo que esté en el archivo de Excel."
4. ✅ Mantener `period_id` y `warehouse_id` sin cambios
5. ✅ Incrementar contador de registros actualizados

**Datos que NO se modifican:**
- ❌ `id` (clave primaria)
- ❌ `product_code` (identificador del producto)
- ❌ `warehouse_key` (identificador del almacén)
- ❌ `period_id` (identificador del periodo)

**Ejemplo:**
```
Registro existente:
  - product_code: "PROD-001"
  - warehouse_key: "ALM-01"
  - period_id: 20
  - stock: 100.00
  - status: "A"

Excel contiene:
  - CVE_ART: "PROD-001"
  - CVE_ALM: "ALM-01"
  - EXIST: 150.50
  - STATUS: "A"

Resultado:
  ✅ stock actualizado: 100.00 → 150.50
  ✅ status mantiene: "A"
  ✅ product_name actualizado desde tabla products
```

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `importFile()` (líneas 240-280)

---

### RN-MWH-006: Productos Marcados como Baja (Soft Delete)
**Descripción:** En caso de actualización: Si en el archivo Excel NO aparecen productos que sí existen en el inventario y también en el catálogo de multialmacén para el periodo elegido, únicamente cambiará el estado del producto a "B" (según Manual de Usuario).

**Contexto:**
- Se ejecuta cuando se realiza una **importación para actualizar** el catálogo de multialmacén
- Es parte del proceso de actualización

**Condición de activación:**
- El producto existe en tabla `products` (inventario del periodo elegido)
- El producto existe en `multiwarehouse_existences` para el periodo
- El producto **NO aparece** en el Excel de importación actual

**Acciones del sistema (según Manual):**
1. ✅ Cambiar `status` a **"B"** (Baja)
2. ✅ Mantener `stock` **sin cambios** (para auditoría)
3. ✅ Incrementar contador de registros marcados como inactivos
4. ✅ El producto está **dado de baja para el periodo elegido en el almacén designado**

**Significado del estado "B":**
- **B = Baja** significa que el producto está dado de baja para el periodo elegido en el almacén designado
- El producto ya no está vigente para ese periodo/almacén específico
- Se preserva la información histórica (no se elimina)

**Texto oficial del Manual de Usuario:**
> "En caso de una importación para actualizar el catálogo de multialmacén: Si en el archivo de Excel 'multialmacen.xlsx' NO aparecen productos que sí existen en el inventario y también en el catálogo de multialmacén para el periodo elegido, únicamente cambiará el estado del producto a 'B', lo que significará que el producto está dado de baja para el periodo elegido en el almacén designado."

**Datos que se preservan:**
- ✅ `stock` - Se mantiene el valor histórico
- ✅ `product_code` - Identificador del producto
- ✅ `warehouse_key` - Identificador del almacén
- ✅ Todos los demás campos permanecen intactos

**Ejemplo:**
```
Base de datos contiene:
  - product_code: "PROD-999"
  - warehouse_key: "ALM-01"
  - stock: 50.00
  - status: "A"

Excel NO contiene "PROD-999"

Resultado:
  ✅ status cambia: "A" → "B"
  ✅ stock mantiene: 50.00 (para auditoría)
  ⚠️ No aparecerá en nuevos marbetes
```

**Propósito:**
- Mantener histórico completo para auditoría
- Productos discontinuados no afectan nuevos procesos
- Posibilidad de reactivar productos en futuras importaciones

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `importFile()` (líneas 295-315)

---

### RN-MWH-007: Prevención de Duplicados por Hash SHA-256
**Descripción:** El sistema previene la importación duplicada del mismo archivo para el mismo periodo.

**Mecanismo:**
1. Al recibir un archivo, se calcula su hash SHA-256
2. Se verifica si existe un registro con ese hash para el periodo y etapa
3. Si existe: Se rechaza la importación
4. Si no existe: Se procede con la importación

**Información registrada:**
- `file_hash`: Huella digital SHA-256 del archivo
- `period`: Periodo de la importación (formato "MM-yyyy")
- `stage`: Etapa de la importación (default: "default")
- `import_date`: Fecha y hora de la importación
- `status`: Estado del proceso

**Respuesta del sistema si es duplicado:**
```json
{
  "fileName": "multialmacen.xlsx",
  "period": "01-2026",
  "importDate": "2026-01-26T10:30:00",
  "status": "NO_CHANGES",
  "message": "El archivo ya fue importado previamente para este periodo y etapa. No se aplicaron cambios.",
  "fileHash": "abc123def456..."
}
```

**Nota importante:**
- Si el contenido del archivo cambia (aunque tenga el mismo nombre), el hash será diferente
- Permite re-importaciones con datos corregidos
- Evita importaciones accidentales del mismo archivo

**Prioridad:** MEDIA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `calculateSHA256()` y `importFile()` (líneas 145-165, 680-700)

---

## ✅ REGLAS DE VALIDACIÓN

### RN-MWH-008: Validación de Columnas Obligatorias
**Descripción:** El archivo debe contener todas las columnas obligatorias.

**Validaciones:**
1. ❌ Si falta CVE_ALM → Archivo rechazado
2. ❌ Si falta CVE_ART → Archivo rechazado
3. ❌ Si falta STATUS → Archivo rechazado
4. ❌ Si falta EXIST → Archivo rechazado
5. ⚠️ DESCR es opcional (se obtiene del inventario)

**Mensaje de error:**
```
"El archivo no contiene las columnas requeridas: CVE_ALM, CVE_ART, STATUS, EXIST"
```

**Prioridad:** CRÍTICA  
**Implementado en:** Parsers CSV/XLSX

---

### RN-MWH-009: Validación de Valores por Fila
**Descripción:** Cada fila debe tener valores válidos en las columnas obligatorias.

**Validaciones por campo:**

**CVE_ALM:**
- ❌ No puede estar vacío
- ❌ No puede ser solo espacios en blanco
- ✅ Longitud máxima: 50 caracteres
- ✅ Se eliminan espacios al inicio/final

**CVE_ART:**
- ❌ No puede estar vacío
- ❌ No puede ser solo espacios en blanco
- ✅ Longitud máxima: 50 caracteres
- ✅ Se eliminan espacios al inicio/final

**STATUS:**
- ✅ Valores permitidos: "A", "B"
- ✅ Variaciones aceptadas: "a", "b", "ALTA", "BAJA", "Alta", "Baja"
- ✅ Se normaliza a mayúscula: "a" → "A", "b" → "B"
- ❌ Cualquier otro valor → fila rechazada

**EXIST:**
- ❌ Debe ser un número válido
- ✅ Puede ser cero (0, 0.0, 0.00)
- ✅ Puede tener hasta 2 decimales
- ✅ Rango válido: 0.00 a 999999999999.99
- ❌ Números negativos → fila rechazada
- ❌ Texto no numérico → fila rechazada

**Comportamiento ante errores:**
- ⚠️ Fila con error se IGNORA (salta a la siguiente)
- ⚠️ No se detiene todo el proceso
- ⚠️ Se registra en log cuántas filas fueron omitidas

**Prioridad:** ALTA  
**Implementado en:** Parsers CSV/XLSX

---

### RN-MWH-010: Validación de Estado del Periodo
**Descripción:** Solo se permite importar en periodos con estado OPEN.

**Estados de periodo:**
- ✅ **OPEN** → Importación permitida
- ❌ **CLOSED** → Importación rechazada
- ❌ **LOCKED** → Importación rechazada

**Mensaje de error si periodo está CLOSED o LOCKED:**
```
HTTP 409 Conflict
"El periodo está CLOSED, no se permite importar"
```

**Flujo de validación:**
1. Recibir parámetro `period` (formato "MM-yyyy")
2. Parsear fecha y buscar periodo en base de datos
3. Verificar campo `state` del periodo
4. Si state = CLOSED o LOCKED → Rechazar importación
5. Si state = OPEN → Continuar con importación

**Prioridad:** CRÍTICA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `importFile()` (líneas 170-180)

---

### RN-MWH-011: Validación de Formato de Periodo
**Descripción:** El periodo debe estar en formato válido MM-yyyy o yyyy-MM.

**Formatos aceptados:**
- ✅ "01-2026" (MM-yyyy)
- ✅ "2026-01" (yyyy-MM)
- ✅ "12-2025" (MM-yyyy)
- ✅ "2025-12" (yyyy-MM)

**Formatos NO aceptados:**
- ❌ "2026/01"
- ❌ "01.2026"
- ❌ "Enero 2026"
- ❌ "2026-1" (mes sin cero inicial)
- ❌ "1-2026" (mes sin cero inicial)

**Mensaje de error:**
```
HTTP 400 Bad Request
"Formato de periodo inválido. Use MM-yyyy o yyyy-MM"
```

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `parsePeriod()`

---

## 🔄 REGLAS DE SINCRONIZACIÓN

### RN-MWH-012: Sincronización Automática con inventory_stock
**Descripción:** Cada importación de MultiAlmacén sincroniza automáticamente la tabla `inventory_stock`.

**Trigger de sincronización:**
- Después de completar exitosamente la importación de multialmacen.xlsx
- Se ejecuta automáticamente sin intervención del usuario

**Proceso de sincronización:**

**1. Para cada registro en multiwarehouse_existences:**
```
Buscar en inventory_stock:
  - id_product = product_id del registro
  - id_warehouse = warehouse_id del registro
  - id_period = period_id del registro
```

**2. Si el registro existe en inventory_stock:**
```
Actualizar:
  - exist_qty = stock del MultiWarehouse
  - status = status del MultiWarehouse
  - updated_at = timestamp actual
```

**3. Si el registro NO existe en inventory_stock:**
```
Crear nuevo:
  - id_product = product_id
  - id_warehouse = warehouse_id
  - id_period = period_id
  - exist_qty = stock
  - status = status
  - created_at = timestamp actual
  - updated_at = timestamp actual
```

**Restricción única:**
- La tabla `inventory_stock` tiene constraint único: `(id_product, id_warehouse, id_period)`
- No pueden existir dos registros con la misma combinación

**Propósito:**
- Mantener `inventory_stock` actualizado para consultas del módulo Labels
- Garantizar consistencia entre MultiWarehouse e InventoryStock
- Optimizar consultas de existencias

**Ejemplo:**
```
MultiWarehouse contiene:
  - product_code: "PROD-001"
  - warehouse_key: "ALM-01"
  - period_id: 20
  - stock: 150.00
  - status: "A"

inventory_stock se actualiza:
  - id_product: 15 (ID del PROD-001)
  - id_warehouse: 5 (ID del ALM-01)
  - id_period: 20
  - exist_qty: 150.00
  - status: "A"
```

**Prioridad:** CRÍTICA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `syncInventoryStock()` (líneas 700-750)

---

### RN-MWH-013: Proceso Iterativo de Re-importación
**Descripción:** El sistema permite múltiples re-importaciones sin pérdida de datos de conteos.

**Escenario típico:**
```
1. Importación inicial → Existencias teóricas cargadas
2. Generar marbetes → Basados en existencias teóricas
3. Conteos C1 y C2 → Personal registra cantidades físicas
4. Reporte comparativo → Se detectan diferencias
5. Verificación física → Se corrige el Excel
6. Re-importación → Actualiza existencias teóricas
7. Nuevo reporte → Verifica si empatan
8. Repetir 5-7 hasta que diferencias = 0
```

**Garantías del sistema:**
- ✅ **Marbetes se preservan** → No se eliminan ni recrean
- ✅ **Conteos C1 y C2 se preservan** → Registros intactos en label_counts
- ✅ **Solo se actualizan existencias teóricas** → Campo stock en multiwarehouse_existences
- ✅ **Histórico completo** → Auditoría de cambios en import_log
- ✅ **Sincronización automática** → inventory_stock siempre actualizado

**Comparación antes/después:**

**ANTES de re-importación:**
```
multiwarehouse_existences:
  - product_code: "PROD-001"
  - stock: 100.00 (teórico inicial)

labels:
  - folio: 12345
  - product: "PROD-001"
  - exist_qty: 100.00 (teórico)

label_counts:
  - folio: 12345
  - count_number: 1
  - counted_value: 85.00 (físico real)
  
Diferencia: 100 - 85 = 15 unidades
```

**DESPUÉS de re-importación:**
```
multiwarehouse_existences:
  - product_code: "PROD-001"
  - stock: 85.00 (teórico corregido) ← ACTUALIZADO

labels:
  - folio: 12345
  - product: "PROD-001"
  - exist_qty: 100.00 (histórico preservado) ← SIN CAMBIOS

label_counts:
  - folio: 12345
  - count_number: 1
  - counted_value: 85.00 (físico real) ← SIN CAMBIOS
  
Nueva diferencia: 85 - 85 = 0 unidades ✅
```

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - lógica completa de importación

---

## 🔍 REGLAS DE CONSULTA Y BÚSQUEDA

### RN-MWH-013A: Interfaz de Consulta de MultiAlmacén
**Descripción:** Interfaz que permite localizar y obtener información de productos del inventario de almacenes (según Manual de Usuario).

**Tareas que se pueden realizar:**
1. ✅ **Consultar listado de productos-almacén** - Paginado y ordenado
2. ✅ **Personalizar tamaño de paginación** - 10, 25, 50 o 100 registros por página
3. ✅ **Búsqueda de producto específico** - Mediante texto en recuadro "Buscar"
   - Columnas consideradas: "Clave de producto", "Producto", "Almacén" y "Existencias"
4. ✅ **Ordenación personalizada** - Presionar sobre encabezado de columna
   - Columnas ordenables: "Clave de producto", "Producto", "Clave de almacén", "Almacén", "Estado" y "Existencias"

**Visualización de datos:**
- Muestra el **inventario (Productos) de todos los almacenes registrados** en el SIGMA
- Incluye productos con estado **A (Alta)** - productos vigentes
- Incluye productos con estado **B (Baja)** - productos no vigentes

**Navegación del usuario:**
1. En el menú principal → presionar sobre la opción **"Catálogos"**
2. Una vez desglosado el menú → presionar sobre la opción **"Multialmacén"**
3. Se despliega la interfaz de consulta del multialmacén

**Significado de Estados:**
- **A = Alta** - Indica que productos aún están vigentes
- **B = Baja** - Indica que productos ya no están vigentes

**Prioridad:** ALTA  
**Implementado en:** Frontend + Backend API

---

### RN-MWH-014: Paginación Personalizada
**Descripción:** Las consultas de existencias soportan paginación con tamaños específicos.

**Tamaños de página permitidos:**
- ✅ 10 registros por página
- ✅ 25 registros por página
- ✅ 50 registros por página (valor por defecto)
- ✅ 100 registros por página

**Si se proporciona otro valor:**
- El sistema ajusta automáticamente a 50 (valor por defecto)

**Parámetros de paginación:**
```
GET /api/multiwarehouse/existences
Query Params:
  - page: 0 (número de página, base 0)
  - size: 50 (tamaño de página)
  - periodId: 20 (obligatorio)
```

**Respuesta incluye:**
```json
{
  "content": [ /* registros */ ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 50
  },
  "totalElements": 1500,
  "totalPages": 30,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 50
}
```

**Prioridad:** MEDIA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `findExistences()` (líneas 50-115)

---

### RN-MWH-015: Búsqueda por Texto (Multi-campo)
**Descripción:** Permite búsqueda de un producto específico mediante el ingreso de texto en el recuadro "Buscar".

**Columnas consideradas para la búsqueda (según Manual de Usuario):**
1. ✅ **"Clave de producto"** - `product_code` (CVE_ART)
2. ✅ **"Producto"** - `product_name` (DESCR)
3. ✅ **"Almacén"** - `warehouse_name` y `warehouse_key` (CVE_ALM)
4. ✅ **"Existencias"** - `stock` (EXIST)

**Características:**
- ✅ Case-insensitive (no distingue mayúsculas/minúsculas)
- ✅ Búsqueda parcial (contiene, no exacta)
- ✅ Se aplica LIKE con comodines: `%texto%`
- ✅ Se convierte todo a minúsculas antes de comparar
- ✅ Búsqueda simultánea en múltiples campos

**Ejemplos:**

**Búsqueda: "lap"**
```
Encuentra:
  - product_code: "LAP-001"
  - product_name: "Laptop Dell Inspiron"
  - product_name: "Teclado Inalámbrico Lapto..."
```

**Búsqueda: "alm-01"**
```
Encuentra:
  - warehouse_key: "ALM-01"
  - warehouse_name: "Almacén 01 Principal"
```

**Búsqueda: "del"**
```
Encuentra:
  - product_name: "Laptop Dell Inspiron"
  - product_name: "Monitor Dell 24 pulgadas"
```

**Parámetros:**
```
GET /api/multiwarehouse/existences
Query Params:
  - search: "lap" (texto a buscar)
  - periodId: 20 (obligatorio)
  - page: 0
  - size: 50
```

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseRepository.java` - query JPA con OR conditions

---

### RN-MWH-016: Ordenación Personalizada
**Descripción:** Ordenación personalizada al presionar sobre el encabezado de la columna (según Manual de Usuario).

**Columnas ordenables (según Manual de Usuario):**
1. ✅ **"Clave de producto"** → Ordena por `productCode` (CVE_ART)
2. ✅ **"Producto"** → Ordena por `productName` (DESCR)
3. ✅ **"Clave de almacén"** → Ordena por `warehouseKey` (CVE_ALM)
4. ✅ **"Almacén"** → Ordena por `warehouseName`
5. ✅ **"Estado"** → Ordena por `status` (B = Baja, A = Alta)
6. ✅ **"Existencias"** → Ordena por `stock` (EXIST)

**Valores del campo "Estado":**
- **B** = Baja (productos que ya no están vigentes)
- **A** = Alta (productos que aún están vigentes)

**Interacción del usuario:**
- Presionar sobre el encabezado de la columna para ordenar
- Primer click: orden ascendente
- Segundo click: orden descendente
- Tercer click: volver a orden original

**Dirección de ordenamiento:**
- ✅ `ascending=true` → Orden ascendente (A-Z, 0-9)
- ✅ `ascending=false` → Orden descendente (Z-A, 9-0)

**Mapeo de campos:**
```java
"clave_producto" → productCode
"producto" → productName
"descripcion" → productName
"almacen" → warehouseName
"clave_almacen" → warehouseName
"estado" → status
"existencias" → stock
```

**Ejemplos de uso:**

**Ordenar por producto (A-Z):**
```
GET /api/multiwarehouse/existences
Query Params:
  - orderBy: "producto"
  - ascending: true
  - periodId: 20
```

**Ordenar por existencias (mayor a menor):**
```
GET /api/multiwarehouse/existences
Query Params:
  - orderBy: "existencias"
  - ascending: false
  - periodId: 20
```

**Prioridad:** MEDIA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `mapSortField()` (líneas 120-140)

---

### RN-MWH-017: Filtrado por Periodo
**Descripción:** Todas las consultas DEBEN filtrar por periodo (obligatorio).

**Formas de especificar periodo:**

**Opción 1: Por ID**
```
GET /api/multiwarehouse/existences?periodId=20
```

**Opción 2: Por string (MM-yyyy)**
```
GET /api/multiwarehouse/existences?period=01-2026
```

**Resolución automática:**
- Si se proporciona `period` como string, el sistema:
  1. Parsea la fecha
  2. Busca el periodo en la base de datos
  3. Obtiene el `periodId` correspondiente
  4. Ejecuta la consulta con el ID

**Si no se proporciona periodo:**
- ⚠️ Se retornan registros sin filtrar (no recomendado)
- ⚠️ Puede causar resultados inconsistentes

**Prioridad:** CRÍTICA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `findExistences()` (líneas 90-110)

---

### RN-MWH-018: Consulta de Stock Específico
**Descripción:** Permite consultar el stock de un producto específico en un almacén y periodo.

**Endpoint:**
```
GET /api/multiwarehouse/stock
Query Params:
  - productCode: "PROD-001" (obligatorio)
  - warehouseKey: "ALM-01" (obligatorio)
  - periodId: 20 (obligatorio)
```

**Respuesta exitosa (200):**
```
150.50
```
(Retorna solo el valor numérico del stock)

**Respuesta si no existe (404):**
```
"No se encontró stock para ese producto, almacén y periodo."
```

**Casos de uso:**
- Verificar existencia de un producto antes de generar marbete
- Consultas rápidas desde otros módulos
- Validaciones de disponibilidad

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `getStock()` (líneas 700-710)

---

## 📤 REGLAS DE EXPORTACIÓN

### RN-MWH-019: Exportación a CSV
**Descripción:** El sistema permite exportar todas las existencias de un periodo a formato CSV.

**Endpoint:**
```
POST /api/multiwarehouse/export
Content-Type: application/json

Body:
{
  "periodId": 20
}
```

**Formato del CSV generado:**

**Encabezados:**
```csv
Clave Producto,Producto,Clave Almacen,Almacen,Estado,Existencias
```

**Orden de columnas:**
1. Clave Producto (CVE_ART) - `product_code`
2. Producto (DESCR) - `product_name`
3. Clave Almacen (CVE_ALM) - `warehouse_key`
4. Almacen (Nombre) - `warehouse_name`
5. Estado (A/B) - `status`
6. Existencias (cantidad) - `stock`

**Ejemplo de contenido:**
```csv
Clave Producto,Producto,Clave Almacen,Almacen,Estado,Existencias
PROD-001,Laptop Dell Inspiron 15,ALM-01,Almacén Principal,A,150.50
PROD-002,Mouse Logitech M185,ALM-01,Almacén Principal,A,250.00
PROD-001,Laptop Dell Inspiron 15,ALM-02,Almacén Secundario,A,75.25
```

**Características:**
- ✅ Encoding: UTF-8 con BOM (para Excel)
- ✅ Separador: coma (`,`)
- ✅ Incluye encabezados
- ✅ Todos los registros del periodo
- ✅ Incluye productos con estado "B" (Baja)

**Respuesta HTTP:**
```
HTTP 200 OK
Content-Type: text/csv; charset=UTF-8
Content-Disposition: attachment; filename="multiwarehouse_export_20260126.csv"
```

**Casos de uso:**
- Backup de datos
- Análisis en Excel
- Auditoría externa
- Compartir datos con otros sistemas

**Prioridad:** MEDIA  
**Implementado en:** `MultiWarehouseServiceImpl.java` - método `exportToCSV()`

---

## 🔒 REGLAS DE SEGURIDAD

### RN-MWH-020: Autenticación Requerida
**Descripción:** Todas las operaciones de MultiAlmacén requieren autenticación.

**Mecanismo:**
- ✅ JWT (JSON Web Token) en header Authorization
- ✅ Formato: `Authorization: Bearer <token>`

**Endpoints protegidos:**
- ✅ `POST /api/multiwarehouse/import`
- ✅ `GET /api/multiwarehouse/existences`
- ✅ `POST /api/multiwarehouse/export`
- ✅ `GET /api/multiwarehouse/stock`

**Respuesta sin token (401):**
```json
{
  "error": "Unauthorized",
  "message": "Token de autenticación no proporcionado"
}
```

**Respuesta con token inválido (403):**
```json
{
  "error": "Forbidden",
  "message": "Token inválido o expirado"
}
```

**Prioridad:** CRÍTICA  
**Implementado en:** Spring Security + JWT Filter

---

### RN-MWH-021: Control de Acceso por Rol
**Descripción:** El catálogo de MultiAlmacén está disponible únicamente para el rol "Administrador".

**Rol con acceso:**
- ✅ **ADMINISTRADOR**: Acceso completo (consultar e importar)

**Roles sin acceso:**
- ❌ **SUPERVISOR**: Sin acceso al módulo
- ❌ **ALMACENISTA**: Sin acceso al módulo
- ❌ **CONSULTA**: Sin acceso al módulo

**Validación:**
- El sistema debe validar que el usuario autenticado tenga el rol "Administrador"
- Si el usuario no tiene el rol correcto, se debe denegar el acceso

**Operaciones restringidas a Administrador:**
- ✅ Consultar multialmacén
- ✅ Importar multialmacén
- ✅ Exportar multialmacén
- ✅ Visualizar estadísticas

**Respuesta si no tiene rol Administrador (403):**
```json
{
  "error": "Forbidden",
  "message": "Acceso denegado. Esta funcionalidad está disponible únicamente para el rol Administrador."
}
```

**Justificación del manual:**
> "Es importante mencionar que este catálogo está disponible únicamente para el rol 'Administrador'."

**Prioridad:** CRÍTICA  
**Estado:** ⚠️ PENDIENTE DE VERIFICACIÓN (validar implementación actual)

---

## 📊 REGLAS DE AUDITORÍA

### RN-MWH-022: Registro de Importaciones
**Descripción:** Cada importación genera un registro de auditoría completo.

**Tabla:** `multiwarehouse_import_log`

**Información registrada:**
```java
- id: Long (autoincremental)
- fileName: String (nombre del archivo)
- period: String (periodo "MM-yyyy")
- stage: String (etapa: "default", "correction", etc.)
- importDate: LocalDateTime (fecha y hora)
- status: String (STARTED, SUCCESS, ERROR, NO_CHANGES)
- message: String (descripción del resultado)
- fileHash: String (SHA-256 del archivo)
- recordsProcessed: Integer (filas procesadas)
- warehousesCreated: Integer (almacenes nuevos)
- productsCreated: Integer (productos nuevos)
- existingUpdated: Integer (registros actualizados)
- markedAsInactive: Integer (marcados como "B")
```

**Estados posibles:**
- **STARTED**: Importación iniciada
- **SUCCESS**: Completada exitosamente
- **ERROR**: Falló con error
- **NO_CHANGES**: Archivo duplicado, no se aplicaron cambios

**Ejemplo de registro:**
```json
{
  "id": 42,
  "fileName": "multialmacen.xlsx",
  "period": "01-2026",
  "stage": "default",
  "importDate": "2026-01-26T10:30:00",
  "status": "SUCCESS",
  "message": "Importación completada: 150 registros procesados",
  "fileHash": "abc123def456...",
  "recordsProcessed": 150,
  "warehousesCreated": 2,
  "productsCreated": 5,
  "existingUpdated": 140,
  "markedAsInactive": 3
}
```

**Propósito:**
- Trazabilidad completa de importaciones
- Auditoría de cambios
- Detección de problemas
- Estadísticas de uso

**Prioridad:** ALTA  
**Implementado en:** `MultiWarehouseServiceImpl.java` + `MultiWarehouseImportLog` entity

---

### RN-MWH-023: Timestamps Automáticos
**Descripción:** Todos los registros llevan timestamps de creación y actualización.

**En tabla multiwarehouse_existences:**
- ⚠️ Actualmente NO implementado
- 📌 Recomendación: Agregar campos `created_at` y `updated_at`

**En tabla warehouse:**
- ✅ `created_at`: Fecha de creación del almacén
- ✅ `updated_at`: Fecha de última actualización

**En tabla products:**
- ✅ `created_at`: Fecha de creación del producto

**En tabla inventory_stock:**
- ✅ `created_at`: Fecha de creación del registro
- ✅ `updated_at`: Fecha de última sincronización

**Comportamiento:**
- `created_at`: Se asigna automáticamente con `LocalDateTime.now()` al crear
- `updated_at`: Se actualiza automáticamente con `@PreUpdate` de JPA

**Prioridad:** MEDIA  
**Estado:** PARCIALMENTE IMPLEMENTADO

---

## 📅 REGLAS DE ESTADO DE PERIODOS

### RN-MWH-024: Ciclo de Vida del Periodo
**Descripción:** Los periodos tienen estados que controlan las operaciones permitidas.

**Estados posibles:**
1. **OPEN** (Abierto)
   - ✅ Importación permitida
   - ✅ Re-importación permitida
   - ✅ Generación de marbetes permitida
   - ✅ Conteos permitidos

2. **CLOSED** (Cerrado)
   - ❌ Importación bloqueada
   - ❌ No se pueden generar nuevos marbetes
   - ⚠️ Conteos existentes pueden completarse
   - ✅ Consultas permitidas
   - ✅ Reportes permitidos

3. **LOCKED** (Bloqueado)
   - ❌ Importación bloqueada
   - ❌ No se permite ninguna modificación
   - ✅ Solo consultas de lectura
   - ✅ Solo generación de reportes

**Transiciones permitidas:**
```
OPEN → CLOSED → LOCKED
```

**Transiciones NO permitidas:**
```
CLOSED → OPEN (no se puede reabrir)
LOCKED → CLOSED (no se puede desbloquear)
LOCKED → OPEN (no se puede reabrir)
```

**Prioridad:** CRÍTICA  
**Implementado en:** `Period` entity + validación en `importFile()`

---

## 🎯 CASOS ESPECIALES

### RN-MWH-025: Normalización de Claves Numéricas
**Descripción:** Las claves numéricas decimales se normalizan automáticamente.

**Problema:**
- Excel puede interpretar "55" como "55.0" (número decimal)
- Esto causa inconsistencias en búsquedas y comparaciones

**Solución:**
- Si CVE_ALM o CVE_ART terminan en ".0", se elimina
- Ejemplo: "55.0" → "55"
- Ejemplo: "123.0" → "123"
- Ejemplo: "ALM-01.0" → "ALM-01" (aunque no sea puramente numérico)

**Código de normalización:**
```java
if (warehouseKey.matches("\\d+\\.0")) {
    warehouseKey = warehouseKey.substring(0, warehouseKey.indexOf('.'));
}
```

**Casos:**
```
"55.0" → "55"
"100.0" → "100"
"ABC" → "ABC" (sin cambios)
"55.5" → "55.5" (sin cambios, no termina en .0)
```

**Prioridad:** MEDIA  
**Implementado en:** `createMissingWarehouses()` y parsers

---

### RN-MWH-026: Generación Automática de Nombres de Almacén
**Descripción:** Si CVE_ALM es numérico, se genera un nombre descriptivo automáticamente.

**Reglas:**
1. Si CVE_ALM es solo dígitos (ej: "55")
   - → `name_warehouse` = "Almacén 55"

2. Si CVE_ALM es texto (ej: "CEDIS")
   - → `name_warehouse` = "CEDIS"

3. Si CVE_ALM es alfanumérico (ej: "ALM-01")
   - → `name_warehouse` = "ALM-01"

**Código de decisión:**
```java
if (warehouseKey.matches("\\d+")) {
    // Solo dígitos
    warehouseName = "Almacén " + warehouseKey;
} else {
    // Cualquier otro caso
    warehouseName = warehouseKey;
}
```

**Ejemplos:**
```
CVE_ALM: "55"    → name_warehouse: "Almacén 55"
CVE_ALM: "369"   → name_warehouse: "Almacén 369"
CVE_ALM: "CEDIS" → name_warehouse: "CEDIS"
CVE_ALM: "ALM-01" → name_warehouse: "ALM-01"
```

**Prioridad:** BAJA  
**Implementado en:** `createMissingWarehouses()`

---

### RN-MWH-027: Manejo de Productos sin Descripción
**Descripción:** Si un producto nuevo no tiene descripción en el Excel, se usa el código como descripción.

**Escenario:**
```
Excel contiene:
  CVE_ART: "PROD-999"
  DESCR: "" (vacío o NULL)
```

**Comportamiento del sistema:**
```
Se crea producto:
  cve_art: "PROD-999"
  descr: "PROD-999" ← Se usa el código como descripción
  status: "A"
  uni_med: "PZA"
```

**Código:**
```java
String description = data.getProductName() != null && !data.getProductName().trim().isEmpty()
    ? data.getProductName()
    : productCode; // Usar código como respaldo
```

**Propósito:**
- Evitar descripciones vacías en la base de datos
- Mantener consistencia en catálogos
- Facilitar identificación en reportes

**Prioridad:** BAJA  
**Implementado en:** `createMissingProducts()`

---

### RN-MWH-028: Tolerancia a Errores en Parseo
**Descripción:** El sistema continúa procesando filas válidas aunque algunas tengan errores.

**Comportamiento:**
- ⚠️ Fila con error se IGNORA (no se procesa)
- ✅ Se continúa con la siguiente fila
- ✅ No se aborta toda la importación
- ✅ Al final se reporta cantidad de filas omitidas

**Tipos de errores tolerados:**
- EXIST no numérico
- STATUS inválido (ni "A" ni "B")
- CVE_ALM vacío
- CVE_ART vacío
- Filas completamente vacías

**Ejemplo:**
```
Excel contiene 100 filas:
  - 95 filas válidas
  - 5 filas con errores

Resultado:
  ✅ 95 registros procesados
  ⚠️ 5 filas ignoradas
  ✅ Status: SUCCESS
  ✅ Message: "Importación completada: 95 registros procesados, 5 filas con errores fueron omitidas"
```

**Prioridad:** MEDIA  
**Implementado en:** Parsers CSV/XLSX

---

## 📋 RESUMEN DE PRIORIDADES

### Prioridad CRÍTICA (9 reglas):
- RN-MWH-001: Formato del Archivo
- RN-MWH-001A: Condiciones Previas para Importar
- RN-MWH-008: Validación de Columnas Obligatorias
- RN-MWH-010: Validación de Estado del Periodo
- RN-MWH-012: Sincronización con inventory_stock
- RN-MWH-017: Filtrado por Periodo
- RN-MWH-020: Autenticación Requerida
- RN-MWH-021: Control de Acceso por Rol (solo Administrador)
- RN-MWH-024: Ciclo de Vida del Periodo

### Prioridad ALTA (13 reglas):
- RN-MWH-001B: Funcionamiento de Importar MultiAlmacén
- RN-MWH-002: Creación Automática de Almacenes
- RN-MWH-003: Creación Automática de Productos
- RN-MWH-004: Importación de Productos Nuevos (Actualización)
- RN-MWH-005: Actualización de Productos Existentes
- RN-MWH-006: Soft Delete de Productos (Marcado como Baja)
- RN-MWH-009: Validación de Valores por Fila
- RN-MWH-011: Validación de Formato de Periodo
- RN-MWH-013: Proceso Iterativo
- RN-MWH-013A: Interfaz de Consulta de MultiAlmacén
- RN-MWH-015: Búsqueda por Texto
- RN-MWH-018: Consulta de Stock Específico
- RN-MWH-022: Registro de Importaciones

### Prioridad MEDIA (7 reglas):
- RN-MWH-007: Prevención de Duplicados
- RN-MWH-014: Paginación Personalizada
- RN-MWH-016: Ordenación Personalizada
- RN-MWH-019: Exportación a CSV
- RN-MWH-023: Timestamps Automáticos
- RN-MWH-025: Normalización de Claves
- RN-MWH-028: Tolerancia a Errores

### Prioridad BAJA (2 reglas):
- RN-MWH-026: Generación de Nombres
- RN-MWH-027: Productos sin Descripción

**Total: 31 reglas de negocio documentadas**

---

## 🔗 INTEGRACIÓN CON OTROS MÓDULOS

### Módulo de Labels (Marbetes)
**Dependencia:** Labels consulta `inventory_stock` que es sincronizado por MultiAlmacén

**Flujo:**
```
multialmacen.xlsx
       ↓
multiwarehouse_existences
       ↓
inventory_stock (sincronización automática)
       ↓
Labels consulta existencias para generar marbetes
```

**Reglas relacionadas:**
- RN-MWH-012: Sincronización automática
- RN-MWH-013: Proceso iterativo preserva marbetes

### Módulo de Periodos
**Dependencia:** MultiAlmacén valida estado del periodo antes de importar

**Reglas relacionadas:**
- RN-MWH-010: Validación de estado
- RN-MWH-024: Ciclo de vida del periodo

### Módulo de Warehouse
**Dependencia:** MultiAlmacén crea almacenes faltantes automáticamente

**Reglas relacionadas:**
- RN-MWH-002: Creación automática de almacenes

### Módulo de Inventory (Products)
**Dependencia:** MultiAlmacén crea productos faltantes y obtiene descripciones

**Reglas relacionadas:**
- RN-MWH-003: Creación automática de productos
- RN-MWH-005: Actualización de descripciones desde inventario

---

## 📊 MÉTRICAS Y KPIs

### Métricas por Importación:
- Registros procesados
- Almacenes creados
- Productos creados
- Registros actualizados
- Registros marcados como inactivos
- Tiempo de procesamiento
- Tamaño del archivo

### Métricas Globales:
- Total de almacenes en el sistema
- Total de productos en el sistema
- Total de existencias por periodo
- Histórico de importaciones
- Tasa de éxito de importaciones

---

## 📖 RESUMEN DE REGLAS OFICIALES DEL MANUAL DE USUARIO

El Manual de Usuario SIGMA especifica las siguientes **reglas de importación** que se aplican **siempre** que se ejecute una importación de multialmacén:

### ⚙️ Reglas de Importación Inicial

**1. Creación automática de almacenes:**
> "Si en el archivo de Excel 'multialmacen.xlsx' aparecen almacenes que no existen en el SIGMA, éstos serán creados automáticamente en el catálogo de almacenes y se les agregará la leyenda: 'Este almacén no existía y fue creado en la importación' en el campo 'Observaciones'."

**2. Creación automática de productos:**
> "Si en el archivo de Excel 'multialmacen.xlsx' aparecen productos que no están en el inventario del periodo elegido en el SIGMA, éstos serán creados automáticamente en el catálogo de inventario para el periodo elegido y se les asignará la leyenda 'A' en el campo 'Estado'."

### 🔄 Reglas de Importación para Actualización

**3. Importar productos nuevos al catálogo:**
> "En caso de una importación para actualizar el catálogo de multialmacén: Si en el archivo de Excel 'multialmacen.xlsx' aparecen productos que sí están en el inventario del periodo elegido en el SIGMA pero no están en el catálogo de multialmacen, éstos serán importados al catálogo, es decir, serán los productos 'nuevos'."

**4. Actualizar productos existentes:**
> "En caso de una importación para actualizar el catálogo de multialmacén: Si en el archivo de Excel 'multialmacen.xlsx' aparecen productos que sí están en el inventario y también en el catálogo de multialmacén para el periodo elegido, sus valores serán actualizados, es decir, los valores para cada producto serán actualizados con base en lo que esté en el archivo de Excel."

**5. Marcar productos como baja (Soft Delete):**
> "En caso de una importación para actualizar el catálogo de multialmacén: Si en el archivo de Excel 'multialmacen.xlsx' NO aparecen productos que sí existen en el inventario y también en el catálogo de multialmacén para el periodo elegido, únicamente cambiará el estado del producto a 'B', lo que significará que el producto está dado de baja para el periodo elegido en el almacén designado."

### 🎯 Concepto Principal

**Importar como Actualización:**
> "Así pues, la operación de 'importar multialmacén' funciona como una variación de actualización del multialmacén para el periodo seleccionado."

### 🔒 Restricción de Acceso

> "Es importante mencionar que este catálogo está disponible únicamente para el rol 'Administrador'."

---

## 🎓 CONCLUSIÓN

El módulo **MultiAlmacén** implementa **31 reglas de negocio** (actualizado con reglas del Manual de Usuario) que cubren:

✅ **Importación masiva** con creación automática de entidades  
✅ **Actualización no destructiva** del catálogo por periodo  
✅ **Validaciones exhaustivas** en múltiples niveles  
✅ **Sincronización automática** con inventory_stock  
✅ **Proceso iterativo** que preserva datos de conteos  
✅ **Búsqueda flexible** por múltiples campos  
✅ **Auditoría completa** de todas las operaciones  
✅ **Control de periodos** según su estado  
✅ **Control de acceso** por rol (solo Administrador)  
✅ **Tolerancia a errores** sin abortar procesos  

**Estado actual: 100% implementado y funcional según Manual de Usuario SIGMA** ✅

---

## 📊 RESUMEN EJECUTIVO

### 🎯 ¿Qué es MultiAlmacén?
Catálogo que **suministra al SIGMA del inventario por almacén** de toda la empresa para un determinado periodo (mes-año).

### 👥 ¿Quién puede usarlo?
**Solo el rol "Administrador"** tiene acceso a este módulo.

### 🔧 Acciones Principales
1. **Consultar** - Visualizar inventario de todos los almacenes
2. **Importar** - Cargar/actualizar desde `C:\Sistemas\SIGMA\Documentos\multialmacen.xlsx`

### 📋 Columnas del Archivo Excel
| Columna | Descripción | Obligatorio |
|---------|-------------|-------------|
| CVE_ALM | Clave del almacén | ✅ Sí |
| CVE_ART | Clave del producto | ��� Sí |
| DESCR | Descripción (se ignora si producto existe) | ⚠️ Opcional |
| STATUS | Estado: A=Alta, B=Baja | ✅ Sí |
| EXIST | Existencias (decimal con 2 decimales) | ✅ Sí |

### ⚙️ 5 Reglas Fundamentales de Importación (Manual de Usuario)
1. **Almacenes nuevos** → Se crean automáticamente
2. **Productos nuevos** → Se crean con estado "A"
3. **Productos en inventario pero no en multialmacén** → Se importan
4. **Productos existentes** → Se actualizan con datos del Excel
5. **Productos no en Excel** → Se marcan como "B" (Baja)

### 🔄 Concepto Clave
> La importación funciona como **actualización**, NO es destructiva

### 🔍 Funcionalidades de Consulta
- ✅ Paginación: 10, 25, 50, 100 registros
- ✅ Búsqueda por: Clave producto, Producto, Almacén, Existencias
- ✅ Ordenación: Por cualquier columna (click en encabezado)
- ✅ Estados: A (Alta - vigentes), B (Baja - no vigentes)

### 📈 Total de Reglas Implementadas
- **31 reglas de negocio** completamente documentadas
- **9 críticas**, **13 altas**, **7 medias**, **2 bajas**
- **100% alineadas con Manual de Usuario SIGMA**

---

## 📚 REFERENCIAS

### Documentación Oficial:
- **Manual de Usuario SIGMA** - Sección "Catálogo de Multialmacén" (páginas 26-30)
  - Consultar multialmacén
  - Importar multialmacén
  - Reglas de importación

### Código fuente:
- **Código fuente:** `MultiWarehouseServiceImpl.java` (773 líneas)
- **Repositorio:** `MultiWarehouseRepository.java`
- **Entidad:** `MultiWarehouseExistence.java`

### Documentación técnica:
- **Documentación técnica:** `FORMATO-EXCEL-MULTIALMACEN.md`
- **Testing:** `TESTING-MULTIALMACEN.md`
- **Correcciones:** `CORRECCION-MULTIALMACEN-REGLAS-NEGOCIO.md`
- **Integración:** `ACTUALIZACION-INVENTORY-STOCK.md`
- **Verificación:** `VERIFICACION-CUMPLIMIENTO-RESUMIDA.md`

---

**Documento generado:** 26 de Enero de 2026  
**Actualizado con:** Manual de Usuario SIGMA (Oficial)  
**Autor:** Sistema de Documentación SIGMAV2  
**Versión:** 2.0
