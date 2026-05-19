# 📊 Reglas de Negocio - Reportes del Sistema de Marbetes

## 📋 Índice de Reportes

1. [Reporte de Distribución](#1-reporte-de-distribución)
2. [Reporte de Listado de Marbetes](#2-reporte-de-listado-de-marbetes)
3. [Reporte de Marbetes Pendientes](#3-reporte-de-marbetes-pendientes)
4. [Reporte de Diferencias](#4-reporte-de-diferencias)
5. [Reporte de Marbetes Cancelados](#5-reporte-de-marbetes-cancelados)
6. [Reporte Comparativo](#6-reporte-comparativo)
7. [Reporte de Almacén con Detalle](#7-reporte-de-almacén-con-detalle)
8. [Reporte de Producto con Detalle](#8-reporte-de-producto-con-detalle)
9. [Generación de Archivo TXT de Existencias](#9-generación-de-archivo-txt-de-existencias)

---

## 1. Reporte de Distribución

### 📌 Propósito
Mostrar cómo se distribuyeron los marbetes impresos entre usuarios y almacenes. Este reporte ayuda a **identificar qué usuario generó qué rango de folios** en cada almacén, útil para rastrear responsabilidades y auditar el proceso de generación.

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/distribution`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Solo marbetes impresos**
   - Se incluyen únicamente marbetes con estado `IMPRESO`
   - No se muestran marbetes generados pero no impresos
   - ⚠️ **Importante**: Un marbete solo cambia a estado `IMPRESO` después de ejecutar la operación de impresión (exportar PDF)

2. **Agrupación**
   - Los marbetes se agrupan por: `almacén + usuario que creó`
   - Cada grupo muestra:
     - Usuario que generó los marbetes (email del usuario)
     - Almacén (clave y nombre)
     - Folio mínimo del rango
     - Folio máximo del rango
     - Total de marbetes generados

3. **Ordenamiento**
   - Los resultados se ordenan por `clave del almacén` (ascendente)

4. **Seguridad**
   - Se valida acceso al almacén si se especifica `warehouseId`
   - Usuarios sin acceso reciben error 403

5. **Contexto de operación**
   - **Administrador/Auxiliar**: Pueden ver distribución de **todos los almacenes**
   - **Almacenista**: Solo ve distribución de **su almacén asignado**

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Usuario | Email del usuario que generó los marbetes |
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre descriptivo del almacén |
| Folio Inicial | Primer folio del rango |
| Folio Final | Último folio del rango |
| Total Marbetes | Cantidad de marbetes generados |

---

## 2. Reporte de Listado de Marbetes

### 📌 Propósito
Listar todos los marbetes de un periodo/almacén con su información completa.

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/list`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Incluye todos los estados**
   - Se muestran marbetes en cualquier estado: `GENERADO`, `IMPRESO`, `CANCELADO`
   - Incluye una bandera `esCancelado` para identificarlos

2. **Conteos**
   - Se obtienen ambos conteos (C1 y C2) si existen
   - Si no hay conteos, los campos aparecen como `null`

3. **Información de producto y almacén**
   - Se consulta el catálogo de productos para obtener detalles
   - Se consulta el catálogo de almacenes

4. **Ordenamiento**
   - Los resultados se ordenan por `número de marbete` (folio) ascendente

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Número Marbete | Folio del marbete |
| Clave Producto | Código del artículo |
| Descripción | Nombre del producto |
| Unidad Medida | UM del producto |
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre del almacén |
| Conteo 1 | Valor del primer conteo (C1) |
| Conteo 2 | Valor del segundo conteo (C2) |
| Estado | Estado actual del marbete |
| Es Cancelado | true/false |

---

## 3. Reporte de Marbetes Pendientes

### 📌 Propósito
Identificar marbetes que aún no tienen conteos completos (falta C1 o C2).

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/pending`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Exclusión de cancelados**
   - **NO** se incluyen marbetes con estado `CANCELADO`
   - Solo se consideran marbetes activos

2. **Criterio de "pendiente"**
   - Un marbete es pendiente si:
     - ✅ **Le falta C1** (conteo1 == null)
     - ✅ **Le falta C2** (conteo2 == null)
     - ✅ **O ambos**

3. **Casos incluidos**
   | Conteo 1 | Conteo 2 | ¿Se incluye? | Razón |
   |----------|----------|--------------|-------|
   | null | null | ✅ SÍ | Falta ambos |
   | 10 | null | ✅ SÍ | Falta C2 |
   | null | 15 | ✅ SÍ | Falta C1 |
   | 10 | 15 | ❌ NO | Completo |

4. **Ordenamiento**
   - Ordenado por `número de marbete` ascendente

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Número Marbete | Folio del marbete |
| Clave Producto | Código del artículo |
| Descripción | Nombre del producto |
| Unidad Medida | UM del producto |
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre del almacén |
| Conteo 1 | Valor si existe, null si falta |
| Conteo 2 | Valor si existe, null si falta |
| Estado | Estado actual |

### 💡 Casos de Uso
- **Supervisores**: Identificar marbetes sin contar
- **Auxiliares de conteo**: Ver qué marbetes requieren C2
- **Administradores**: Monitorear avance del inventario

### 🔍 Contexto de Operación
- Los marbetes pendientes aparecen durante el proceso de conteo
- Un marbete permanece pendiente hasta que tenga **ambos conteos** (C1 y C2)
- **Importante**: Si un marbete es cancelado, **desaparece** de este reporte inmediatamente

### ⚠️ Consideraciones Especiales
- **Marbetes sin imprimir**: Solo aparecen si ya fueron impresos (estado `IMPRESO`)
- **Cancelación durante conteo**: Si se cancela un marbete pendiente, se remueve del listado
- **Reimpresión**: Los marbetes reimpresos aparecen normalmente si les faltan conteos

---

## 4. Reporte de Diferencias

### 📌 Propósito
Mostrar marbetes donde C1 y C2 tienen valores diferentes, requiriendo resolución.

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/with-differences`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Exclusión de cancelados**
   - **NO** se incluyen marbetes `CANCELADO`

2. **Criterios de inclusión** (TODOS deben cumplirse)
   - ✅ `C1 != null` (Existe el primer conteo)
   - ✅ `C2 != null` (Existe el segundo conteo)
   - ✅ `C1 > 0` (El primer conteo es mayor a cero)
   - ✅ `C2 > 0` (El segundo conteo es mayor a cero)
   - ✅ `C1 != C2` (Los conteos son diferentes)

3. **Casos incluidos/excluidos**
   | C1 | C2 | ¿Se incluye? | Razón |
   |----|----|--------------|-------|
   | 10 | 5 | ✅ SÍ | Diferencia válida |
   | 100 | 95 | ✅ SÍ | Diferencia válida |
   | 0 | 0 | ❌ NO | Ambos en cero |
   | 0 | 10 | ❌ NO | C1 no válido |
   | 10 | 0 | ❌ NO | C2 no válido |
   | 5 | 5 | ❌ NO | Sin diferencia |
   | null | 10 | ❌ NO | Falta C1 |
   | 10 | null | ❌ NO | Falta C2 |

4. **Cálculo de diferencia**
   - Se calcula como: `|C1 - C2|` (valor absoluto)
   - Ejemplo: C1=10, C2=15 → Diferencia=5

5. **Ordenamiento**
   - Ordenado por `número de marbete` ascendente

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Número Marbete | Folio del marbete |
| Clave Producto | Código del artículo |
| Descripción | Nombre del producto |
| Unidad Medida | UM del producto |
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre del almacén |
| Conteo 1 | Primer conteo |
| Conteo 2 | Segundo conteo |
| Diferencia | Valor absoluto de C1 - C2 |
| Estado | Estado del marbete |

### 💡 Casos de Uso
- **Supervisores**: Identificar discrepancias que requieren tercer conteo
- **Auditores**: Verificar calidad del inventario
- **Administradores**: Analizar exactitud de los conteos

### 🔧 Corrección Implementada
**Fecha**: 2026-01-22  
**Problema previo**: Se incluían marbetes con C1=0 y C2=0  
**Solución**: Agregada validación `C1 > 0` y `C2 > 0`

---

## 5. Reporte de Marbetes Cancelados

### 📌 Propósito
Listar todos los marbetes que fueron cancelados durante el inventario.

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/cancelled`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Fuente de datos**
   - Se consulta la tabla `label_cancelled` (no la tabla principal `label`)
   - Solo se incluyen registros donde `reactivado = false`

2. **Incluye reactivados**
   - Si un marbete fue cancelado y luego reactivado, **NO** aparece

3. **Información de auditoría**
   - Se muestra quién canceló el marbete
   - Se muestra cuándo fue cancelado
   - Se incluye el motivo de cancelación

4. **Conteos previos**
   - Si existían conteos antes de cancelar, se muestran
   - Útil para auditoría

5. **Ordenamiento**
   - Ordenado por `número de marbete` ascendente

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Número Marbete | Folio del marbete cancelado |
| Clave Producto | Código del artículo |
| Descripción | Nombre del producto |
| Unidad Medida | UM del producto |
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre del almacén |
| Conteo 1 | C1 si existía antes de cancelar |
| Conteo 2 | C2 si existía antes de cancelar |
| Motivo Cancelación | Razón por la cual se canceló |
| Fecha Cancelación | Timestamp de la cancelación |
| Usuario que Canceló | Email del usuario |

### 💡 Casos de Uso
- **Auditoría**: Rastrear cancelaciones
- **Supervisión**: Verificar motivos de cancelación
- **Análisis**: Identificar patrones de cancelaciones

### 🔍 Proceso de Cancelación

#### ¿Cuándo cancelar un marbete?
Un marbete debe cancelarse cuando:
- ❌ El producto no se encuentra físicamente en el almacén
- ❌ El producto fue trasladado a otro almacén
- ❌ El marbete se generó por error
- ❌ El producto ya no existe en inventario

#### ¿Quién puede cancelar?
- ✅ **Todos los roles** pueden cancelar marbetes:
  - `ADMINISTRADOR`
  - `AUXILIAR`
  - `ALMACENISTA`
  - `AUXILIAR_DE_CONTEO`

#### Flujo de Cancelación
1. Usuario ingresa al módulo de **Conteo**
2. Ingresa el folio del marbete
3. Marca la casilla "Cancelado"
4. El sistema registra:
   - Folio cancelado
   - Usuario que canceló
   - Fecha/hora de cancelación
   - Motivo (opcional)
   - Conteos previos (si existían)

#### Efectos de la Cancelación
- 🔴 El marbete cambia a estado `CANCELADO`
- 🔴 Se registra en la tabla `label_cancelled`
- 🔴 Ya **NO** aparece en:
  - Reporte de pendientes
  - Reporte de diferencias
  - Reporte comparativo
  - Reporte de almacén con detalle
  - Reporte de producto con detalle
- ✅ **SÍ** aparece en:
  - Reporte de cancelados
  - Reporte de listado (con bandera `esCancelado = true`)

### ⚙️ Reactivación de Marbetes
- Si un marbete cancelado es **reactivado**, el campo `reactivado` cambia a `true`
- Los marbetes reactivados **NO** aparecen en el reporte de cancelados
- **Nota**: La funcionalidad de reactivación está implementada en el sistema

---

## 6. Reporte Comparativo

### 📌 Propósito
Comparar existencias físicas (contadas) con existencias teóricas (del sistema).

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/comparative`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Exclusión de cancelados**
   - **NO** se incluyen marbetes `CANCELADO`

2. **Agrupación**
   - Los marbetes se agrupan por: `producto + almacén`
   - Un producto puede tener múltiples marbetes que se suman

3. **Cálculo de existencias físicas**
   - Para cada marbete:
     - Se usa `C2` (segundo conteo) si existe
     - Si no existe C2, se usa `C1` (primer conteo)
     - Si no existe ninguno, se considera 0
   - Se suman todas las existencias del mismo producto/almacén

4. **Obtención de existencias teóricas**
   - Se consulta la tabla `inventory_stock`
   - Se busca por: `productId`, `warehouseId`, `periodId`
   - Se toma el campo `existQty`
   - Si no existe registro, se considera 0

5. **Cálculo de diferencia**
   - `Diferencia = Existencias Físicas - Existencias Teóricas`
   - Puede ser positiva (sobrante) o negativa (faltante)

6. **Cálculo de porcentaje**
   - `Porcentaje = (Diferencia / Existencias Teóricas) × 100`
   - Si existencias teóricas = 0, entonces porcentaje = 0
   - Se redondea a 4 decimales (HALF_UP)

7. **Ordenamiento**
   - Primero por `clave de almacén`
   - Luego por `clave de producto`

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre del almacén |
| Clave Producto | Código del artículo |
| Descripción | Nombre del producto |
| Unidad Medida | UM del producto |
| Existencias Físicas | Suma de conteos (C2 o C1) |
| Existencias Teóricas | Del sistema (inventory_stock) |
| Diferencia | Física - Teórica |
| Porcentaje | % de diferencia |

### 💡 Interpretación

**Diferencia Positiva (+)**
- Hay **más** producto físicamente que en el sistema
- Ejemplo: Teóricas=100, Físicas=110 → Diferencia=+10 (+10%)

**Diferencia Negativa (-)**
- Hay **menos** producto físicamente que en el sistema
- Ejemplo: Teóricas=100, Físicas=90 → Diferencia=-10 (-10%)

**Diferencia Cero (0)**
- Coincidencia exacta entre físico y teórico

### 💡 Casos de Uso
- **Contabilidad**: Ajustes de inventario
- **Almacén**: Identificar mermas o sobrantes
- **Auditoría**: Validar exactitud del sistema

---

## 7. Reporte de Almacén con Detalle

### 📌 Propósito
Ver el detalle completo de cada marbete de un almacén (sin agrupar).

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/warehouse-detail`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Incluye todos los estados**
   - Se muestran marbetes en cualquier estado
   - Incluye bandera `esCancelado`

2. **Sin agrupación**
   - Cada marbete es una línea individual
   - No se suman cantidades

3. **Cantidad mostrada**
   - Se usa `C2` si existe
   - Si no existe C2, se usa `C1`
   - Si no existe ninguno, cantidad = 0

4. **Ordenamiento**
   - Primero por `clave de almacén`
   - Luego por `clave de producto`
   - Finalmente por `número de marbete`

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre del almacén |
| Clave Producto | Código del artículo |
| Descripción | Nombre del producto |
| Unidad Medida | UM del producto |
| Número Marbete | Folio del marbete |
| Cantidad | C2 o C1 (lo que exista) |
| Estado | Estado del marbete |
| Es Cancelado | true/false |

### 💡 Casos de Uso
- **Supervisores**: Ver detalle completo del almacén
- **Auditoría**: Verificar cada marbete individualmente
- **Análisis**: Identificar productos con múltiples marbetes

---

## 8. Reporte de Producto con Detalle

### 📌 Propósito
Ver el detalle de cada marbete agrupado por producto, con totales.

### 🎯 Endpoint
`POST /api/sigmav2/labels/reports/product-detail`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo
- `warehouseId` (opcional): Filtrar por almacén específico

### ✅ Reglas de Negocio

1. **Exclusión de cancelados**
   - **NO** se incluyen marbetes `CANCELADO`

2. **Cálculo de totales por producto**
   - Se calcula el total de existencias por producto
   - Suma de todos los marbetes del mismo producto
   - El total se muestra en cada línea del producto

3. **Existencias por marbete**
   - Se usa `C2` si existe
   - Si no existe C2, se usa `C1`
   - Si no existe ninguno, existencias = 0

4. **Ordenamiento**
   - Primero por `clave de producto`
   - Luego por `clave de almacén`
   - Finalmente por `número de marbete`

### 📊 Información Mostrada
| Campo | Descripción |
|-------|-------------|
| Clave Producto | Código del artículo |
| Descripción | Nombre del producto |
| Unidad Medida | UM del producto |
| Clave Almacén | Código del almacén |
| Nombre Almacén | Nombre del almacén |
| Número Marbete | Folio del marbete |
| Existencias | C2 o C1 de este marbete |
| Total | Suma de todos los marbetes del producto |

### 💡 Ejemplo

**Producto: "TORNILLO 1/4"**
| Marbete | Almacén | Existencias | Total |
|---------|---------|-------------|-------|
| 100 | A01 | 10 | 35 |
| 101 | A01 | 15 | 35 |
| 102 | A02 | 10 | 35 |

El **Total (35)** es la suma de las existencias de los 3 marbetes.

### 💡 Casos de Uso
- **Compras**: Ver existencias totales por producto
- **Producción**: Planificar basado en inventario físico
- **Análisis**: Comparar distribución del producto en almacenes

---

## 9. Generación de Archivo TXT de Existencias

### 📌 Propósito
Generar un archivo de texto plano (.txt) con el inventario completo de productos y sus existencias físicas, ordenado alfabéticamente por clave de producto. Este archivo es el **resultado final** del proceso de inventario físico.

### 🎯 Endpoint
`POST /api/sigmav2/labels/generate-file`

### 📥 Filtros
- `periodId` (obligatorio): ID del periodo del cual se generará el archivo

### ✅ Reglas de Negocio

1. **Exclusión de cancelados**
   - **NO** se incluyen marbetes con estado `CANCELADO`
   - Solo se consideran marbetes activos

2. **Agrupación por producto**
   - Los marbetes se agrupan por `productId`
   - Se suman las existencias físicas de todos los marbetes del mismo producto
   - **Nota**: Un producto puede tener múltiples marbetes en diferentes almacenes

3. **Cálculo de existencias físicas**
   - Para cada marbete:
     - Se usa `C2` (segundo conteo) si existe
     - Si no existe C2, se usa `C1` (primer conteo)
     - Si no existe ninguno, se considera `0`
   - Se suman todas las existencias del mismo producto

4. **Ordenamiento**
   - Los productos se ordenan **alfabéticamente** por `clave de producto`

5. **Ubicación del archivo**
   - Directorio: `C:\Sistemas\SIGMA\Documentos\`
   - Si el directorio no existe, el sistema lo crea automáticamente

6. **Nombre del archivo**
   - Formato: `Existencias_{NombrePeriodo}.txt`
   - Ejemplo: `Existencias_Diciembre2016.txt`
   - El nombre del periodo se formatea: Primera letra mayúscula, sin espacios

7. **Actualización de archivos**
   - Si el archivo ya existe, se **sobrescribe**
   - El archivo contendrá siempre la información más reciente
   - No se crean versiones, solo un archivo por periodo

### 📊 Estructura del Archivo

El archivo TXT contiene 3 columnas separadas por tabulador:

| Columna | Descripción | Ejemplo |
|---------|-------------|---------|
| Clave_Producto | Código del artículo | `GM17CRTBS` |
| Descripción | Nombre del producto | `CARTUCHO P/ANT. GM17` |
| Existencias | Total de existencias físicas | `150.50` |

### 📝 Ejemplo de Contenido

```
CDI-206NG	ANILLO M3L/M4L NEGRO	25.00
CDI-3A8NG	ANILLO M3L/M4L NEGRO	30.00
EQUIPO	EQUIPO Y/O HERRAMIENTA	0.00
F15-1ET5	ENCENDEDOR F15 AGRANEL 5 COLORES	20550.00
GM17CRTBS	CARTUCHO P/ANT. GM17	70.00
GM18MEXCI	ANTORCHA EMP. GM18 1 CAJA DE 96 PZS	547.00
```

### ⚙️ Proceso de Generación

1. **Inicio**: Usuario selecciona el periodo
2. **Procesamiento**: Sistema ejecuta:
   - Obtiene todos los marbetes no cancelados del periodo
   - Agrupa por producto
   - Calcula existencias (C2 o C1)
   - Suma totales por producto
   - Ordena alfabéticamente
3. **Escritura**: Crea/actualiza el archivo TXT
4. **Confirmación**: Muestra mensaje de éxito

### 🔒 Seguridad

**Roles Permitidos**:
- ✅ `ADMINISTRADOR`
- ✅ `AUXILIAR`
- ✅ `ALMACENISTA`

**Notas de Seguridad**:
- El archivo se guarda en el servidor (no se descarga al navegador)
- Acceso al archivo requiere permisos del sistema operativo

### ⚠️ Consideraciones Importantes

1. **Marbetes sin conteos**
   - Si un marbete no tiene C1 ni C2, aporta `0` al total
   - Recomendación: Completar todos los conteos antes de generar el archivo

2. **Productos en múltiples almacenes**
   - Se suman las existencias de todos los almacenes
   - Ejemplo: Producto "A" con 10 unidades en Almacén 1 y 15 en Almacén 2 = **25 total**

3. **Regeneración**
   - Puede regenerarse el archivo cuantas veces sea necesario
   - Útil si se actualizan conteos después de la primera generación

4. **Formato de decimales**
   - Se respetan los decimales de las existencias
   - Ejemplo: `150.50`, `20.25`, `100.00`

### 💡 Casos de Uso

- **Contabilidad**: Actualizar sistema contable con inventario físico
- **Auditoría**: Documentar resultado final del inventario
- **Integración**: Importar existencias a otros sistemas
- **Respaldo**: Archivo histórico del inventario por periodo

### 📈 Flujo Completo del Inventario

```
1. Generación de marbetes
   ↓
2. Impresión de marbetes (PDF)
   ↓
3. Conteo físico (C1)
   ↓
4. Segundo conteo (C2)
   ↓
5. Resolución de diferencias
   ↓
6. Generación de archivo TXT ← **Resultado final**
```

### 🧪 Validaciones

El sistema valida:
- ✅ Que el periodo exista
- ✅ Que el usuario tenga permisos
- ✅ Que existan marbetes en el periodo
- ✅ Que se pueda escribir en el directorio

### 📞 Troubleshooting

**Error: "No se pudo crear el directorio"**
- Verificar permisos de escritura en `C:\Sistemas\`
- El sistema intentará crear la estructura completa

**Error: "No hay marbetes en el periodo"**
- Verificar que se hayan generado marbetes
- Verificar que no todos estén cancelados

**Archivo vacío o con pocos productos**
- Verificar que los conteos se hayan registrado
- Los marbetes sin conteos aportan `0` al total

---

## 🔒 Seguridad (Aplica a Todos los Reportes)

### Validación de Acceso
- Si se especifica `warehouseId`, se valida que el usuario tenga acceso
- Los usuarios sin acceso reciben **403 Forbidden**

### Roles Permitidos
- ✅ `ADMINISTRADOR`
- ✅ `AUXILIAR`
- ✅ `ALMACENISTA`
- ✅ `AUXILIAR_DE_CONTEO`

---

## 📈 Performance

### Optimizaciones Implementadas
1. **Caché de catálogos**: Productos y almacenes se consultan una vez
2. **Batch queries**: Se obtienen todos los conteos de una vez
3. **Streaming**: Uso de Java Streams para procesamiento eficiente

### Recomendaciones
- Para periodos con muchos marbetes (>10,000), considerar paginación
- Agregar índices en: `period_id`, `warehouse_id`, `product_id`, `estado`

---

## 🧪 Testing Recomendado

### Casos de Prueba Críticos

1. **Reporte Vacío**
   - Sin marbetes en el periodo → resultado vacío []

2. **Solo Cancelados**
   - Todos los marbetes cancelados → reportes sin datos (excepto reporte de cancelados)

3. **Sin Conteos**
   - Marbetes sin C1/C2 → aparecen en "pendientes", no en "diferencias"

4. **Diferencias en Cero**
   - C1=0, C2=0 → NO aparece en reporte de diferencias

5. **Validación de Acceso**
   - Usuario sin acceso al almacén → 403 Forbidden

---

## 📞 Contacto

Para dudas sobre reglas de negocio:
- **Documentación técnica**: `docs/AUDITORIA-APIS-VALIDACIONES.md`
- **Correcciones aplicadas**: `docs/CORRECCIONES-IMPLEMENTADAS-AUDITORIA.md`

---

**Última actualización**: 2026-01-22  
**Versión**: 1.0  
**Autor**: Equipo de Desarrollo SIGMAV2
